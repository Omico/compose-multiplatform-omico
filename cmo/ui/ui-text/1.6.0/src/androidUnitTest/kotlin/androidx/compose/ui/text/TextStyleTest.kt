/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.lerp
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastDistinctBy
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure
import kotlin.test.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextStyleTest {
    @Test
    fun `constructor with default values`() {
        val style = TextStyle()

        assertThat(style.brush).isNull()
        assertThat(style.color).isEqualTo(Color.Unspecified)
        assertThat(style.fontSize.isUnspecified).isTrue()
        assertThat(style.fontWeight).isNull()
        assertThat(style.fontStyle).isNull()
        assertThat(style.letterSpacing.isUnspecified).isTrue()
        assertThat(style.localeList).isNull()
        assertThat(style.background).isEqualTo(Color.Unspecified)
        assertThat(style.drawStyle).isNull()
        assertThat(style.textAlign).isEqualTo(TextAlign.Unspecified)
        assertThat(style.textDirection).isEqualTo(TextDirection.Unspecified)
        assertThat(style.textDecoration).isNull()
        assertThat(style.fontFamily).isNull()
        assertThat(style.platformStyle).isNull()
        assertThat(style.lineBreak).isEqualTo(LineBreak.Unspecified)
        assertThat(style.hyphens).isEqualTo(Hyphens.Unspecified)
        assertThat(style.textMotion).isNull()
    }

    @Test
    fun `constructor with customized hyphens`() {
        val style = TextStyle(hyphens = Hyphens.Auto)

        assertThat(style.hyphens).isEqualTo(Hyphens.Auto)
        assertThat(style.lineBreak).isEqualTo(LineBreak.Unspecified)
    }

    @Test
    fun `constructor with customized brush`() {
        val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Red))

        val style = TextStyle(brush = brush)

        assertThat(style.brush).isEqualTo(brush)
    }

    @Test
    fun `constructor with customized brush and alpha`() {
        val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Red))

        val style = TextStyle(brush = brush, alpha = 0.3f)

        assertThat(style.brush).isEqualTo(brush)
        assertThat(style.alpha).isEqualTo(0.3f)
    }

    @Test
    fun `constructor with gradient brush has unspecified color`() {
        val brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Red))

        val style = TextStyle(brush = brush)

        assertThat(style.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `constructor with SolidColor converts to regular color`() {
        val brush = SolidColor(Color.Red)

        val style = TextStyle(brush = brush)

        assertThat(style.color).isEqualTo(Color.Red)
    }

    @Test
    fun `constructor with customized textMotion`() {
        val style = TextStyle(textMotion = TextMotion.Animated)

        assertThat(style.textMotion).isEqualTo(TextMotion.Animated)
    }

    @Test
    fun `empty copy with existing brush should not remove brush`() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))

        val style = TextStyle(brush = brush)

        assertThat(style.copy().brush).isEqualTo(brush)
    }

    @Test
    fun `empty copy with existing color should not remove color`() {
        val style = TextStyle(color = Color.Red)

        assertThat(style.copy().color).isEqualTo(Color.Red)
    }

    @Test
    fun `empty copy with existing drawStyle should not remove drawStyle`() {
        val style = TextStyle(drawStyle = Stroke(2f))

        assertThat(style.copy().drawStyle).isEqualTo(Stroke(2f))
    }

    @Test
    fun `platformTextStyle copy with existing drawStyle should not remove drawStyle`() {
        val style = TextStyle(drawStyle = Stroke(2f))

        assertThat(
            style.copy(platformStyle = PlatformTextStyle()).drawStyle
        ).isEqualTo(Stroke(2f))
    }

    @Test
    fun `empty copy with existing hyphens should not remove hyphens`() {
        val style = TextStyle(hyphens = Hyphens.Auto)

        assertThat(style.copy().hyphens).isEqualTo(Hyphens.Auto)
    }

    @Test
    fun `copy with hyphens returns new hyphens`() {
        val style = TextStyle(hyphens = Hyphens.Auto)

        assertThat(style.copy(hyphens = Hyphens.None).hyphens).isEqualTo(Hyphens.None)
    }

    @Test
    fun `brush copy with existing color should remove color`() {
        val style = TextStyle(color = Color.Red)
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))

        with(style.copy(brush = brush)) {
            assertThat(this.color).isEqualTo(Color.Unspecified)
            assertThat(this.brush).isEqualTo(brush)
        }
    }

    @Test
    fun `color copy with existing brush should remove brush`() {
        val brush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val style = TextStyle(brush = brush)

        with(style.copy(color = Color.Red)) {
            assertThat(this.color).isEqualTo(Color.Red)
            assertThat(this.brush).isNull()
        }
    }

    @Test
    fun `copy with textMotion returns new textMotion`() {
        val style = TextStyle(textMotion = TextMotion.Animated)

        val newStyle = style.copy(textMotion = TextMotion.Static)

        assertThat(newStyle.textMotion).isEqualTo(TextMotion.Static)
    }

    @Test
    fun `copy with lineBreak returns new lineBreak`() {
        val style = TextStyle(lineBreak = LineBreak.Paragraph)

        val newStyle = style.copy(lineBreak = LineBreak.Heading)

        assertThat(newStyle.lineBreak).isEqualTo(LineBreak.Heading)
    }

    @Test
    fun `copy without lineBreak uses existing lineBreak`() {
        val style = TextStyle(lineBreak = LineBreak.Paragraph)

        val newStyle = style.copy()

        assertThat(newStyle.lineBreak).isEqualTo(style.lineBreak)
    }

    @Test
    fun `constructor with customized color`() {
        val color = Color.Red

        val style = TextStyle(color = color)

        assertThat(style.color).isEqualTo(color)
    }

    @Test
    fun `constructor with half-transparent color`() {
        val color = Color.Red.copy(alpha = 0.5f)

        val style = TextStyle(color = color)

        assertThat(style.color).isEqualTo(color)
        assertThat(style.alpha).isWithin(1f / 256f).of(0.5f)
    }

    @Test
    fun `constructor with customized fontSize`() {
        val fontSize = 18.sp

        val style = TextStyle(fontSize = fontSize)

        assertThat(style.fontSize).isEqualTo(fontSize)
    }

    @Test
    fun `constructor with customized fontWeight`() {
        val fontWeight = FontWeight.W500

        val style = TextStyle(fontWeight = fontWeight)

        assertThat(style.fontWeight).isEqualTo(fontWeight)
    }

    @Test
    fun `constructor with customized fontStyle`() {
        val fontStyle = FontStyle.Italic

        val style = TextStyle(fontStyle = fontStyle)

        assertThat(style.fontStyle).isEqualTo(fontStyle)
    }

    @Test
    fun `constructor with customized letterSpacing`() {
        val letterSpacing = 1.em

        val style = TextStyle(letterSpacing = letterSpacing)

        assertThat(style.letterSpacing).isEqualTo(letterSpacing)
    }

    @Test
    fun `constructor with customized baselineShift`() {
        val baselineShift = BaselineShift.Superscript

        val style = TextStyle(baselineShift = baselineShift)

        assertThat(style.baselineShift).isEqualTo(baselineShift)
    }

    @Test
    fun `constructor with customized locale`() {
        val localeList = LocaleList("en-US")

        val style = TextStyle(localeList = localeList)

        assertThat(style.localeList).isEqualTo(localeList)
    }

    @Test
    fun `constructor with customized background`() {
        val color = Color.Red

        val style = TextStyle(background = color)

        assertThat(style.background).isEqualTo(color)
    }

    @Test
    fun `constructor with customized textDecoration`() {
        val decoration = TextDecoration.Underline

        val style = TextStyle(textDecoration = decoration)

        assertThat(style.textDecoration).isEqualTo(decoration)
    }

    @Test
    fun `constructor with customized fontFamily`() {
        val fontFamily = FontFamily.SansSerif

        val style = TextStyle(fontFamily = fontFamily)

        assertThat(style.fontFamily).isEqualTo(fontFamily)
    }

    @Test
    fun `merge with other's hyphens is unspecified should use this hyphens`() {
        val style = TextStyle(hyphens = Hyphens.Auto)
        val otherStyle = TextStyle(hyphens = Hyphens.Unspecified)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isEqualTo(style.hyphens)
    }

    @Test
    fun `merge with other's hyphens is set should use other's hyphens`() {
        val style = TextStyle(hyphens = Hyphens.Auto)
        val otherStyle = TextStyle(hyphens = Hyphens.None)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.hyphens).isEqualTo(otherStyle.hyphens)
    }

    @Test
    fun `constructor with customized lineBreak`() {
        val style = TextStyle(lineBreak = LineBreak.Heading)

        assertThat(style.lineBreak).isEqualTo(LineBreak.Heading)
    }

    @Test
    fun `constructor with customized drawStyle`() {
        val stroke = Stroke(width = 8f)

        val style = TextStyle(drawStyle = stroke)

        assertThat(style.drawStyle).isEqualTo(stroke)
    }

    @Test
    fun `merge with empty other should return this`() {
        val style = TextStyle()

        val newStyle = style.merge()

        assertThat(newStyle).isEqualTo(style)
    }

    @Test
    fun `merge with other's color is unspecified should use this' color`() {
        val style = TextStyle(color = Color.Red)

        val newStyle = style.merge(TextStyle(color = Color.Unspecified))

        assertThat(newStyle.color).isEqualTo(style.color)
    }

    @Test
    fun `merge with other's color is set should use other's color`() {
        val style = TextStyle(color = Color.Red)
        val otherStyle = TextStyle(color = Color.Green)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.color).isEqualTo(otherStyle.color)
    }

    @Test
    fun `merge with other's fontFamily is null should use this' fontFamily`() {
        val style = TextStyle(fontFamily = FontFamily.SansSerif)

        val newStyle = style.merge(TextStyle(fontFamily = null))

        assertThat(newStyle.fontFamily).isEqualTo(style.fontFamily)
    }

    @Test
    fun `merge with other's fontFamily is set should use other's fontFamily`() {
        val style = TextStyle(fontFamily = FontFamily.SansSerif)
        val otherStyle = TextStyle(fontFamily = FontFamily.Serif)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.fontFamily).isEqualTo(otherStyle.fontFamily)
    }

    @Test
    fun `merge with other's fontSize is unspecified should use this' fontSize`() {
        val style = TextStyle(fontSize = 3.5.sp)

        val newStyle = style.merge(TextStyle(fontSize = TextUnit.Unspecified))

        assertThat(newStyle.fontSize).isEqualTo(style.fontSize)
    }

    @Test
    fun `merge with other's fontSize is set should use other's fontSize`() {
        val style = TextStyle(fontSize = 3.5.sp)
        val otherStyle = TextStyle(fontSize = 8.7.sp)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.fontSize).isEqualTo(otherStyle.fontSize)
    }

    @Test
    fun `merge with other's fontWeight is null should use this' fontWeight`() {
        val style = TextStyle(fontWeight = FontWeight.W300)

        val newStyle = style.merge(TextStyle(fontWeight = null))

        assertThat(newStyle.fontWeight).isEqualTo(style.fontWeight)
    }

    @Test
    fun `merge with other's fontWeight is set should use other's fontWeight`() {
        val style = TextStyle(fontWeight = FontWeight.W300)
        val otherStyle = TextStyle(fontWeight = FontWeight.W500)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.fontWeight).isEqualTo(otherStyle.fontWeight)
    }

    @Test
    fun `merge with other's fontStyle is null should use this' fontStyle`() {
        val style = TextStyle(fontStyle = FontStyle.Italic)

        val newStyle = style.merge(TextStyle(fontStyle = null))

        assertThat(newStyle.fontStyle).isEqualTo(style.fontStyle)
    }

    @Test
    fun `merge with other's fontStyle is set should use other's fontStyle`() {
        val style = TextStyle(fontStyle = FontStyle.Italic)
        val otherStyle = TextStyle(fontStyle = FontStyle.Normal)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.fontStyle).isEqualTo(otherStyle.fontStyle)
    }

    @Test
    fun `merge with other's fontSynthesis is null should use this' fontSynthesis`() {
        val style = TextStyle(fontSynthesis = FontSynthesis.Style)

        val newStyle = style.merge(TextStyle(fontSynthesis = null))

        assertThat(newStyle.fontSynthesis).isEqualTo(style.fontSynthesis)
    }

    @Test
    fun `merge with other's fontSynthesis is set should use other's fontSynthesis`() {
        val style = TextStyle(fontSynthesis = FontSynthesis.Style)
        val otherStyle = TextStyle(fontSynthesis = FontSynthesis.Weight)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.fontSynthesis).isEqualTo(otherStyle.fontSynthesis)
    }

    @Test
    fun `merge with other's fontFeature is null should use this' fontSynthesis`() {
        val style = TextStyle(fontFeatureSettings = "\"kern\" 0")

        val newStyle = style.merge(TextStyle(fontFeatureSettings = null))

        assertThat(newStyle.fontFeatureSettings).isEqualTo(style.fontFeatureSettings)
    }

    @Test
    fun `merge with other's fontFeature is set should use other's fontSynthesis`() {
        val style = TextStyle(fontFeatureSettings = "\"kern\" 0")
        val otherStyle = TextStyle(fontFeatureSettings = "\"kern\" 1")

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.fontFeatureSettings).isEqualTo(otherStyle.fontFeatureSettings)
    }

    @Test
    fun `merge with other's letterSpacing is unspecified should use this' letterSpacing`() {
        val style = TextStyle(letterSpacing = 1.2.em)

        val newStyle = style.merge(TextStyle(letterSpacing = TextUnit.Unspecified))

        assertThat(newStyle.letterSpacing).isEqualTo(style.letterSpacing)
    }

    @Test
    fun `merge with other's letterSpacing is set should use other's letterSpacing`() {
        val style = TextStyle(letterSpacing = 1.2.em)
        val otherStyle = TextStyle(letterSpacing = 1.5.em)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.letterSpacing).isEqualTo(otherStyle.letterSpacing)
    }

    @Test
    fun `merge with other's baselineShift is null should use this' baselineShift`() {
        val style = TextStyle(baselineShift = BaselineShift.Superscript)

        val newStyle = style.merge(TextStyle(baselineShift = null))

        assertThat(newStyle.baselineShift).isEqualTo(style.baselineShift)
    }

    @Test
    fun `merge with other's baselineShift is set should use other's baselineShift`() {
        val style = TextStyle(baselineShift = BaselineShift.Superscript)
        val otherStyle = TextStyle(baselineShift = BaselineShift.Subscript)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.baselineShift).isEqualTo(otherStyle.baselineShift)
    }

    @Test
    fun `merge with other's background is null should use this' background`() {
        val style = TextStyle(background = Color.Red)

        val newStyle = style.merge(TextStyle(background = Color.Unspecified))

        assertThat(newStyle.background).isEqualTo(style.background)
    }

    @Test
    fun `merge with other's background is set should use other's background`() {
        val style = TextStyle(background = Color.Red)
        val otherStyle = TextStyle(background = Color.Green)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.background).isEqualTo(otherStyle.background)
    }

    @Test
    fun `merge with other's textDecoration is null should use this' textDecoration`() {
        val style = TextStyle(textDecoration = TextDecoration.LineThrough)

        val newStyle = style.merge(TextStyle(textDecoration = null))

        assertThat(newStyle.textDecoration).isEqualTo(style.textDecoration)
    }

    @Test
    fun `merge with other's textDecoration is set should use other's textDecoration`() {
        val style = TextStyle(textDecoration = TextDecoration.LineThrough)
        val otherStyle = TextStyle(textDecoration = TextDecoration.Underline)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDecoration).isEqualTo(otherStyle.textDecoration)
    }

    @Test
    fun `merge with other's drawStyle is null should use this' drawStyle`() {
        val drawStyle1 = Stroke(cap = StrokeCap.Butt)
        val style = TextStyle(drawStyle = drawStyle1)

        val newTextStyle = style.merge(TextStyle(drawStyle = null))

        assertThat(newTextStyle.drawStyle).isEqualTo(drawStyle1)
    }

    @Test
    fun `merge with other's drawStyle is set should use other's drawStyle`() {
        val drawStyle1 = Stroke(cap = StrokeCap.Butt)
        val drawStyle2 = Fill
        val style = TextStyle(drawStyle = drawStyle1)
        val otherStyle = TextStyle(drawStyle = drawStyle2)

        val newTextStyle = style.merge(otherStyle)

        assertThat(newTextStyle.drawStyle).isEqualTo(otherStyle.drawStyle)
    }

    @Test
    fun `merge with other's locale is null should use this' locale`() {
        val style = TextStyle(localeList = LocaleList("en-US"))

        val newStyle = style.merge(TextStyle(localeList = null))

        assertThat(newStyle.localeList).isEqualTo(style.localeList)
    }

    @Test
    fun `merge with other's locale is set should use other's locale`() {
        val style = TextStyle(localeList = LocaleList("en-US"))
        val otherStyle = TextStyle(localeList = LocaleList("ja-JP"))

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.localeList).isEqualTo(otherStyle.localeList)
    }

    @Test
    fun `merge textAlign uses other's textAlign`() {
        val style = TextStyle(textAlign = TextAlign.Justify)
        val otherStyle = TextStyle(textAlign = TextAlign.Right)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textAlign).isEqualTo(otherStyle.textAlign)
    }

    @Test
    fun `merge textAlign other unspecified, return original`() {
        val style = TextStyle(textAlign = TextAlign.Justify)

        val newStyle = style.merge(TextStyle(textAlign = TextAlign.Unspecified))

        assertThat(newStyle.textAlign).isEqualTo(style.textAlign)
    }

    @Test
    fun `merge textAlign both unspecified returns unspecified`() {
        val style = TextStyle(textAlign = TextAlign.Unspecified)

        val newStyle = style.merge(TextStyle(textAlign = TextAlign.Unspecified))

        assertThat(newStyle.textAlign).isEqualTo(TextAlign.Unspecified)
    }

    @Test
    fun `merge textDirection uses other's textDirection`() {
        val style = TextStyle(textDirection = TextDirection.Rtl)
        val otherStyle = TextStyle(textDirection = TextDirection.Ltr)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textDirection).isEqualTo(otherStyle.textDirection)
    }

    @Test
    fun `merge textDirection other unspecified, returns original`() {
        val style = TextStyle(textDirection = TextDirection.Rtl)

        val newStyle = style.merge(TextStyle(textDirection = TextDirection.Unspecified))

        assertThat(newStyle.textDirection).isEqualTo(style.textDirection)
    }

    @Test
    fun `merge textDirection both unspecified returns unspecified`() {
        val style = TextStyle(textDirection = TextDirection.Unspecified)

        val newStyle = style.merge(TextStyle(textDirection = TextDirection.Unspecified))

        assertThat(newStyle.textDirection).isEqualTo(TextDirection.Unspecified)
    }

    @Test
    fun `merge lineHeight uses other's lineHeight`() {
        val style = TextStyle(lineHeight = 12.sp)
        val otherStyle = TextStyle(lineHeight = 20.sp)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeight).isEqualTo(otherStyle.lineHeight)
    }

    @Test
    fun `merge lineHeight other unspecified, return original`() {
        val style = TextStyle(lineHeight = 12.sp)

        val newStyle = style.merge(TextStyle(lineHeight = TextUnit.Unspecified))

        assertThat(newStyle.lineHeight).isEqualTo(style.lineHeight)
    }

    @Test
    fun `merge lineHeight both unspecified returns unspecified`() {
        val style = TextStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = style.merge(TextStyle(lineHeight = TextUnit.Unspecified))

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
    }

    @Test
    fun `merge textIndent uses other's textIndent`() {
        val style = TextStyle(textIndent = TextIndent(firstLine = 12.sp))
        val otherStyle = TextStyle(textIndent = TextIndent(firstLine = 20.sp))

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.textIndent).isEqualTo(otherStyle.textIndent)
    }

    @Test
    fun `merge textIndent other null, return original`() {
        val style = TextStyle(textIndent = TextIndent(firstLine = 12.sp))

        val newStyle = style.merge(TextStyle(textIndent = null))

        assertThat(newStyle.textIndent).isEqualTo(style.textIndent)
    }

    @Test
    fun `merge textIndent both null returns null`() {
        val style = TextStyle(textIndent = null)

        val newStyle = style.merge(TextStyle(textIndent = null))

        assertThat(newStyle.textIndent).isNull()
    }

    @Test
    fun `merge with null platformStyles null has null platformStyle`() {
        val style = TextStyle(platformStyle = null)
        val otherStyle = TextStyle(platformStyle = null)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.platformStyle).isNull()
    }

    @Test
    fun `merge with brush has other brush and no color`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = TextStyle(color = Color.Red)
        val otherStyle = TextStyle(brush = brush)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
    }

    @Test
    fun `merge with unspecified brush has original brush`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = TextStyle(brush = brush)
        val otherStyle = TextStyle()

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
    }

    @Test
    fun `merge brush with brush uses other's alpha`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = TextStyle(brush = brush, alpha = 0.3f)
        val otherStyle = TextStyle(brush = brush, alpha = 0.6f)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
        assertThat(mergedStyle.alpha).isEqualTo(0.6f)
    }

    @Test
    fun `merge brush with brush uses current alpha if other's is NaN`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))

        val style = TextStyle(brush = brush, alpha = 0.3f)
        val otherStyle = TextStyle(brush = brush)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.color).isEqualTo(Color.Unspecified)
        assertThat(mergedStyle.brush).isEqualTo(brush)
        assertThat(mergedStyle.alpha).isEqualTo(0.3f)
    }

    @Test
    fun `merge unspecified linebreak and lineBreak uses other's lineBreak`() {
        val style = TextStyle(lineBreak = LineBreak.Unspecified)
        val otherStyle = TextStyle(lineBreak = LineBreak.Heading)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(otherStyle.lineBreak)
    }

    @Test
    fun `merge linebreak and unspecified lineBreak uses original`() {
        val style = TextStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = TextStyle(lineBreak = LineBreak.Unspecified)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(style.lineBreak)
    }

    @Test
    fun `merge with both unspecified lineBreak uses unspecified`() {
        val style = TextStyle(lineBreak = LineBreak.Unspecified)
        val otherStyle = TextStyle(lineBreak = LineBreak.Unspecified)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(LineBreak.Unspecified)
    }

    @Test
    fun `merge with both non-null lineBreak uses other's lineBreak`() {
        val style = TextStyle(lineBreak = LineBreak.Paragraph)
        val otherStyle = TextStyle(lineBreak = LineBreak.Heading)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.lineBreak).isEqualTo(otherStyle.lineBreak)
    }

    @Test
    fun `merge null and non-null textMotion uses other's textMotion`() {
        val style = TextStyle(textMotion = null)
        val otherStyle = TextStyle(textMotion = TextMotion.Animated)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.textMotion).isEqualTo(otherStyle.textMotion)
    }

    @Test
    fun `merge non-null and null textMotion uses original`() {
        val style = TextStyle(textMotion = TextMotion.Animated)
        val otherStyle = TextStyle(textMotion = null)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.textMotion).isEqualTo(style.textMotion)
    }

    @Test
    fun `merge with both null textMotion uses null`() {
        val style = TextStyle(textMotion = null)
        val otherStyle = TextStyle(textMotion = null)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.textMotion).isEqualTo(null)
    }

    @Test
    fun `merge with both non-null textMotion uses other's textMotion`() {
        val style = TextStyle(textMotion = TextMotion.Animated)
        val otherStyle = TextStyle(textMotion = TextMotion.Static)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.textMotion).isEqualTo(otherStyle.textMotion)
    }

    @Test
    fun `plus operator merges other TextStyle`() {
        val style = TextStyle(
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Rtl
        ) + TextStyle(
            color = Color.Green,
            fontFamily = FontFamily.Cursive,
            textAlign = TextAlign.Justify,
            lineHeight = 12.sp
        )

        assertThat(style).isEqualTo(
            TextStyle(
                color = Color.Green, // SpanStyle attribute overridden by RHS
                fontWeight = FontWeight.Bold, // SpanStyle attribute from LHS,
                fontFamily = FontFamily.Cursive, // SpanStyle attribute from RHS
                textAlign = TextAlign.Justify, // ParagraphStyle attribute overridden by RHS
                textDirection = TextDirection.Rtl, // from LHS,
                lineHeight = 12.sp // ParagraphStyle attribute from RHS
            )
        )
    }

    @Test
    fun `plus operator merges other SpanStyle`() {
        val style = TextStyle(
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ) + SpanStyle(
            color = Color.Green,
            fontFamily = FontFamily.Cursive
        )

        assertThat(style).isEqualTo(
            TextStyle(
                color = Color.Green, // SpanStyle attribute overridden by RHS
                fontWeight = FontWeight.Bold, // SpanStyle attribute from LHS,
                fontFamily = FontFamily.Cursive, // SpanStyle attribute from RHS
                textAlign = TextAlign.Center // ParagraphStyle attribute from LHS
            )
        )
    }

    @Test
    fun `plus operator merges other ParagraphStyle`() {
        val style = TextStyle(
            color = Color.Red,
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Rtl
        ) + ParagraphStyle(
            textAlign = TextAlign.Justify,
            lineHeight = 12.sp
        )

        assertThat(style).isEqualTo(
            TextStyle(
                color = Color.Red, // SpanStyle from LHS
                textAlign = TextAlign.Justify, // ParagraphStyle attribute overridden by RHS
                textDirection = TextDirection.Rtl, // from LHS,
                lineHeight = 12.sp // ParagraphStyle attribute from RHS
            )
        )
    }

    @Test
    fun `lerp color with a and b are specified`() {
        val color1 = Color.Red
        val color2 = Color.Green
        val t = 0.3f
        val style1 = TextStyle(color = color1)
        val style2 = TextStyle(color = color2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.color).isEqualTo(lerp(start = color1, stop = color2, fraction = t))
    }

    @Test
    fun `lerp hyphens with a and b are not Null and t is smaller than half`() {
        val hyphens1 = Hyphens.Auto
        val hyphens2 = Hyphens.None
        val t = 0.3f
        val style1 = TextStyle(hyphens = hyphens1)
        val style2 = TextStyle(hyphens = hyphens2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.hyphens).isEqualTo(hyphens1)
    }

    @Test
    fun `lerp hyphens with a and b are not Null and t is larger than half`() {
        val hyphens1 = Hyphens.Auto
        val hyphens2 = Hyphens.None
        val t = 0.8f
        val style1 = TextStyle(hyphens = hyphens1)
        val style2 = TextStyle(hyphens = hyphens2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.hyphens).isEqualTo(hyphens2)
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is smaller than half`() {
        val fontFamily1 = FontFamily.SansSerif
        val fontFamily2 = FontFamily.Serif
        val t = 0.3f
        val style1 = TextStyle(fontFamily = fontFamily1)
        val style2 = TextStyle(fontFamily = fontFamily2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.fontFamily).isEqualTo(fontFamily1)
    }

    @Test
    fun `lerp fontFamily with a and b are not Null and t is larger than half`() {
        val fontFamily1 = FontFamily.SansSerif
        val fontFamily2 = FontFamily.Serif
        val t = 0.8f
        val style1 = TextStyle(fontFamily = fontFamily1)
        val style2 = TextStyle(fontFamily = fontFamily2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.fontFamily).isEqualTo(fontFamily2)
    }

    @Test
    fun `lerp fontSize with a and b are not Null`() {
        val fontSize1 = 8.sp
        val fontSize2 = 16.sp
        val t = 0.8f
        val style1 = TextStyle(fontSize = fontSize1)
        val style2 = TextStyle(fontSize = fontSize2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        // a + (b - a) * t = 8.0f + (16.0f  - 8.0f) * 0.8f = 14.4f
        assertThat(newStyle.fontSize).isEqualTo(14.4.sp)
    }

    @Test
    fun `lerp fontWeight with a and b are not Null`() {
        val fontWeight1 = FontWeight.W200
        val fontWeight2 = FontWeight.W500
        val t = 0.8f
        val style1 = TextStyle(fontWeight = fontWeight1)
        val style2 = TextStyle(fontWeight = fontWeight2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.fontWeight).isEqualTo(lerp(fontWeight1, fontWeight2, t))
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is smaller than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.3f
        val style1 = TextStyle(fontStyle = fontStyle1)
        val style2 = TextStyle(fontStyle = fontStyle2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.fontStyle).isEqualTo(fontStyle1)
    }

    @Test
    fun `lerp fontStyle with a and b are not Null and t is larger than half`() {
        val fontStyle1 = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal
        // attributes other than fontStyle are required for lerp not to throw an exception
        val t = 0.8f
        val style1 = TextStyle(fontStyle = fontStyle1)
        val style2 = TextStyle(fontStyle = fontStyle2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.fontStyle).isEqualTo(fontStyle2)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is smaller than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = TextStyle(fontSynthesis = fontSynthesis1)
        val style2 = TextStyle(fontSynthesis = fontSynthesis2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.fontSynthesis).isEqualTo(fontSynthesis1)
    }

    @Test
    fun `lerp fontSynthesis with a and b are not Null and t is larger than half`() {
        val fontSynthesis1 = FontSynthesis.Style
        val fontSynthesis2 = FontSynthesis.Weight

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = TextStyle(fontSynthesis = fontSynthesis1)
        val style2 = TextStyle(fontSynthesis = fontSynthesis2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.fontSynthesis).isEqualTo(fontSynthesis2)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is smaller than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.3f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = TextStyle(fontFeatureSettings = fontFeatureSettings1)
        val style2 = TextStyle(fontFeatureSettings = fontFeatureSettings2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings1)
    }

    @Test
    fun `lerp fontFeatureSettings with a and b are not Null and t is larger than half`() {
        val fontFeatureSettings1 = "\"kern\" 0"
        val fontFeatureSettings2 = "\"kern\" 1"

        val t = 0.8f
        // attributes other than fontSynthesis are required for lerp not to throw an exception
        val style1 = TextStyle(fontFeatureSettings = fontFeatureSettings1)
        val style2 = TextStyle(fontFeatureSettings = fontFeatureSettings2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.fontFeatureSettings).isEqualTo(fontFeatureSettings2)
    }

    @Test
    fun `lerp baselineShift with a and b are not Null`() {
        val baselineShift1 = BaselineShift(1.0f)
        val baselineShift2 = BaselineShift(2.0f)
        val t = 0.3f
        val style1 = TextStyle(baselineShift = baselineShift1)
        val style2 = TextStyle(baselineShift = baselineShift2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.baselineShift)
            .isEqualTo(lerp(baselineShift1, baselineShift2, t))
    }

    @Test
    fun `lerp textGeometricTransform with a and b are not Null`() {
        val textTransform1 =
            TextGeometricTransform(scaleX = 1.5f, skewX = 0.1f)
        val textTransform2 =
            TextGeometricTransform(scaleX = 1.0f, skewX = 0.3f)
        val t = 0.3f
        val style1 = TextStyle(textGeometricTransform = textTransform1)
        val style2 = TextStyle(textGeometricTransform = textTransform2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.textGeometricTransform)
            .isEqualTo(lerp(textTransform1, textTransform2, t))
    }

    @Test
    fun `lerp locale with a and b are not Null and t is smaller than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.3f
        val style1 = TextStyle(localeList = localeList1)
        val style2 = TextStyle(localeList = localeList2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.localeList).isEqualTo(localeList1)
    }

    @Test
    fun `lerp locale with a and b are not Null and t is larger than half`() {
        val localeList1 = LocaleList("en-US")
        val localeList2 = LocaleList("ja-JP")
        val t = 0.8f
        val style1 = TextStyle(localeList = localeList1)
        val style2 = TextStyle(localeList = localeList2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.localeList).isEqualTo(localeList2)
    }

    @Test
    fun `lerp background with a and b are Null and t is smaller than half`() {
        val style1 = TextStyle(background = Color.Unspecified)
        val style2 = TextStyle(background = Color.Unspecified)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.1f)

        assertThat(newStyle.background).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `lerp background with a is Null and b is not Null`() {
        val t = 0.1f
        val style1 = TextStyle(background = Color.Unspecified)
        val color2 = Color.Red
        val style2 = TextStyle(background = color2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.background).isEqualTo(lerp(Color.Unspecified, color2, t))
    }

    @Test
    fun `lerp background with a is Not Null and b is Null`() {
        val t = 0.1f
        val color1 = Color.Red
        val style1 = TextStyle(background = color1)
        val style2 = TextStyle(background = Color.Unspecified)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.background).isEqualTo(lerp(color1, Color.Unspecified, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is smaller than half`() {
        val color1 = Color.Red
        val color2 = Color.Green
        val t = 0.2f
        val style1 = TextStyle(background = color1)
        val style2 = TextStyle(background = color2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp background with a and b are not Null and t is larger than half`() {
        val color1 = Color.Red
        val color2 = Color.Green
        val t = 0.8f
        val style1 = TextStyle(background = color1)
        val style2 = TextStyle(background = color2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.background).isEqualTo(lerp(color1, color2, t))
    }

    @Test
    fun `lerp textDecoration with a and b are not Null and t is smaller than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.2f
        val style1 = TextStyle(textDecoration = decoration1)
        val style2 = TextStyle(textDecoration = decoration2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.textDecoration).isEqualTo(decoration1)
    }

    @Test
    fun `lerp textDecoration with a and b are not Null and t is larger than half`() {
        val decoration1 = TextDecoration.LineThrough
        val decoration2 = TextDecoration.Underline
        val t = 0.8f
        val style1 = TextStyle(textDecoration = decoration1)
        val style2 = TextStyle(textDecoration = decoration2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.textDecoration).isEqualTo(decoration2)
    }

    @Test
    fun `lerp drawStyle with a and b are not null and fraction is smaller than half`() {
        val drawStyle1 = Fill
        val drawStyle2 = Stroke(width = 8f)
        val t = 0.2f
        val style1 = TextStyle(drawStyle = drawStyle1)
        val style2 = TextStyle(drawStyle = drawStyle2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.drawStyle).isEqualTo(drawStyle1)
    }

    @Test
    fun `lerp drawStyle with a and b are not null and fraction is larger than half`() {
        val drawStyle1 = Fill
        val drawStyle2 = Stroke(width = 8f)
        val t = 0.8f
        val style1 = TextStyle(drawStyle = drawStyle1)
        val style2 = TextStyle(drawStyle = drawStyle2)

        val newStyle = lerp(start = style1, stop = style2, fraction = t)

        assertThat(newStyle.drawStyle).isEqualTo(drawStyle2)
    }

    @Test
    fun `lerp textAlign with a unspecified, b not null and t is smaller than half`() {
        val style1 = TextStyle(textAlign = TextAlign.Unspecified)
        val style2 = TextStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textAlign).isEqualTo(TextAlign.Unspecified)
    }

    @Test
    fun `lerp textAlign with a and b are not Null and t is smaller than half`() {
        val style1 = TextStyle(textAlign = TextAlign.Left)
        val style2 = TextStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textAlign).isEqualTo(style1.textAlign)
    }

    @Test
    fun `lerp textAlign with a and b are not Null and t is larger than half`() {
        val style1 = TextStyle(textAlign = TextAlign.Left)
        val style2 = TextStyle(textAlign = TextAlign.Right)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.textAlign).isEqualTo(style2.textAlign)
    }

    @Test
    fun `lerp textDirection with a unspecified, b not null and t is smaller than half`() {
        val style1 = TextStyle(textDirection = TextDirection.Unspecified)
        val style2 = TextStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textDirection).isEqualTo(TextDirection.Unspecified)
    }

    @Test
    fun `lerp textDirection with a and b are not Null and t is smaller than half`() {
        val style1 = TextStyle(textDirection = TextDirection.Ltr)
        val style2 = TextStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.textDirection).isEqualTo(style1.textDirection)
    }

    @Test
    fun `lerp textDirection with a and b are not Null and t is larger than half`() {
        val style1 = TextStyle(textDirection = TextDirection.Ltr)
        val style2 = TextStyle(textDirection = TextDirection.Rtl)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.textDirection).isEqualTo(style2.textDirection)
    }

    @Test
    fun `lerp textIndent with a null, b not null and t is smaller than half returns null`() {
        val style1 = TextStyle(textIndent = null)
        val style2 = TextStyle(textIndent = TextIndent(firstLine = 20.sp))
        val fraction = 0.4f
        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.textIndent).isEqualTo(
            lerp(TextIndent(), style2.textIndent!!, fraction)
        )
    }

    @Test
    fun `lerp textIndent with a and b are not Null`() {
        val style1 = TextStyle(textIndent = TextIndent(firstLine = 10.sp))
        val style2 = TextStyle(textIndent = TextIndent(firstLine = 20.sp))
        val fraction = 0.6f
        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.textIndent).isEqualTo(
            lerp(style1.textIndent!!, style2.textIndent!!, fraction)
        )
    }

    @Test
    fun `lerp lineHeight with a and b are specified`() {
        val style1 = TextStyle(lineHeight = 10.sp)
        val style2 = TextStyle(lineHeight = 20.sp)
        val fraction = 0.4f

        val newStyle = lerp(start = style1, stop = style2, fraction = fraction)

        assertThat(newStyle.lineHeight).isEqualTo(
            androidx.compose.ui.unit.lerp(style1.lineHeight, style2.lineHeight, fraction)
        )
    }

    @Test
    fun `lerp lineHeight with a and b are unspecified`() {
        val style1 = TextStyle(lineHeight = TextUnit.Unspecified)
        val style2 = TextStyle(lineHeight = TextUnit.Unspecified)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.lineHeight).isEqualTo(TextUnit.Unspecified)
    }

    @Test
    fun `lerp with null platformStyles has null platformStyle`() {
        val style = TextStyle(platformStyle = null)
        val otherStyle = TextStyle(platformStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.5f)

        assertThat(lerpedStyle.platformStyle).isNull()
    }

    @Test
    fun `constructor without platformStyle sets platformStyle to null`() {
        val style = TextStyle(textAlign = TextAlign.Start)

        assertThat(style.platformStyle).isNull()
    }

    @Test
    fun `copy without platformStyle uses existing platformStyle`() {
        val style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
        val newStyle = style.copy()

        assertThat(newStyle.platformStyle).isEqualTo(style.platformStyle)
    }

    @Test
    fun `lerp brush with a specified, b specified and t is smaller than half`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val style1 = TextStyle(brush = brush)
        val style2 = TextStyle(color = Color.Red)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.4f)

        assertThat(newStyle.brush).isEqualTo(brush)
        assertThat(newStyle.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `lerp brush with a specified, b specified and t is larger than half`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val style1 = TextStyle(brush = brush)
        val style2 = TextStyle(color = Color.Red)

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.brush).isEqualTo(null)
        assertThat(newStyle.color).isEqualTo(Color.Red)
    }

    @Test
    fun `lerp brush with a specified, b not specified and t is larger than half`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val style1 = TextStyle(brush = brush)
        val style2 = TextStyle()

        val newStyle = lerp(start = style1, stop = style2, fraction = 0.6f)

        assertThat(newStyle.brush).isNull()
        assertThat(newStyle.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `lerp with non-null start, unspecified end, closer to start has non-null lineBreak`() {
        val style = TextStyle(lineBreak = LineBreak.Heading)
        val otherStyle = TextStyle(lineBreak = LineBreak.Unspecified)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineBreak).isEqualTo(style.lineBreak)
    }

    @Test
    fun `lerp with non-null start, unspecified end, closer to end has unspecified lineBreak`() {
        val style = TextStyle(lineBreak = LineBreak.Heading)
        val otherStyle = TextStyle(lineBreak = LineBreak.Unspecified)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineBreak).isEqualTo(LineBreak.Unspecified)
    }

    @Test
    fun `lerp with unspecified start, non-null end, closer to start has null lineBreak`() {
        val style = TextStyle(lineBreak = LineBreak.Unspecified)
        val otherStyle = TextStyle(lineBreak = LineBreak.Heading)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineBreak).isEqualTo(LineBreak.Unspecified)
    }

    @Test
    fun `lerp with unspecified start, non-null end, closer to end has non-null lineBreak`() {
        val style = TextStyle(lineBreak = LineBreak.Unspecified)
        val otherStyle = TextStyle(lineBreak = LineBreak.Heading)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineBreak).isEqualTo(otherStyle.lineBreak)
    }

    @Test
    fun `lerp with non-null start, null end, closer to start has non-null textMotion`() {
        val style = TextStyle(textMotion = TextMotion.Animated)
        val otherStyle = TextStyle(textMotion = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.textMotion).isSameInstanceAs(style.textMotion)
    }

    @Test
    fun `lerp with non-null start, null end, closer to end has null textMotion`() {
        val style = TextStyle(textMotion = TextMotion.Animated)
        val otherStyle = TextStyle(textMotion = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.textMotion).isNull()
    }

    @Test
    fun `lerp with null start, non-null end, closer to start has null textMotion`() {
        val style = TextStyle(textMotion = null)
        val otherStyle = TextStyle(textMotion = TextMotion.Animated)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.textMotion).isNull()
    }

    @Test
    fun `lerp with null start, non-null end, closer to end has non-null textMotion`() {
        val style = TextStyle(textMotion = null)
        val otherStyle = TextStyle(textMotion = TextMotion.Animated)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.textMotion).isSameInstanceAs(otherStyle.textMotion)
    }

    @Test
    fun `toSpanStyle return attributes with correct values`() {
        val color = Color.Red
        val fontSize = 56.sp
        val fontWeight = FontWeight.Bold
        val fontStyle = FontStyle.Italic
        val fontSynthesis = FontSynthesis.All
        val fontFamily = FontFamily.Default
        val fontFeatureSettings = "font feature settings"
        val letterSpacing = 0.2.sp
        val baselineShift = BaselineShift.Subscript
        val textGeometricTransform = TextGeometricTransform(scaleX = 0.5f, skewX = 0.6f)
        val localeList = LocaleList("tr-TR")
        val background = Color.Yellow
        val decoration = TextDecoration.Underline
        val shadow = Shadow(color = Color.Green, offset = Offset(2f, 4f))

        val style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = decoration,
            shadow = shadow
        )

        assertThat(style.toSpanStyle()).isEqualTo(
            SpanStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                fontFamily = fontFamily,
                fontFeatureSettings = fontFeatureSettings,
                letterSpacing = letterSpacing,
                baselineShift = baselineShift,
                textGeometricTransform = textGeometricTransform,
                localeList = localeList,
                background = background,
                textDecoration = decoration,
                shadow = shadow
            )
        )
    }

    @Test
    fun `toSpanStyle return attributes with correct values for brush`() {
        val brush = Brush.linearGradient(listOf(Color.Blue, Color.Red))
        val fontSize = 56.sp
        val fontWeight = FontWeight.Bold
        val fontStyle = FontStyle.Italic
        val fontSynthesis = FontSynthesis.All
        val fontFamily = FontFamily.Default
        val fontFeatureSettings = "font feature settings"
        val letterSpacing = 0.2.sp
        val baselineShift = BaselineShift.Subscript
        val textGeometricTransform = TextGeometricTransform(scaleX = 0.5f, skewX = 0.6f)
        val localeList = LocaleList("tr-TR")
        val background = Color.Yellow
        val decoration = TextDecoration.Underline
        val shadow = Shadow(color = Color.Green, offset = Offset(2f, 4f))
        val drawStyle = Stroke(width = 8f)

        val style = TextStyle(
            brush = brush,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = decoration,
            shadow = shadow,
            drawStyle = drawStyle
        )

        assertThat(style.toSpanStyle()).isEqualTo(
            SpanStyle(
                brush = brush,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                fontSynthesis = fontSynthesis,
                fontFamily = fontFamily,
                fontFeatureSettings = fontFeatureSettings,
                letterSpacing = letterSpacing,
                baselineShift = baselineShift,
                textGeometricTransform = textGeometricTransform,
                localeList = localeList,
                background = background,
                textDecoration = decoration,
                shadow = shadow,
                drawStyle = drawStyle
            )
        )
    }

    @Test
    fun `toParagraphStyle return attributes with correct values`() {
        val textAlign = TextAlign.Justify
        val textDirection = TextDirection.Rtl
        val lineHeight = 100.sp
        val textIndent = TextIndent(firstLine = 20.sp, restLine = 40.sp)
        val lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
        val hyphens = Hyphens.Auto
        val lineBreak = LineBreak(
            strategy = LineBreak.Strategy.Balanced,
            strictness = LineBreak.Strictness.Strict,
            wordBreak = LineBreak.WordBreak.Phrase
        )
        val textMotion = TextMotion.Animated

        val style = TextStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            lineHeight = lineHeight,
            textIndent = textIndent,
            lineHeightStyle = lineHeightStyle,
            lineBreak = lineBreak,
            hyphens = hyphens,
            textMotion = textMotion
        )

        assertThat(style.toParagraphStyle()).isEqualTo(
            ParagraphStyle(
                textAlign = textAlign,
                textDirection = textDirection,
                lineHeight = lineHeight,
                textIndent = textIndent,
                lineHeightStyle = lineHeightStyle,
                hyphens = hyphens,
                lineBreak = lineBreak,
                textMotion = textMotion
            )
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `negative lineHeight throws IllegalStateException`() {
        TextStyle(lineHeight = (-1).sp)
    }

    @Test
    fun `lineHeightStyle lerp with null lineHeightStyles has null lineHeightStyle`() {
        val style = TextStyle(lineHeightStyle = null)
        val otherStyle = TextStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.5f)

        assertThat(lerpedStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `lineHeightStyle lerp with non-null start, null end, closer to start has non-null`() {
        val style = TextStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = TextStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineHeightStyle).isSameInstanceAs(style.lineHeightStyle)
    }

    @Test
    fun `lineHeightStyle lerp with non-null start, null end, closer to end has null`() {
        val style = TextStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = TextStyle(lineHeightStyle = null)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `lineHeightStyle lerp with null start, non-null end, closer to start has null`() {
        val style = TextStyle(lineHeightStyle = null)
        val otherStyle = TextStyle(lineHeightStyle = LineHeightStyle.Default)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.4f)

        assertThat(lerpedStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `lineHeightStyle lerp with null start, non-null end, closer to end has non-null`() {
        val style = TextStyle(lineHeightStyle = null)
        val otherStyle = TextStyle(lineHeightStyle = LineHeightStyle.Default)

        val lerpedStyle = lerp(start = style, stop = otherStyle, fraction = 0.6f)

        assertThat(lerpedStyle.lineHeightStyle).isSameInstanceAs(otherStyle.lineHeightStyle)
    }

    @Test
    fun `equals return false for different line height behavior`() {
        val style = TextStyle(lineHeightStyle = null)
        val otherStyle = TextStyle(lineHeightStyle = LineHeightStyle.Default)

        assertThat(style == otherStyle).isFalse()
    }

    @Test
    fun `equals return true for same line height behavior`() {
        val style = TextStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = TextStyle(lineHeightStyle = LineHeightStyle.Default)

        assertThat(style == otherStyle).isTrue()
    }

    @Test
    fun `hashCode is same for same line height behavior`() {
        val style = TextStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = TextStyle(lineHeightStyle = LineHeightStyle.Default)

        assertThat(style.hashCode()).isEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `hashCode is different for different line height behavior`() {
        val style = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Bottom,
                trim = LineHeightStyle.Trim.None
            )
        )
        val otherStyle = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        )

        assertThat(style.hashCode()).isNotEqualTo(otherStyle.hashCode())
    }

    @Test
    fun `copy with lineHeightStyle returns new lineHeightStyle`() {
        val style = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Bottom,
                trim = LineHeightStyle.Trim.None
            )
        )
        val newLineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
        val newStyle = style.copy(lineHeightStyle = newLineHeightStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(newLineHeightStyle)
    }

    @Test
    fun `copy without lineHeightStyle uses existing lineHeightStyle`() {
        val style = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Bottom,
                trim = LineHeightStyle.Trim.None
            )
        )
        val newStyle = style.copy()

        assertThat(newStyle.lineHeightStyle).isEqualTo(style.lineHeightStyle)
    }

    @Test
    fun `merge with null lineHeightStyle uses other's lineHeightStyle`() {
        val style = TextStyle(lineHeightStyle = null)
        val otherStyle = TextStyle(lineHeightStyle = LineHeightStyle.Default)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(otherStyle.lineHeightStyle)
    }

    @Test
    fun `merge with non-null lineHeightStyle, returns original`() {
        val style = TextStyle(lineHeightStyle = LineHeightStyle.Default)
        val otherStyle = TextStyle(lineHeightStyle = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(style.lineHeightStyle)
    }

    @Test
    fun `merge with both null lineHeightStyle returns null`() {
        val style = TextStyle(lineHeightStyle = null)
        val otherStyle = TextStyle(lineHeightStyle = null)

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isNull()
    }

    @Test
    fun `merge with both non-null lineHeightStyle returns other's lineHeightStyle`() {
        val style = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )
        val otherStyle = TextStyle(
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Bottom,
                trim = LineHeightStyle.Trim.Both
            )
        )

        val newStyle = style.merge(otherStyle)

        assertThat(newStyle.lineHeightStyle).isEqualTo(otherStyle.lineHeightStyle)
    }

    @Test
    fun `constructor without lineHeightStyle sets lineHeightStyle to null`() {
        val style = TextStyle(textAlign = TextAlign.Start)

        assertThat(style.lineHeightStyle).isNull()
    }

    @Test
    fun resolveTextDirection_unspecified() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.Unspecified
            )
        ).isEqualTo(TextDirection.Ltr)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.Unspecified
            )
        ).isEqualTo(TextDirection.Rtl)
    }

    @Test
    fun resolveTextDirection_Content() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.Content
            )
        ).isEqualTo(TextDirection.ContentOrLtr)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.Content
            )
        ).isEqualTo(TextDirection.ContentOrRtl)
    }

    @Test
    fun resolveTextDirection_ContentOrLtr() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.ContentOrLtr
            )
        ).isEqualTo(TextDirection.ContentOrLtr)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.ContentOrLtr
            )
        ).isEqualTo(TextDirection.ContentOrLtr)
    }

    @Test
    fun resolveTextDirection_ContentOrRtl() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.ContentOrRtl
            )
        ).isEqualTo(TextDirection.ContentOrRtl)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.ContentOrRtl
            )
        ).isEqualTo(TextDirection.ContentOrRtl)
    }

    @Test
    fun resolveTextDirection_Ltr() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.Ltr
            )
        ).isEqualTo(TextDirection.Ltr)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.Ltr
            )
        ).isEqualTo(TextDirection.Ltr)
    }

    @Test
    fun resolveTextDirection_Rtl() {
        assertThat(
            resolveTextDirection(
                LayoutDirection.Ltr,
                TextDirection.Rtl
            )
        ).isEqualTo(TextDirection.Rtl)

        assertThat(
            resolveTextDirection(
                LayoutDirection.Rtl,
                TextDirection.Rtl
            )
        ).isEqualTo(TextDirection.Rtl)
    }

    @Test
    fun textStyle_allParamsMerge_nonDefaultWins() {
        val subject = TextStyle.Default
        val parameters = TextStyle::class.allConstructorParams()
        val others = parameters.map { (param, constructor) ->
            val args = mapOf(param to subject.getNotEqualValueFor(param))
                .addPairwiseArgsToFix(parameters.map { it.first })
            param to constructor.callBy(args)
        }
        for ((param, other) in others) {
            val merged = subject.merge(other)
            val inverseMerged = other.merge(subject)

            // hand-rolling error messages since this is a all-params test
            assertNotEquals(subject, merged,
                "subject.merge(other) on param=`${param.name}` failed" +
                    "\n\tmerged  = $merged" +
                    "\n\tother   = $other" +
                    "\n\tsubject = $subject"
            )
            assertNotEquals(subject, inverseMerged,
                "other.merge(subject) on param=`${param.name}` failed" +
                    "\n\tmerged  = $merged" +
                    "\n\tother   = $other" +
                    "\n\tsubject = $subject"
            )
        }
    }
}

/**
 * Some params such as (brush, alpha) require a sibling parameter.
 *
 * This function is to fix the args map for them.
 */
private fun Map<KParameter, Any?>.addPairwiseArgsToFix(
    params: Collection<KParameter>
): Map<KParameter, Any?> {
    val name = keys.first().name
    return this + when (name) {
        "brush" -> {
            val alpha = params.first { it.name == "alpha" }
            mapOf(alpha to 0.7f)
        }
        "alpha" -> {
            val brush = params.first { it.name == "brush" }
            mapOf(brush to linearGradient(listOf(Color.Red)))
        }
        else -> emptyMap()
    }
}

/**
 * All parameters on any public constructor, distinct by name
 */
private fun KClass<TextStyle>.allConstructorParams() = this.constructors
    .filter { it.visibility == KVisibility.PUBLIC }
    .flatMap { it.parameters.map { param -> param to it } }
    .fastDistinctBy { it.first.name }

/**
 * Compute a distinct value for [KParameter] from the value in [kParameter]
 */
private fun TextStyle.getNotEqualValueFor(kParameter: KParameter): Any {
    val prop: KProperty1<TextStyle, *> =
        TextStyle::class.memberProperties.first { it.name == kParameter.name }
    val currentValue = prop.get(this)
    val newValue: Any = when (kParameter.type.jvmErasure) {
        Color::class -> Color.Magenta
        TextUnit::class -> 4.em
        FontWeight::class -> FontWeight(712)
        FontStyle::class -> FontStyle.Italic
        FontSynthesis::class -> FontSynthesis.Weight
        FontFamily::class -> FontFamily.Cursive
        String::class -> (currentValue as? String ?: "") + " more text"
        BaselineShift::class -> BaselineShift.Superscript
        TextGeometricTransform::class -> TextGeometricTransform(0.5f, 0.5f)
        LocaleList::class -> LocaleList("fr")
        TextDecoration::class -> TextDecoration.Underline
        Shadow::class -> Shadow(color = Color.Red)
        TextAlign::class -> TextAlign.Justify
        TextDirection::class -> TextDirection.Rtl
        TextIndent::class -> TextIndent(10.sp)
        PlatformTextStyle::class -> PlatformTextStyle(emojiSupportMatch = EmojiSupportMatch.None)
        LineHeightStyle::class -> LineHeightStyle(
            LineHeightStyle.Alignment.Center,
            LineHeightStyle.Trim.None
        )
        LineBreak::class -> LineBreak.Heading
        Hyphens::class -> Hyphens.Auto
        DrawStyle::class -> Stroke(1f)
        TextMotion::class -> TextMotion.Animated
        Brush::class -> linearGradient(listOf(Color.Blue, Color.Red))
        Float::class -> (currentValue as? Float).nextDistinct()
        Int::class -> (currentValue as? Int ?: 0) + 4
        else -> TODO("Please add an branch to this switch for ${kParameter.type}")
    }
    require(newValue != currentValue) {
        "Logic for making distinct values failed, update this function so that" +
            " $currentValue != $newValue for ${prop.name}; you may need to add logic" +
            " based on $currentValue inside the switch"
    }
    return newValue
}

/**
 * Floats can break addition on NaN and Infinity, hardcode distinct values
 */
private fun Float?.nextDistinct(): Float {
    return if (this == null) {
        1f
    } else if (this.isNaN()) {
        2f
    } else if (this.isInfinite()) {
        3f
    } else {
        this + 4.2f
    }
}
