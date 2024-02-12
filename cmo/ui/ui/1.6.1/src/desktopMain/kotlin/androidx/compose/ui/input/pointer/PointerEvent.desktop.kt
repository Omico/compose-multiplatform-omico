/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import java.awt.event.MouseEvent

internal actual typealias NativePointerButtons = Int
internal actual typealias NativePointerKeyboardModifiers = Int

/**
 * Creates [PointerButtons] with the specified state of the pressed buttons.
 */
fun PointerButtons(
    isPrimaryPressed: Boolean = false,
    isSecondaryPressed: Boolean = false,
    isTertiaryPressed: Boolean = false,
    isBackPressed: Boolean = false,
    isForwardPressed: Boolean = false
): PointerButtons {
    var res = 0
    if (isPrimaryPressed) res = res or ButtonMasks.Primary
    if (isSecondaryPressed) res = res or ButtonMasks.Secondary
    if (isTertiaryPressed) res = res or ButtonMasks.Tertiary
    if (isBackPressed) res = res or ButtonMasks.Back
    if (isForwardPressed) res = res or ButtonMasks.Forward
    return PointerButtons(res)
}

/**
 * Creates [PointerKeyboardModifiers] with the specified state of the pressed keyboard modifiers.
 */
fun PointerKeyboardModifiers(
    isCtrlPressed: Boolean = false,
    isMetaPressed: Boolean = false,
    isAltPressed: Boolean = false,
    isShiftPressed: Boolean = false,
    isAltGraphPressed: Boolean = false,
    isSymPressed: Boolean = false,
    isFunctionPressed: Boolean = false,
    isCapsLockOn: Boolean = false,
    isScrollLockOn: Boolean = false,
    isNumLockOn: Boolean = false,
): PointerKeyboardModifiers {
    var res = 0
    if (isCtrlPressed) res = res or KeyboardModifierMasks.CtrlPressed
    if (isMetaPressed) res = res or KeyboardModifierMasks.MetaPressed
    if (isAltPressed) res = res or KeyboardModifierMasks.AltPressed
    if (isShiftPressed) res = res or KeyboardModifierMasks.ShiftPressed
    if (isAltGraphPressed) res = res or KeyboardModifierMasks.AltGraphPressed
    if (isSymPressed) res = res or KeyboardModifierMasks.SymPressed
    if (isFunctionPressed) res = res or KeyboardModifierMasks.FunctionPressed
    if (isCapsLockOn) res = res or KeyboardModifierMasks.CapsLockOn
    if (isScrollLockOn) res = res or KeyboardModifierMasks.ScrollLockOn
    if (isNumLockOn) res = res or KeyboardModifierMasks.NumLockOn
    return PointerKeyboardModifiers(res)
}

internal actual fun EmptyPointerKeyboardModifiers() = PointerKeyboardModifiers()

/**
 * Describes a pointer input change event that has occurred at a particular point in time.
 */
actual data class PointerEvent internal constructor(
    /**
     * The changes.
     */
    actual val changes: List<PointerInputChange>,

    /**
     * The state of buttons (e.g. mouse or stylus buttons) during this event.
     */
    actual val buttons: PointerButtons,

    /**
     * The state of modifier keys during this event.
     */
    actual val keyboardModifiers: PointerKeyboardModifiers,

    /**
     * Original raw native event from AWT.
     *
     * Note, that its type can be different from [type], which is sent by Compose.
     * For example, Compose can send synthetic Move event on relayout,
     * but [mouseEvent] will tell that it is Up event
     */
    val mouseEvent: MouseEvent?
) {
    internal actual constructor(
        changes: List<PointerInputChange>,
        internalPointerEvent: InternalPointerEvent?
    ) : this(
        changes,
        internalPointerEvent?.buttons ?: PointerButtons(0),
        internalPointerEvent?.keyboardModifiers ?: PointerKeyboardModifiers(0),
        internalPointerEvent?.mouseEvent
    ) {
        this.type = internalPointerEvent?.type ?: PointerEventType.Unknown
    }

    /**
     * @param changes The changes.
     */
    actual constructor(changes: List<PointerInputChange>) : this(
        changes,
        buttons = PointerButtons(0),
        keyboardModifiers = PointerKeyboardModifiers(0),
        mouseEvent = null
    )

    actual var type: PointerEventType = PointerEventType.Unknown
        internal set
}

private object ButtonMasks {
    const val Primary = 1 shl 0
    const val Secondary = 1 shl 1
    const val Tertiary = 1 shl 2
    const val Back = 1 shl 3
    const val Forward = 1 shl 4
}

private object KeyboardModifierMasks {
    const val CtrlPressed = 1 shl 0
    const val MetaPressed = 1 shl 1
    const val AltPressed = 1 shl 2
    const val AltGraphPressed = 1 shl 3
    const val SymPressed = 1 shl 4
    const val ShiftPressed = 1 shl 5
    const val FunctionPressed = 1 shl 6
    const val CapsLockOn = 1 shl 7
    const val ScrollLockOn = 1 shl 8
    const val NumLockOn = 1 shl 9
}
actual val PointerButtons.isPrimaryPressed
    get() = (packedValue and ButtonMasks.Primary) != 0

actual val PointerButtons.isSecondaryPressed: Boolean
    get() = ((packedValue and ButtonMasks.Secondary) != 0)

actual val PointerButtons.isTertiaryPressed: Boolean
    get() = (packedValue and ButtonMasks.Tertiary) != 0

actual val PointerButtons.isBackPressed: Boolean
    get() = packedValue and ButtonMasks.Back != 0

actual val PointerButtons.isForwardPressed: Boolean
    get() = packedValue and ButtonMasks.Forward != 0

actual fun PointerButtons.isPressed(buttonIndex: Int): Boolean =
    when (buttonIndex) {
        0 -> isPrimaryPressed
        1 -> isSecondaryPressed
        2 -> isTertiaryPressed
        3 -> isBackPressed
        4 -> isForwardPressed
        else -> false
    }

actual val PointerButtons.areAnyPressed: Boolean
    get() = isPrimaryPressed || isSecondaryPressed || isTertiaryPressed ||
        isBackPressed || isForwardPressed

actual fun PointerButtons.indexOfFirstPressed(): Int = when {
    isPrimaryPressed -> 0
    isSecondaryPressed -> 1
    isTertiaryPressed -> 2
    isBackPressed -> 3
    isForwardPressed -> 4
    else -> -1
}

actual fun PointerButtons.indexOfLastPressed(): Int = when {
    isForwardPressed -> 4
    isBackPressed -> 3
    isTertiaryPressed -> 2
    isSecondaryPressed -> 1
    isPrimaryPressed -> 0
    else -> -1
}

actual val PointerKeyboardModifiers.isCtrlPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.CtrlPressed) != 0

actual val PointerKeyboardModifiers.isMetaPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.MetaPressed) != 0

actual val PointerKeyboardModifiers.isAltPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.AltPressed) != 0

actual val PointerKeyboardModifiers.isAltGraphPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.AltGraphPressed) != 0

actual val PointerKeyboardModifiers.isSymPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.SymPressed) != 0

actual val PointerKeyboardModifiers.isShiftPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.ShiftPressed) != 0

actual val PointerKeyboardModifiers.isFunctionPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.FunctionPressed) != 0

actual val PointerKeyboardModifiers.isCapsLockOn: Boolean
    get() = (packedValue and KeyboardModifierMasks.CapsLockOn) != 0

actual val PointerKeyboardModifiers.isScrollLockOn: Boolean
    get() = (packedValue and KeyboardModifierMasks.ScrollLockOn) != 0

actual val PointerKeyboardModifiers.isNumLockOn: Boolean
    get() = (packedValue and KeyboardModifierMasks.NumLockOn) != 0
