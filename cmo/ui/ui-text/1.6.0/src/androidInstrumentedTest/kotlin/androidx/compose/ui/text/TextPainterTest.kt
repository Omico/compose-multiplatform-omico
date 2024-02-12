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

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TextPainterTest {

    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)
    private var defaultDensity = Density(density = 1f)
    private var layoutDirection = LayoutDirection.Ltr

    private val longString = "Lorem ipsum dolor sit amet, consectetur " +
        "adipiscing elit. Curabitur augue leo, finibus vitae felis ac, pretium condimentum " +
        "augue. Nullam non libero sed lectus aliquet venenatis non at purus. Fusce id arcu " +
        "eu mauris pulvinar laoreet."

    private val longText = AnnotatedString(longString)

    @Test
    fun drawTextWithMeasurer_shouldBeEqualTo_drawTextLayoutResult() {
        val measurer = textMeasurer()
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
            constraints = Constraints(maxWidth = 400, maxHeight = 400)
        )

        val bitmap = draw {
            drawText(textLayoutResult)
        }
        val bitmap2 = draw {
            drawText(
                measurer,
                text = longText,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
                size = Size(400f, 400f)
            )
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextString_sizeUnspecified_shouldFitTheTextInside() {
        // We check whether drawing with unspecified size tries to fit the text in a given
        // canvas area.
        val measurer = textMeasurer()

        // No size constrained
        val bitmap = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longString,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f)
            )
        }

        // size constrained but larger than drawing area
        val bitmap2 = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longString,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f),
                size = Size(200f, 200f)
            )
        }

        // size constrained to drawing area
        val bitmap3 = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longString,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f),
                size = Size(100f, 100f)
            )
        }

        // when size is not constrained by Size.Unspecified, default behavior should limit size to
        // drawing area. Hence bitmap != bitmap2, bitmap == bitmap3
        assertThat(bitmap).isNotEqualToBitmap(bitmap2)
        assertThat(bitmap).isEqualToBitmap(bitmap3)
    }

    @Test
    fun drawTextString_widthUnspecified_shouldFitTheTextHorizontally() {
        val measurer = textMeasurer()

        // No width constrained
        val bitmap = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longString,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f),
                size = Size(Float.NaN, 200f)
            )
        }

        // width constrained but larger than drawing area
        val bitmap2 = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longString,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f),
                size = Size(200f, 200f)
            )
        }

        // width constrained to drawing area
        val bitmap3 = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longString,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f),
                size = Size(100f, 200f)
            )
        }

        // when width is not constrained by Float.NaN, default behavior should limit width to
        // drawing area. Hence bitmap != bitmap2, bitmap == bitmap3
        assertThat(bitmap).isNotEqualToBitmap(bitmap2)
        assertThat(bitmap).isEqualToBitmap(bitmap3)
    }

    @Test
    fun drawTextString_heightUnspecified_shouldFitTheTextVertically() {
        val measurer = textMeasurer()

        // No height constrained
        val bitmap = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longString,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f),
                size = Size(200f, Float.NaN)
            )
        }

        // height constrained but larger than drawing area
        val bitmap2 = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longString,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f),
                size = Size(200f, 200f)
            )
        }

        // height constrained to drawing area
        val bitmap3 = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longString,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f),
                size = Size(200f, 100f)
            )
        }

        // when height is not constrained by Float.NaN, default behavior should limit height to
        // drawing area. Hence bitmap != bitmap2, bitmap == bitmap3
        assertThat(bitmap).isNotEqualToBitmap(bitmap2)
        assertThat(bitmap).isEqualToBitmap(bitmap3)
    }

    @Test
    fun drawTextAnnotatedString_sizeUnspecified_shouldFitTheTextInside() {
        // We check whether drawing with unspecified size tries to fit the text in a given
        // canvas area.
        val measurer = textMeasurer()

        val bitmap = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longText,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f)
            )
        }

        val bitmap2 = draw(300f, 300f, 200f, 200f) {
            drawText(
                measurer,
                text = longText,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 12.sp),
                topLeft = Offset(100f, 100f),
                size = Size(200f, 200f)
            )
        }

        assertThat(bitmap).isNotEqualToBitmap(bitmap2)
    }

    @Test
    fun textMeasurerCache_shouldNotAffectTheResult_forColor() {
        val measurer = textMeasurer(cacheSize = 8)

        val bitmap = draw {
            drawText(
                textMeasurer = measurer,
                text = longText,
                style = TextStyle(
                    color = Color.Red,
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = 20.sp
                ),
                size = Size(400f, 400f)
            )
        }
        val bitmap2 = draw {
            drawText(
                textMeasurer = measurer,
                text = longText,
                style = TextStyle(
                    color = Color.Blue,
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = 20.sp
                ),
                size = Size(400f, 400f)
            )
        }

        assertThat(bitmap).isNotEqualToBitmap(bitmap2)
    }

    @Test
    fun textMeasurerCache_shouldNotAffectTheResult_forFontSize() {
        val measurer = textMeasurer(cacheSize = 8)

        val bitmap = draw {
            drawText(
                textMeasurer = measurer,
                text = longText,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 20.sp),
                size = Size(400f, 400f)
            )
        }
        val bitmap2 = draw {
            drawText(
                textMeasurer = measurer,
                text = longText,
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = 24.sp),
                size = Size(400f, 400f)
            )
        }

        assertThat(bitmap).isNotEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextLayout_shouldChangeColor() {
        val measurer = textMeasurer()
        val textLayoutResultRed = measurer.measure(
            text = longText,
            style = TextStyle(
                color = Color.Red,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints.fixed(400, 400)
        )

        val textLayoutResultBlue = measurer.measure(
            text = longText,
            style = TextStyle(
                color = Color.Blue,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints.fixed(400, 400)
        )

        val bitmap = draw {
            drawText(textLayoutResultRed, color = Color.Blue)
        }
        val bitmap2 = draw {
            drawText(textLayoutResultBlue)
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextLayout_shouldChangeAlphaColor() {
        val measurer = textMeasurer()
        val textLayoutResultOpaque = measurer.measure(
            text = longText,
            style = TextStyle(
                color = Color.Red,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints.fixed(400, 400)
        )

        val textLayoutResultHalfOpaque = measurer.measure(
            text = longText,
            style = TextStyle(
                color = Color.Red.copy(alpha = 0.5f),
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints.fixed(400, 400)
        )

        val bitmap = draw {
            drawText(textLayoutResultOpaque, alpha = 0.5f)
        }
        val bitmap2 = draw {
            drawText(textLayoutResultHalfOpaque)
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextLayout_shouldChangeBrush() {
        val rbBrush = Brush.radialGradient(listOf(Color.Red, Color.Blue))
        val gyBrush = Brush.radialGradient(listOf(Color.Green, Color.Yellow))
        val measurer = textMeasurer()
        val textLayoutResultRB = measurer.measure(
            text = longText,
            style = TextStyle(
                brush = rbBrush,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints.fixed(400, 400)
        )

        val textLayoutResultGY = measurer.measure(
            text = longText,
            style = TextStyle(
                brush = gyBrush,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints.fixed(400, 400)
        )

        val bitmap = draw {
            drawText(textLayoutResultRB, brush = gyBrush)
        }
        val bitmap2 = draw {
            drawText(textLayoutResultGY)
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextLayout_shouldChangeAlphaForBrush() {
        val rbBrush = Brush.radialGradient(listOf(Color.Red, Color.Blue))
        val measurer = textMeasurer()
        val textLayoutResultOpaque = measurer.measure(
            text = longText,
            style = TextStyle(
                brush = rbBrush,
                alpha = 1f,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints.fixed(400, 400)
        )

        val textLayoutResultHalfOpaque = measurer.measure(
            text = longText,
            style = TextStyle(
                brush = rbBrush,
                alpha = 0.5f,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints.fixed(400, 400)
        )

        val bitmap = draw {
            drawText(textLayoutResultOpaque, alpha = 0.5f)
        }
        val bitmap2 = draw {
            drawText(textLayoutResultHalfOpaque)
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextLayout_shouldChangeDrawStyle() {
        val fillDrawStyle = Fill
        val strokeDrawStyle = Stroke(8f, cap = StrokeCap.Round)
        val measurer = textMeasurer()
        val textLayoutResultFill = measurer.measure(
            text = longText,
            style = TextStyle(
                drawStyle = fillDrawStyle,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints(maxWidth = 400, maxHeight = 400)
        )

        val textLayoutResultStroke = measurer.measure(
            text = longText,
            style = TextStyle(
                drawStyle = strokeDrawStyle,
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints(maxWidth = 400, maxHeight = 400)
        )

        val bitmap = draw {
            drawText(textLayoutResultFill, drawStyle = strokeDrawStyle)
        }
        val bitmap2 = draw {
            drawText(textLayoutResultStroke)
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun textMeasurerDraw_isConstrainedTo_canvasSizeByDefault() {
        val measurer = textMeasurer()
        // coerceIn the width, height is ignored
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            constraints = Constraints.fixed(200, 4000)
        )

        val bitmap = draw(200f, 4000f) {
            drawText(textLayoutResult)
        }
        val bitmap2 = draw(200f, 4000f) {
            drawText(measurer, longText, style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ))
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun textMeasurerDraw_usesCanvasDensity_ByDefault() {
        val measurer = textMeasurer()
        // coerceIn the width, height is ignored
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ),
            density = Density(4f),
            constraints = Constraints.fixed(1000, 1000)
        )

        val bitmap = draw {
            drawText(textLayoutResult)
        }

        defaultDensity = Density(4f)
        val bitmap2 = draw {
            drawText(measurer, longText, style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 20.sp
            ))
        }

        assertThat(bitmap).isEqualToBitmap(bitmap2)
    }

    @Test
    fun drawTextClipsTheContent_ifOverflowIsClip() {
        val measurer = textMeasurer()
        // coerceIn the width, height is ignored
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 14.sp
            ),
            softWrap = false,
            overflow = TextOverflow.Clip,
            constraints = Constraints.fixed(200, 200)
        )

        val bitmap = draw(400f, 200f) {
            drawText(textLayoutResult)
        }
        val croppedBitmap = Bitmap.createBitmap(bitmap, 200, 0, 200, 200)

        // cropped part should be empty
        assertThat(croppedBitmap).isEqualToBitmap(Bitmap.createBitmap(
            200,
            200,
            Bitmap.Config.ARGB_8888))
    }

    @Test
    fun drawTextClipsTheContent_ifOverflowIsEllipsis_ifLessThanOneLineFits() {
        val measurer = textMeasurer()
        with(defaultDensity) {
            val fontSize = 20.sp
            val height = fontSize.toPx().ceilToInt() / 2
            val textLayoutResult = measurer.measure(
                text = longText,
                style = TextStyle(
                    fontFamily = fontFamilyMeasureFont,
                    fontSize = fontSize
                ),
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints.fixed(200, height)
            )

            val bitmap = draw(200f, 200f) {
                drawText(textLayoutResult)
            }
            val croppedBitmap = Bitmap.createBitmap(bitmap, 0, height, 200, 200 - height)

            // cropped part should be empty
            assertThat(croppedBitmap).isEqualToBitmap(
                Bitmap.createBitmap(
                    200,
                    200 - height,
                    Bitmap.Config.ARGB_8888
                )
            )
        }
    }

    @Test
    fun drawTextDoesNotClipTheContent_ifOverflowIsVisible() {
        val measurer = textMeasurer()
        // coerceIn the width, height is ignored
        val textLayoutResult = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 14.sp
            ),
            softWrap = false,
            overflow = TextOverflow.Clip,
            constraints = Constraints.fixed(400, 200)
        )

        val textLayoutResultNoClip = measurer.measure(
            text = longText,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = 14.sp
            ),
            softWrap = false,
            overflow = TextOverflow.Visible,
            constraints = Constraints.fixed(200, 200)
        )

        val bitmap = draw(400f, 200f) {
            drawText(textLayoutResult)
        }

        val bitmapNoClip = draw(400f, 200f) {
            drawText(textLayoutResultNoClip)
        }

        // cropped part should be empty
        assertThat(bitmap).isEqualToBitmap(bitmapNoClip)
    }

    private fun textMeasurer(
        fontFamilyResolver: FontFamily.Resolver = this.fontFamilyResolver,
        density: Density = this.defaultDensity,
        layoutDirection: LayoutDirection = this.layoutDirection,
        cacheSize: Int = 0
    ): TextMeasurer = TextMeasurer(
        fontFamilyResolver,
        density,
        layoutDirection,
        cacheSize
    )

    fun draw(
        bitmapWidth: Float = 1000f,
        bitmapHeight: Float = 1000f,
        canvasWidth: Float = bitmapWidth,
        canvasHeight: Float = bitmapHeight,
        block: DrawScope.() -> Unit
    ): Bitmap {
        val size = Size(bitmapWidth, bitmapHeight)
        val bitmap = Bitmap.createBitmap(
            size.width.toIntPx(),
            size.height.toIntPx(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap.asImageBitmap())
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            defaultDensity,
            layoutDirection,
            canvas,
            Size(canvasWidth, canvasHeight),
            block
        )
        return bitmap
    }
}
