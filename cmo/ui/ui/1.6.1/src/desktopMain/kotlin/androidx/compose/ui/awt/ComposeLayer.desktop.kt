/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.SessionMutex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.AccessibilityControllerImpl
import androidx.compose.ui.platform.DesktopPlatform
import androidx.compose.ui.platform.DesktopTextInputSession
import androidx.compose.ui.platform.PlatformComponent
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.density
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseWheelEvent
import java.awt.im.InputMethodRequests
import java.util.concurrent.atomic.AtomicReference
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoView

internal class ComposeLayer {
    private var isDisposed = false

    private val _component = ComponentImpl()
    val component: SkiaLayer get() = _component

    private val scene = ComposeScene(
        Dispatchers.Swing,
        _component,
        Density(1f),
        _component::needRedraw
    )

    private val density get() = _component.density.density

    /**
     * Keyboard modifiers state might be changed when window is not focused, so window doesn't
     * receive any key events.
     * This flag is set when window focus changes. Then we can rely on it when handling the
     * first movementEvent to get the actual keyboard modifiers state from it.
     * After window gains focus, the first motionEvent.metaState (after focus gained) is used
     * to update windowInfo.keyboardModifiers.
     *
     * TODO: needs to be set `true` when focus changes:
     * (Window focus change is implemented in JB fork, but not upstreamed yet).
     */
    private var keyboardModifiersRequireUpdate = false

    fun makeAccessible(component: Component) = object : Accessible {
        override fun getAccessibleContext(): AccessibleContext? {
            // TODO move System.getenv out of this method
            if (System.getenv("COMPOSE_DISABLE_ACCESSIBILITY") != null) return null
            val controller =
                scene.mainOwner?.accessibilityController as? AccessibilityControllerImpl
            val accessible = controller?.rootAccessible
            accessible?.getAccessibleContext()?.accessibleParent = component.parent as Accessible
            return accessible?.getAccessibleContext()
        }
    }

    @OptIn(InternalComposeUiApi::class)
    private inner class ComponentImpl :
        SkiaLayer(externalAccessibleFactory = ::makeAccessible), Accessible, PlatformComponent {
        var currentInputMethodRequests: InputMethodRequests? = null

        override fun addNotify() {
            super.addNotify()
            resetDensity()
            initContent()
            updateSceneSize()
        }

        override fun paint(g: Graphics) {
            resetDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = currentInputMethodRequests
        override var componentCursor: Cursor
            get() = super.getCursor()
            set(value) {
                super.setCursor(value)
            }

        override fun enableInput(inputMethodRequests: InputMethodRequests) {
            activeTextInputSessionJob.getAndSet(null)?.cancel()
            currentInputMethodRequests = inputMethodRequests
            enableInputMethods(true)
            val focusGainedEvent = FocusEvent(this, FocusEvent.FOCUS_GAINED)
            inputContext.dispatchEvent(focusGainedEvent)
        }

        override fun disableInput(inputMethodRequests: InputMethodRequests?) {
            activeTextInputSessionJob.getAndSet(null)?.cancel()
            if (inputMethodRequests == null || inputMethodRequests === currentInputMethodRequests) {
                currentInputMethodRequests = null
            }
        }

        private val activeTextInputSessionJob = AtomicReference<Job?>(null)

        private val textInputSessionMutex = SessionMutex<DesktopTextInputSession>()

        override suspend fun textInputSession(
            session: suspend PlatformTextInputSessionScope.() -> Nothing
        ): Nothing = textInputSessionMutex.withSessionCancellingPrevious(
            sessionInitializer = {
                DesktopTextInputSession(
                    coroutineScope = it,
                    inputComponent = this,
                    component = _component
                )
            },
            session = session
        )

        override fun doLayout() {
            super.doLayout()
            updateSceneSize()
        }

        private fun updateSceneSize() {
            this@ComposeLayer.scene.constraints = Constraints(
                maxWidth = (width * density.density).toInt().coerceAtLeast(0),
                maxHeight = (height * density.density).toInt().coerceAtLeast(0)
            )
        }

        override fun getPreferredSize(): Dimension {
            return if (isPreferredSizeSet) super.getPreferredSize() else Dimension(
                (this@ComposeLayer.scene.contentSize.width / density.density).toInt(),
                (this@ComposeLayer.scene.contentSize.height / density.density).toInt()
            )
        }

        override val locationOnScreen: Point
            @Suppress("ACCIDENTAL_OVERRIDE") // KT-47743
            get() = super.getLocationOnScreen()

        override var density: Density = Density(1f)

        private fun resetDensity() {
            density = (this as SkiaLayer).density
            if (this@ComposeLayer.scene.density != density) {
                this@ComposeLayer.scene.density = density
                updateSceneSize()
            }
        }
    }

    init {
        _component.skikoView = object : SkikoView {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                try {
                    scene.render(canvas, nanoTime)
                } catch (e: Throwable) {
                    if (System.getProperty("compose.desktop.render.ignore.errors") == null) {
                        throw e
                    }
                }
            }
        }

        _component.addInputMethodListener(object : InputMethodListener {
            override fun caretPositionChanged(event: InputMethodEvent?) {
                if (isDisposed) return
                if (event != null) {
                    scene.onInputMethodEvent(event)
                }
            }

            override fun inputMethodTextChanged(event: InputMethodEvent) {
                if (isDisposed) return
                scene.onInputMethodEvent(event)
            }
        })

        _component.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = Unit
            override fun mousePressed(event: MouseEvent) = onMouseEvent(event)
            override fun mouseReleased(event: MouseEvent) = onMouseEvent(event)
            override fun mouseEntered(event: MouseEvent) = onMouseEvent(event)
            override fun mouseExited(event: MouseEvent) = onMouseEvent(event)
        })
        _component.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) = onMouseEvent(event)
            override fun mouseMoved(event: MouseEvent) = onMouseEvent(event)
        })
        _component.addMouseWheelListener { event ->
            onMouseWheelEvent(event)
        }
        _component.focusTraversalKeysEnabled = false
        _component.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) = onKeyEvent(event)
            override fun keyReleased(event: KeyEvent) = onKeyEvent(event)
            override fun keyTyped(event: KeyEvent) = onKeyEvent(event)
        })
    }

    private fun onMouseEvent(event: MouseEvent) {
        // AWT can send events after the window is disposed
        if (isDisposed) return
        if (keyboardModifiersRequireUpdate) {
            keyboardModifiersRequireUpdate = false
            scene.setCurrentKeyboardModifiers(event.keyboardModifiers)
        }
        scene.onMouseEvent(density, event)
    }

    private fun onMouseWheelEvent(event: MouseWheelEvent) {
        if (isDisposed) return
        scene.onMouseWheelEvent(density, event)
    }

    private fun onKeyEvent(event: KeyEvent) {
        if (isDisposed) return
        scene.setCurrentKeyboardModifiers(event.toPointerKeyboardModifiers())
        if (scene.sendKeyEvent(ComposeKeyEvent(event))) {
            event.consume()
        }
    }

    private fun KeyEvent.toPointerKeyboardModifiers() = PointerKeyboardModifiers(
        isCtrlPressed = this.isControlDown,
        isMetaPressed = this.isMetaDown,
        isAltPressed = this.isAltDown,
        isShiftPressed = this.isShiftDown,
        isAltGraphPressed = this.isAltGraphDown,
        isSymPressed = false,
        isFunctionPressed = false,
        isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
        isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
        isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK)
    )

    fun dispose() {
        check(!isDisposed)
        scene.close()
        _component.dispose()
        _initContent = null
        isDisposed = true
    }

    fun setContent(
        onPreviewKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        onKeyEvent: (ComposeKeyEvent) -> Boolean = { false },
        content: @Composable () -> Unit
    ) {
        // If we call it before attaching, everything probably will be fine,
        // but the first composition will be useless, as we set density=1
        // (we don't know the real density if we have unattached component)
        _initContent = {
            scene.setContent(
                onPreviewKeyEvent = onPreviewKeyEvent,
                onKeyEvent = onKeyEvent,
                content = content
            )
        }
        initContent()
    }

    private var _initContent: (() -> Unit)? = null

    private fun initContent() {
        if (_component.isDisplayable) {
            _initContent?.invoke()
            _initContent = null
        }
    }
}

@Suppress("ControlFlowWithEmptyBody")
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeScene.onMouseEvent(
    density: Float,
    event: MouseEvent
) {
    val eventType = when (event.id) {
        MouseEvent.MOUSE_PRESSED -> PointerEventType.Press
        MouseEvent.MOUSE_RELEASED -> PointerEventType.Release
        MouseEvent.MOUSE_DRAGGED -> PointerEventType.Move
        MouseEvent.MOUSE_MOVED -> PointerEventType.Move
        MouseEvent.MOUSE_ENTERED -> PointerEventType.Enter
        MouseEvent.MOUSE_EXITED -> PointerEventType.Exit
        else -> PointerEventType.Unknown
    }
    sendPointerEvent(
        eventType = eventType,
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event
    )
}

@Suppress("ControlFlowWithEmptyBody")
@OptIn(ExperimentalComposeUiApi::class)
private fun ComposeScene.onMouseWheelEvent(
    density: Float,
    event: MouseWheelEvent
) {
    sendPointerEvent(
        eventType = PointerEventType.Scroll,
        position = Offset(event.x.toFloat(), event.y.toFloat()) * density,
        scrollDelta = if (event.isShiftDown) {
            Offset(event.preciseWheelRotation.toFloat(), 0f)
        } else {
            Offset(0f, event.preciseWheelRotation.toFloat())
        },
        timeMillis = event.`when`,
        type = PointerType.Mouse,
        buttons = event.buttons,
        keyboardModifiers = event.keyboardModifiers,
        nativeEvent = event
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private val MouseEvent.buttons
    get() = PointerButtons(
        isPrimaryPressed = (modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0 && !isMacOsCtrlClick,
        isSecondaryPressed = (modifiersEx and MouseEvent.BUTTON3_DOWN_MASK) != 0 ||
            isMacOsCtrlClick,
        isTertiaryPressed = (modifiersEx and MouseEvent.BUTTON2_DOWN_MASK) != 0,
        isBackPressed = (modifiersEx and MouseEvent.getMaskForButton(4)) != 0,
        isForwardPressed = (modifiersEx and MouseEvent.getMaskForButton(5)) != 0,
    )

@OptIn(ExperimentalComposeUiApi::class)
private val MouseEvent.keyboardModifiers
    get() = PointerKeyboardModifiers(
        isCtrlPressed = (modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0,
        isMetaPressed = (modifiersEx and InputEvent.META_DOWN_MASK) != 0,
        isAltPressed = (modifiersEx and InputEvent.ALT_DOWN_MASK) != 0,
        isShiftPressed = (modifiersEx and InputEvent.SHIFT_DOWN_MASK) != 0,
        isAltGraphPressed = (modifiersEx and InputEvent.ALT_GRAPH_DOWN_MASK) != 0,
        isSymPressed = false,
        isFunctionPressed = false,
        isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
        isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
        isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK),
    )

private fun getLockingKeyStateSafe(
    mask: Int
): Boolean = try {
    Toolkit.getDefaultToolkit().getLockingKeyState(mask)
} catch (_: Exception) {
    false
}

private val MouseEvent.isMacOsCtrlClick
    get() = (
        DesktopPlatform.Current == DesktopPlatform.MacOS &&
            ((modifiersEx and InputEvent.BUTTON1_DOWN_MASK) != 0) &&
            ((modifiersEx and InputEvent.CTRL_DOWN_MASK) != 0)
        )
