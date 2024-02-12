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

package androidx.compose.ui.platform

import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwtCursor
import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

internal actual fun sendKeyEvent(
    platformInputService: PlatformInput,
    focusOwner: FocusOwner,
    keyEvent: KeyEvent
): Boolean {
    when {
        keyEvent.nativeKeyEvent.id == java.awt.event.KeyEvent.KEY_TYPED ->
            platformInputService.charKeyPressed = true
        keyEvent.type == KeyEventType.KeyUp ->
            platformInputService.charKeyPressed = false
    }
    return focusOwner.dispatchKeyEvent(keyEvent)
}

private val defaultCursor = Cursor(Cursor.DEFAULT_CURSOR)

internal actual fun setPointerIcon(
    containerCursor: PlatformComponentWithCursor?,
    icon: PointerIcon?
) {
    when (icon) {
        is AwtCursor -> containerCursor?.componentCursor = icon.cursor
        else -> if (containerCursor?.componentCursor != defaultCursor) {
            containerCursor?.componentCursor = defaultCursor
        }
    }
}
