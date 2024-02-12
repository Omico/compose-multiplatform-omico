/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

import android.text.TextUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.toCharArray

@OptIn(ExperimentalFoundationApi::class)
internal actual fun CharSequence.toCharArray(
    destination: CharArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int
) {
    if (this is TextFieldCharSequence) {
        toCharArray(destination, destinationOffset, startIndex, endIndex)
    } else {
        TextUtils.getChars(this, startIndex, endIndex, destination, destinationOffset)
    }
}
