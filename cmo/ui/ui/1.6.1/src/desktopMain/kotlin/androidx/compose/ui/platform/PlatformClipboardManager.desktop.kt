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

package androidx.compose.ui.platform

import androidx.compose.ui.text.AnnotatedString
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException

internal actual class PlatformClipboardManager : ClipboardManager {
    internal val systemClipboard = try {
        Toolkit.getDefaultToolkit().getSystemClipboard()
    } catch (e: java.awt.HeadlessException) { null }

    actual override fun getText(): AnnotatedString? {
        return systemClipboard?.let {
            try {
                AnnotatedString(it.getData(DataFlavor.stringFlavor) as String)
            } catch (_: UnsupportedFlavorException) {
                null
            }
        }
    }

    actual override fun setText(annotatedString: AnnotatedString) {
        systemClipboard?.setContents(StringSelection(annotatedString.text), null)
    }
}
