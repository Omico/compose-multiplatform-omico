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

internal actual data class PointerInputEvent(
    val eventType: PointerEventType,
    actual val uptime: Long,
    actual val pointers: List<PointerInputEventData>,
    val buttons: PointerButtons = PointerButtons(0),
    val keyboardModifiers: PointerKeyboardModifiers = PointerKeyboardModifiers(0),
    val mouseEvent: MouseEvent? = null
)
