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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SkiaLayerTest {
    @get:Rule
    val rule = createComposeRule()

    private val layer = TestSkiaLayer()
    private val cos45 = cos(PI / 4)

    @Test
    fun initial() {
        val matrix = layer.matrix

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun move() {
        layer.move(IntOffset(10, 20))
        val matrix = layer.matrix

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun resize() {
        layer.resize(IntSize(100, 10))
        val matrix = layer.matrix

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `resize and move`() {
        layer.resize(IntSize(100, 10))
        layer.move(IntOffset(10, 20))
        val matrix = layer.matrix

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 10f,
            translationY = 20f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(10, 20), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(110, 30), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 10f,
            translationY = 20f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(10, 20), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(110, 30), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `scale, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(200, 40), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `scale, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(-100, -30), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationX, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationX = 45f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        val y = (10 * cos45).roundToInt()
        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, y), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationX, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationX = 45f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        val matrix = layer.matrix

        val y = 10 * (1 - cos45.toFloat())
        assertEquals(Offset(0f, y), matrix.map(Offset(0f, 0f)))
        assertEquals(Offset(100f, 10f), matrix.map(Offset(100f, 10f)))
    }

    @Test
    fun `rotationY, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationY = 45f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        val x = (100 * cos45).roundToInt()
        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(x, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationY, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationY = 45f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        val matrix = layer.matrix

        val x = (100 * (1 - cos45)).roundToInt()
        assertEquals(IntOffset(x, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationZ, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(-10, 100), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `rotationZ, bottom-right origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            rotationZ = 90f,
            transformOrigin = TransformOrigin(1f, 1f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(110, -90), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100, 10), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, scale, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            scaleX = 2f,
            scaleY = 4f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(0 + 60, 0 + 7), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100 * 2 + 60, 10 * 4 + 7), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, rotationZ, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(0 + 60, 0 + 7), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(-10 + 60, 100 + 7), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, rotationX, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationX = 45f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        val y = (10 * cos45).roundToInt()
        val translationY = (7 * cos45).roundToInt()
        assertEquals(IntOffset(0 + 60, 0 + translationY), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(100 + 60, y + translationY), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, rotationY, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            rotationY = 45f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        val x = (100 * cos45).roundToInt()
        val translationX = (60 * cos45).roundToInt()
        assertEquals(IntOffset(0 + translationX, 0 + 7), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(x + translationX, 10 + 7), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `scale, rotationZ, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            scaleX = 2f,
            scaleY = 4f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(0, 0), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(-10 * 4, 100 * 2), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `translation, scale, rotationZ, left-top origin`() {
        layer.resize(IntSize(100, 10))
        layer.updateProperties(
            translationX = 60f,
            translationY = 7f,
            scaleX = 2f,
            scaleY = 4f,
            rotationZ = 90f,
            transformOrigin = TransformOrigin(0f, 0f)
        )
        val matrix = layer.matrix

        assertEquals(IntOffset(0 + 60, 0 + 7), matrix.map(Offset(0f, 0f)).round())
        assertEquals(IntOffset(-10 * 4 + 60, 100 * 2 + 7), matrix.map(Offset(100f, 10f)).round())
    }

    @Test
    fun `is in layer`() {
        layer.resize(IntSize(0, 0))
        layer.updateProperties(
            clip = false
        )

        assertTrue(layer.isInLayer(Offset(-1f, -1f)))
        assertTrue(layer.isInLayer(Offset(0f, 0f)))
        assertTrue(layer.isInLayer(Offset(1f, 1f)))

        layer.resize(IntSize(0, 0))
        layer.updateProperties(
            clip = true
        )

        assertFalse(layer.isInLayer(Offset(-1f, -1f)))
        assertFalse(layer.isInLayer(Offset(0f, 0f)))
        assertFalse(layer.isInLayer(Offset(1f, 1f)))

        layer.resize(IntSize(0, 0))
        layer.updateProperties(
            clip = true,
            shape = CircleShape
        )

        assertFalse(layer.isInLayer(Offset(-1f, -1f)))
        assertFalse(layer.isInLayer(Offset(0f, 0f)))
        assertFalse(layer.isInLayer(Offset(1f, 1f)))

        layer.resize(IntSize(1, 2))
        layer.updateProperties(
            clip = true
        )

        assertFalse(layer.isInLayer(Offset(-1f, -1f)))
        assertTrue(layer.isInLayer(Offset(0f, 0f)))
        assertTrue(layer.isInLayer(Offset(0f, 1f)))
        assertFalse(layer.isInLayer(Offset(0f, 2f)))
        assertFalse(layer.isInLayer(Offset(1f, 0f)))

        layer.resize(IntSize(100, 200))
        layer.updateProperties(
            clip = true,
            shape = CircleShape
        )

        assertFalse(layer.isInLayer(Offset(5f, 5f)))
        assertFalse(layer.isInLayer(Offset(95f, 195f)))
        assertTrue(layer.isInLayer(Offset(50f, 100f)))
    }

    private fun TestSkiaLayer() = SkiaLayer(
        Density(1f, 1f),
        invalidateParentLayer = {},
        drawBlock = {}
    )

    private fun SkiaLayer.updateProperties(
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        alpha: Float = 1f,
        translationX: Float = 0f,
        translationY: Float = 0f,
        shadowElevation: Float = 0f,
        ambientShadowColor: Color = DefaultShadowColor,
        spotShadowColor: Color = DefaultShadowColor,
        rotationX: Float = 0f,
        rotationY: Float = 0f,
        rotationZ: Float = 0f,
        cameraDistance: Float = 0f,
        transformOrigin: TransformOrigin = TransformOrigin.Center,
        shape: Shape = RectangleShape,
        clip: Boolean = false,
        renderEffect: RenderEffect? = null,
        compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
    ) {
        val scope = ReusableGraphicsLayerScope()
        scope.cameraDistance = cameraDistance
        scope.scaleX = scaleX
        scope.scaleY = scaleY
        scope.alpha = alpha
        scope.translationX = translationX
        scope.translationY = translationY
        scope.shadowElevation = shadowElevation
        scope.ambientShadowColor = ambientShadowColor
        scope.spotShadowColor = spotShadowColor
        scope.rotationX = rotationX
        scope.rotationY = rotationY
        scope.rotationZ = rotationZ
        scope.cameraDistance = cameraDistance
        scope.transformOrigin = transformOrigin
        scope.shape = shape
        scope.clip = clip
        scope.renderEffect = renderEffect
        scope.compositingStrategy = compositingStrategy
        updateLayerProperties(scope, LayoutDirection.Ltr, Density(1f))
    }
}
