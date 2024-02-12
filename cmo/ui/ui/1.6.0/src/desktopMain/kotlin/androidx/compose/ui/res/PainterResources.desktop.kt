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

package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import org.xml.sax.InputSource

/**
 * Load a [Painter] from an resource stored in resources for the application and decode
 * it based on the file extension.
 *
 * Supported formats:
 * - SVG
 * - XML vector drawable
 *   (see https://developer.android.com/guide/topics/graphics/vector-drawable-resources)
 * - raster formats (BMP, GIF, HEIF, ICO, JPEG, PNG, WBMP, WebP)
 *
 * To load an image from other places (file storage, database, network), use these
 * functions inside [LaunchedEffect] or [remember]:
 * [loadImageBitmap]
 * [loadSvgPainter]
 * [loadXmlImageVector]
 *
 * @param resourcePath  path to the file in the resources folder
 * @return [Painter] used for drawing the loaded resource
 */
@Composable
fun painterResource(
    resourcePath: String
): Painter = when (resourcePath.substringAfterLast(".")) {
    "svg" -> rememberSvgResource(resourcePath)
    "xml" -> rememberVectorXmlResource(resourcePath)
    else -> rememberBitmapResource(resourcePath)
}

@Composable
private fun rememberSvgResource(resourcePath: String): Painter {
    val density = LocalDensity.current
    return remember(resourcePath, density) {
        useResource(resourcePath) {
            loadSvgPainter(it, density)
        }
    }
}

@Composable
private fun rememberVectorXmlResource(resourcePath: String): Painter {
    val density = LocalDensity.current
    val image = remember(resourcePath, density) {
        useResource(resourcePath) {
            loadXmlImageVector(InputSource(it), density)
        }
    }
    return rememberVectorPainter(image)
}

@Composable
private fun rememberBitmapResource(resourcePath: String): Painter {
    val image = remember(resourcePath) {
        useResource(resourcePath, ::loadImageBitmap)
    }
    return BitmapPainter(image)
}
