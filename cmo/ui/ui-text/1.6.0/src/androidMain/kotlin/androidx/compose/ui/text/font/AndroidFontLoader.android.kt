/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.font

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Async
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Blocking
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.OptionalLocal
import androidx.core.content.res.ResourcesCompat
import kotlin.coroutines.resume
import kotlin.runCatching
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Android implementation for [Font.ResourceLoader]. It is designed to load only [ResourceFont].
 */
internal class AndroidFontLoader(
    context: Context
) : PlatformFontLoader {
    private val context = context.applicationContext

    @OptIn(ExperimentalTextApi::class)
    override fun loadBlocking(font: Font): Typeface? {
        return when (font) {
            is AndroidFont -> font.typefaceLoader.loadBlocking(context, font)
            is ResourceFont -> when (font.loadingStrategy) {
                Blocking -> font.load(context)
                OptionalLocal -> runCatching { font.load(context) }.getOrNull()
                Async -> throw UnsupportedOperationException("Unsupported Async font load path")
                else -> throw IllegalArgumentException(
                    "Unknown loading type ${font.loadingStrategy}"
                )
            }.setFontVariationSettings(font.variationSettings, context)
            else -> null
        }
    }

    @OptIn(ExperimentalTextApi::class)
    override suspend fun awaitLoad(font: Font): Typeface? {
        return when (font) {
            is AndroidFont -> font.typefaceLoader.awaitLoad(context, font)
            is ResourceFont -> font.loadAsync(context)
                .setFontVariationSettings(font.variationSettings, context)
            else -> throw IllegalArgumentException("Unknown font type: $font")
        }
    }

    override val cacheKey: Any? = null
}

private fun ResourceFont.load(context: Context): Typeface =
    ResourcesCompat.getFont(context, resId)!!

// TODO(seanmcq): Move to core-ktx to dedup
private suspend fun ResourceFont.loadAsync(context: Context): Typeface {
    return suspendCancellableCoroutine { continuation ->
        ResourcesCompat.getFont(context, resId, object : ResourcesCompat.FontCallback() {
            override fun onFontRetrieved(typeface: Typeface) {
                continuation.resume(typeface)
            }

            override fun onFontRetrievalFailed(reason: Int) {
                continuation.cancel(
                    IllegalStateException("Unable to load font ${this@loadAsync} (reason=$reason)")
                )
            }
        }, null)
    }
}
