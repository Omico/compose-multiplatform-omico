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

@file:Suppress("DEPRECATION") // https://github.com/JetBrains/compose-jb/issues/1514

package androidx.compose.ui.window.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.sendKeyEvent
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.sendMouseWheelEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.density
import androidx.compose.ui.window.launchApplication
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class)
class WindowInputEventTest {
    @Test
    fun `catch key handlers`() = runApplicationTest {
        var window: ComposeWindow? = null
        val onKeyEventKeys = mutableSetOf<Key>()
        val onPreviewKeyEventKeys = mutableSetOf<Key>()

        fun clear() {
            onKeyEventKeys.clear()
            onPreviewKeyEventKeys.clear()
        }

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                onPreviewKeyEvent = {
                    onPreviewKeyEventKeys.add(it.key)
                    it.key == Key.Q
                },
                onKeyEvent = {
                    onKeyEventKeys.add(it.key)
                    it.key == Key.W
                }
            ) {
                window = this.window
            }
        }

        awaitIdle()

        window?.sendKeyEvent(KeyEvent.VK_Q)
        awaitIdle()
        assertThat(onPreviewKeyEventKeys).isEqualTo(setOf(Key.Q))
        assertThat(onKeyEventKeys).isEqualTo(emptySet<Key>())

        clear()
        window?.sendKeyEvent(KeyEvent.VK_W)
        awaitIdle()
        assertThat(onPreviewKeyEventKeys).isEqualTo(setOf(Key.W))
        assertThat(onKeyEventKeys).isEqualTo(setOf(Key.W))

        clear()
        window?.sendKeyEvent(KeyEvent.VK_E)
        awaitIdle()
        assertThat(onPreviewKeyEventKeys).isEqualTo(setOf(Key.E))
        assertThat(onKeyEventKeys).isEqualTo(setOf(Key.E))

        exitApplication()
    }

    @Test
    fun `catch key handlers with focused node`() = runApplicationTest {
        var window: ComposeWindow? = null
        val onWindowKeyEventKeys = mutableSetOf<Key>()
        val onWindowPreviewKeyEventKeys = mutableSetOf<Key>()
        val onNodeKeyEventKeys = mutableSetOf<Key>()
        val onNodePreviewKeyEventKeys = mutableSetOf<Key>()

        fun clear() {
            onWindowKeyEventKeys.clear()
            onWindowPreviewKeyEventKeys.clear()
            onNodeKeyEventKeys.clear()
            onNodePreviewKeyEventKeys.clear()
        }

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                onPreviewKeyEvent = {
                    onWindowPreviewKeyEventKeys.add(it.key)
                    it.key == Key.Q
                },
                onKeyEvent = {
                    onWindowKeyEventKeys.add(it.key)
                    it.key == Key.W
                },
            ) {
                window = this.window

                val focusRequester = remember(::FocusRequester)
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Box(
                    Modifier
                        .focusRequester(focusRequester)
                        .focusTarget()
                        .onPreviewKeyEvent {
                            onNodePreviewKeyEventKeys.add(it.key)
                            it.key == Key.E
                        }
                        .onKeyEvent {
                            onNodeKeyEventKeys.add(it.key)
                            it.key == Key.R
                        }
                )
            }
        }

        awaitIdle()

        window?.sendKeyEvent(KeyEvent.VK_Q)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.Q))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(emptySet<Key>())
        assertThat(onNodeKeyEventKeys).isEqualTo(emptySet<Key>())
        assertThat(onWindowKeyEventKeys).isEqualTo(emptySet<Key>())

        clear()
        window?.sendKeyEvent(KeyEvent.VK_W)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.W))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(setOf(Key.W))
        assertThat(onNodeKeyEventKeys).isEqualTo(setOf(Key.W))
        assertThat(onWindowKeyEventKeys).isEqualTo(setOf(Key.W))

        clear()
        window?.sendKeyEvent(KeyEvent.VK_E)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.E))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(setOf(Key.E))
        assertThat(onNodeKeyEventKeys).isEqualTo(emptySet<Key>())
        assertThat(onWindowKeyEventKeys).isEqualTo(emptySet<Key>())

        clear()
        window?.sendKeyEvent(KeyEvent.VK_R)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.R))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(setOf(Key.R))
        assertThat(onNodeKeyEventKeys).isEqualTo(setOf(Key.R))
        assertThat(onWindowKeyEventKeys).isEqualTo(emptySet<Key>())

        clear()
        window?.sendKeyEvent(KeyEvent.VK_T)
        awaitIdle()
        assertThat(onWindowPreviewKeyEventKeys).isEqualTo(setOf(Key.T))
        assertThat(onNodePreviewKeyEventKeys).isEqualTo(setOf(Key.T))
        assertThat(onNodeKeyEventKeys).isEqualTo(setOf(Key.T))
        assertThat(onWindowKeyEventKeys).isEqualTo(setOf(Key.T))

        exitApplication()
    }

    @Test
    fun `catch mouse press + move + release`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val events = mutableListOf<PointerEvent>()

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    Modifier.fillMaxSize().pointerInput(events) {
                        while (true) {
                            awaitPointerEventScope {
                                events += awaitPointerEvent()
                            }
                        }
                    }
                )
            }
        }
        val density by lazy { window.density.density }

        awaitIdle()
        assertThat(events.size).isEqualTo(0)

        window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, x = 100, y = 50)
        awaitIdle()
        assertThat(events.size).isEqualTo(1)
        assertThat(events.last().pressed).isEqualTo(true)
        assertThat(events.last().position).isEqualTo(Offset(100 * density, 50 * density))

        window.sendMouseEvent(MouseEvent.MOUSE_DRAGGED, x = 90, y = 40)
        awaitIdle()
        assertThat(events.size).isEqualTo(2)
        assertThat(events.last().pressed).isEqualTo(true)
        assertThat(events.last().position).isEqualTo(Offset(90 * density, 40 * density))

        window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, x = 80, y = 30)
        awaitIdle()
        assertThat(events.size).isEqualTo(3)
        assertThat(events.last().pressed).isEqualTo(false)
        assertThat(events.last().position).isEqualTo(Offset(80 * density, 30 * density))

        exitApplication()
    }

    @Test
    fun `catch mouse move`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val onMoves = mutableListOf<Offset>()
        var onEnters = 0
        var onExits = 0

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    Modifier
                        .fillMaxSize()
                        .onPointerEvent(PointerEventType.Move) {
                            onMoves.add(it.changes.first().position)
                        }
                        .onPointerEvent(PointerEventType.Enter) { onEnters++ }
                        .onPointerEvent(PointerEventType.Exit) { onExits++ }
                )
            }
        }
        val density by lazy { window.density.density }

        awaitIdle()
        assertThat(onMoves.size).isEqualTo(0)
        assertThat(onEnters).isEqualTo(0)
        assertThat(onExits).isEqualTo(0)

        window.sendMouseEvent(MouseEvent.MOUSE_ENTERED, x = 100, y = 50)
        awaitIdle()
        assertThat(onMoves.size).isEqualTo(0)
        assertThat(onEnters).isEqualTo(1)
        assertThat(onExits).isEqualTo(0)

        window.sendMouseEvent(MouseEvent.MOUSE_MOVED, x = 90, y = 50)
        awaitIdle()
        assertThat(onMoves.size).isEqualTo(1)
        assertThat(onMoves.last()).isEqualTo(Offset(90 * density, 50 * density))
        assertThat(onEnters).isEqualTo(1)
        assertThat(onExits).isEqualTo(0)

        window.sendMouseEvent(MouseEvent.MOUSE_PRESSED, x = 90, y = 50)
        window.sendMouseEvent(MouseEvent.MOUSE_DRAGGED, x = 80, y = 50)
        window.sendMouseEvent(MouseEvent.MOUSE_RELEASED, x = 80, y = 50)
        awaitIdle()
        assertThat(onMoves.size).isEqualTo(2)
        assertThat(onMoves.last()).isEqualTo(Offset(80 * density, 50 * density))
        assertThat(onEnters).isEqualTo(1)
        assertThat(onExits).isEqualTo(0)

        // TODO(https://github.com/JetBrains/compose-jb/issues/1176) fix catching exit event
//        window.sendMouseEvent(MouseEvent.MOUSE_EXITED, x = 900, y = 500)
//        awaitIdle()
//        assertThat(onMoves.size).isEqualTo(2)
//        assertThat(onEnters).isEqualTo(1)
//        assertThat(onExits).isEqualTo(1)

        exitApplication()
    }

    @Test
    fun `catch mouse scroll`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val deltas = mutableListOf<Offset>()

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    Modifier
                        .fillMaxSize()
                        .onPointerEvent(PointerEventType.Scroll) {
                            deltas.add(it.changes.first().scrollDelta)
                        }
                )
            }
        }

        awaitIdle()
        assertThat(deltas.size).isEqualTo(0)

        window.sendMouseWheelEvent(
            MouseEvent.MOUSE_WHEEL,
            x = 100,
            y = 50,
            scrollType = MouseWheelEvent.WHEEL_UNIT_SCROLL,
            wheelRotation = 1
        )
        awaitIdle()
        assertThat(deltas.size).isEqualTo(1)
        assertThat(deltas.last()).isEqualTo(Offset(0f, 1f))

        window.sendMouseWheelEvent(
            MouseEvent.MOUSE_WHEEL,
            x = 100,
            y = 50,
            scrollType = MouseWheelEvent.WHEEL_UNIT_SCROLL,
            wheelRotation = -1
        )
        awaitIdle()
        assertThat(deltas.size).isEqualTo(2)
        assertThat(deltas.last()).isEqualTo(Offset(0f, -1f))

        exitApplication()
    }

    @Test
    fun `catch multiple scroll events in one frame`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val deltas = mutableListOf<Offset>()

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    Modifier
                        .fillMaxSize()
                        .onPointerEvent(PointerEventType.Scroll) {
                            deltas.add(it.changes.first().scrollDelta)
                        }
                )
            }
        }

        awaitIdle()
        assertThat(deltas.size).isEqualTo(0)

        val eventCount = 500

        repeat(eventCount) {
            window.sendMouseWheelEvent(
                MouseEvent.MOUSE_WHEEL,
                x = 100,
                y = 50,
                scrollType = MouseWheelEvent.WHEEL_UNIT_SCROLL,
                wheelRotation = 1
            )
        }
        awaitIdle()
        assertThat(deltas.size).isEqualTo(eventCount)
        assertThat(deltas.all { it == Offset(0f, 1f) }).isTrue()

        exitApplication()
    }

    @Test
    fun `catch only the first scroll event in one frame`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val deltas = mutableListOf<Offset>()

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    Modifier
                        .fillMaxSize()
                        .onFirstPointerEvent(PointerEventType.Scroll) {
                            deltas.add(it.changes.first().scrollDelta)
                        }
                )
            }
        }

        awaitIdle()
        assertThat(deltas.size).isEqualTo(0)

        val eventCount = 500

        repeat(eventCount) {
            window.sendMouseWheelEvent(
                MouseEvent.MOUSE_WHEEL,
                x = 100,
                y = 50,
                scrollType = MouseWheelEvent.WHEEL_UNIT_SCROLL,
                wheelRotation = 1
            )
        }
        awaitIdle()
        assertThat(deltas.size).isEqualTo(1)
        assertThat(deltas.first()).isEqualTo(Offset(0f, 1f))

        exitApplication()
    }

    @Test(timeout = 5000)
    fun `receive buttons and modifiers`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val receivedButtons = mutableListOf<PointerButtons>()
        val receivedKeyboardModifiers = mutableListOf<PointerKeyboardModifiers>()

        launchApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    Modifier
                        .fillMaxSize()
                        .onPointerEvent(PointerEventType.Press) {
                            receivedButtons.add(it.buttons)
                            receivedKeyboardModifiers.add(it.keyboardModifiers)
                        }
                        .onPointerEvent(PointerEventType.Scroll) {
                            receivedButtons.add(it.buttons)
                            receivedKeyboardModifiers.add(it.keyboardModifiers)
                        }
                )
            }
        }

        awaitIdle()

        window.sendMouseEvent(
            MouseEvent.MOUSE_PRESSED,
            x = 100,
            y = 50,
            modifiers = MouseEvent.SHIFT_DOWN_MASK or MouseEvent.CTRL_DOWN_MASK or
                MouseEvent.BUTTON1_DOWN_MASK or MouseEvent.BUTTON3_DOWN_MASK
        )

        awaitIdle()
        assertThat(receivedButtons.size).isEqualTo(1)
        assertThat(receivedButtons.last()).isEqualTo(
            PointerButtons(
                isPrimaryPressed = true,
                isSecondaryPressed = true,
            )
        )
        assertThat(receivedKeyboardModifiers.size).isEqualTo(1)
        assertThat(receivedKeyboardModifiers.last()).isEqualTo(
            PointerKeyboardModifiers(
                isCtrlPressed = true,
                isShiftPressed = true,
                isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
                isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
                isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK),
            )
        )

        window.sendMouseWheelEvent(
            MouseEvent.MOUSE_WHEEL,
            x = 100,
            y = 50,
            scrollType = MouseWheelEvent.WHEEL_UNIT_SCROLL,
            wheelRotation = 1,
            modifiers = MouseEvent.SHIFT_DOWN_MASK or MouseEvent.CTRL_DOWN_MASK or
                MouseEvent.BUTTON1_DOWN_MASK or MouseEvent.BUTTON3_DOWN_MASK
        )

        awaitIdle()
        assertThat(receivedButtons.size).isEqualTo(2)
        assertThat(receivedButtons.last()).isEqualTo(
            PointerButtons(
                isPrimaryPressed = true,
                isSecondaryPressed = true,
            )
        )
        assertThat(receivedKeyboardModifiers.size).isEqualTo(2)
        assertThat(receivedKeyboardModifiers.last()).isEqualTo(
            PointerKeyboardModifiers(
                isCtrlPressed = true,
                isShiftPressed = true,
                isCapsLockOn = getLockingKeyStateSafe(KeyEvent.VK_CAPS_LOCK),
                isScrollLockOn = getLockingKeyStateSafe(KeyEvent.VK_SCROLL_LOCK),
                isNumLockOn = getLockingKeyStateSafe(KeyEvent.VK_NUM_LOCK),
            )
        )

        exitApplication()
    }

    private fun getLockingKeyStateSafe(
        mask: Int
    ): Boolean = try {
        Toolkit.getDefaultToolkit().getLockingKeyState(mask)
    } catch (_: Exception) {
        false
    }

    private fun Modifier.onPointerEvent(
        eventType: PointerEventType,
        onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
    ) = pointerInput(eventType, onEvent) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == eventType) {
                    onEvent(event)
                }
            }
        }
    }

    /**
     * Handle only the first received event and drop all the others that are received
     * in a single frame
     */
    private fun Modifier.onFirstPointerEvent(
        eventType: PointerEventType,
        onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
    ) = pointerInput(eventType, onEvent) {
        while (true) {
            awaitPointerEventScope {
                val event = awaitEvent(eventType)
                onEvent(event)
            }
        }
    }

    private suspend fun AwaitPointerEventScope.awaitEvent(
        eventType: PointerEventType
    ): PointerEvent {
        var event: PointerEvent
        do {
            event = awaitPointerEvent()
        } while (
            event.type != eventType
        )
        return event
    }

    private val PointerEvent.pressed get() = changes.first().pressed
    private val PointerEvent.position get() = changes.first().position
}
