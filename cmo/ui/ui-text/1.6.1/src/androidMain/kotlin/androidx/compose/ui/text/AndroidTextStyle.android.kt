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

package androidx.compose.ui.text

internal const val DefaultIncludeFontPadding = false

/**
 * Provides Android specific [TextStyle] configuration options for styling and compatibility.
 */
actual class PlatformTextStyle {
    /**
     * Android specific text span styling and compatibility configuration.
     */
    actual val spanStyle: PlatformSpanStyle?

    /**
     * Android specific paragraph styling and compatibility configuration.
     */
    actual val paragraphStyle: PlatformParagraphStyle?

    /**
     * Convenience constructor for when you already have a [spanStyle] and [paragraphStyle].
     *
     * @param spanStyle platform specific span styling
     * @param paragraphStyle platform specific paragraph styling
     */
    constructor(
        spanStyle: PlatformSpanStyle?,
        paragraphStyle: PlatformParagraphStyle?
    ) {
        this.spanStyle = spanStyle
        this.paragraphStyle = paragraphStyle
    }

    /**
     * Enables turning on and off for Android [includeFontPadding](https://developer.android.com/reference/android/text/StaticLayout.Builder#setIncludePad(boolean)).
     *
     * includeFontPadding was added to Android in order to prevent clipping issues on tall scripts.
     * However that issue has been fixed since Android 28. Jetpack Compose backports the fix for
     * Android versions prior to Android 28. Therefore the original reason why includeFontPadding
     * was needed in invalid on Compose.
     *
     * This configuration was added for migration of the apps in case some code or design was
     * relying includeFontPadding=true behavior.
     *
     * @param includeFontPadding Set whether to include extra space beyond font ascent and descent.
     */
    constructor(
        includeFontPadding: Boolean = DefaultIncludeFontPadding
    ) : this(
        paragraphStyle = PlatformParagraphStyle(includeFontPadding = includeFontPadding),
        spanStyle = null
    )

    /**
     * [EmojiSupportMatch] allows you to control emoji support replacement behavior.
     *
     * You can disable emoji support matches by passing [EmojiSupportMatch.None]
     *
     * @param emojiSupportMatch configuration for emoji support match and replacement
     */
    constructor(
        emojiSupportMatch: EmojiSupportMatch
    ) : this(
        paragraphStyle = PlatformParagraphStyle(emojiSupportMatch),
        spanStyle = null
    )

    override fun hashCode(): Int {
        var result = spanStyle?.hashCode() ?: 0
        result = 31 * result + (paragraphStyle?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformTextStyle) return false
        if (paragraphStyle != other.paragraphStyle) return false
        if (spanStyle != other.spanStyle) return false
        return true
    }

    override fun toString(): String {
        return "PlatformTextStyle(" +
            "spanStyle=$spanStyle, " +
            "paragraphSyle=$paragraphStyle" +
        ")"
    }
}

internal actual fun createPlatformTextStyle(
    spanStyle: PlatformSpanStyle?,
    paragraphStyle: PlatformParagraphStyle?
): PlatformTextStyle {
    return PlatformTextStyle(spanStyle, paragraphStyle)
}

/**
 * Provides Android specific [ParagraphStyle] configuration options for styling and compatibility.
 */
actual class PlatformParagraphStyle {
    actual companion object {
        actual val Default: PlatformParagraphStyle =
            PlatformParagraphStyle()
    }

    /**
     * Include extra space beyond font ascent and descent.
     *
     * Enables turning on and off for Android [includeFontPadding](https://developer.android.com/reference/android/text/StaticLayout.Builder#setIncludePad(boolean)).
     *
     * includeFontPadding was added to Android in order to prevent clipping issues on tall scripts.
     * However that issue has been fixed since Android 28. Jetpack Compose backports the fix for
     * Android versions prior to Android 28. Therefore the original reason why includeFontPadding
     * was needed in invalid on Compose.
     *
     * This configuration was added for migration of the apps in case some code or design  was
     * relying includeFontPadding=true behavior.
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    val includeFontPadding: Boolean

    /**
     * When to replace emoji with support emoji using androidx.emoji2.
     *
     * This is only available on Android.
     */
    val emojiSupportMatch: EmojiSupportMatch

    /**
     * Represents platform specific text flags
     *
     * @param includeFontPadding Set whether to include extra space beyond font ascent and descent.
     */
    constructor(includeFontPadding: Boolean = DefaultIncludeFontPadding) {
        this.includeFontPadding = includeFontPadding
        this.emojiSupportMatch = EmojiSupportMatch.Default
    }

    /**
     * Represents platform specific text flags
     *
     * @param emojiSupportMatch control emoji support matches on Android
     * @param includeFontPadding Set whether to include extra space beyond font ascent and descent.
     */
    constructor(
        emojiSupportMatch: EmojiSupportMatch = EmojiSupportMatch.Default,
        includeFontPadding: Boolean = DefaultIncludeFontPadding
    ) {
        this.includeFontPadding = includeFontPadding
        this.emojiSupportMatch = emojiSupportMatch
    }

    /**
     * Represents platform specific text flags.
     *
     * @param emojiSupportMatch control emoji support matches on Android
     */
    constructor(emojiSupportMatch: EmojiSupportMatch = EmojiSupportMatch.Default) {
        this.includeFontPadding = DefaultIncludeFontPadding
        this.emojiSupportMatch = emojiSupportMatch
    }

    /**
     * Default platform paragraph style
     */
    constructor() : this(
        includeFontPadding = DefaultIncludeFontPadding,
        emojiSupportMatch = EmojiSupportMatch.Default
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformParagraphStyle) return false
        if (includeFontPadding != other.includeFontPadding) return false
        if (emojiSupportMatch != other.emojiSupportMatch) return false
        return true
    }

    override fun hashCode(): Int {
        var result = includeFontPadding.hashCode()
        result = 31 * result + emojiSupportMatch.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlatformParagraphStyle(" +
            "includeFontPadding=$includeFontPadding, " +
            "emojiSupportMatch=$emojiSupportMatch" +
        ")"
    }

    actual fun merge(other: PlatformParagraphStyle?): PlatformParagraphStyle {
        if (other == null) return this
        // merge strategy is simple overwrite for current params, update if a optional param happens
        return other
    }
}

/**
 * Provides Android specific [SpanStyle] configuration options for styling and compatibility.
 */
actual class PlatformSpanStyle {
    actual companion object {
        actual val Default: PlatformSpanStyle = PlatformSpanStyle()
    }

    actual fun merge(other: PlatformSpanStyle?): PlatformSpanStyle {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformSpanStyle) return false
        return true
    }

    @Suppress("RedundantOverride")
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "PlatformSpanStyle()"
    }
}

/**
 * Interpolate between two PlatformParagraphStyle's.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
actual fun lerp(
    start: PlatformParagraphStyle,
    stop: PlatformParagraphStyle,
    fraction: Float
): PlatformParagraphStyle {
    if (start.includeFontPadding == stop.includeFontPadding) return start

    return PlatformParagraphStyle(
        emojiSupportMatch = lerpDiscrete(
            start.emojiSupportMatch,
            stop.emojiSupportMatch,
            fraction
        ),
        includeFontPadding = lerpDiscrete(
            start.includeFontPadding,
            stop.includeFontPadding,
            fraction
        )
    )
}

/**
 * Interpolate between two PlatformSpanStyle's.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
actual fun lerp(
    start: PlatformSpanStyle,
    stop: PlatformSpanStyle,
    fraction: Float
): PlatformSpanStyle {
    return start
}
