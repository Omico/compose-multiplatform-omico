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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GraphicsLayerScopeTest {

    @Test
    fun initialValuesAreCorrect() {
        GraphicsLayerScope().assertCorrectDefaultValuesAreCorrect()
    }

    @Test
    fun resetValuesAreCorrect() {
        val scope = GraphicsLayerScope() as ReusableGraphicsLayerScope
        scope.scaleX = 2f
        scope.scaleY = 2f
        scope.alpha = 0.5f
        scope.translationX = 5f
        scope.translationY = 5f
        scope.shadowElevation = 5f
        scope.rotationX = 5f
        scope.rotationY = 5f
        scope.rotationZ = 5f
        scope.cameraDistance = 5f
        scope.transformOrigin = TransformOrigin(0.7f, 0.1f)
        scope.shape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ) = Outline.Rectangle(size.toRect())
        }
        scope.clip = true
        scope.size = Size(100f, 200f)
        scope.reset()
        scope.assertCorrectDefaultValuesAreCorrect()
    }

    @Test
    fun testGraphicsLayerSize() {
        val scope = GraphicsLayerScope() as ReusableGraphicsLayerScope
        scope.size = Size(2560f, 1400f)
        with(scope) {
            assertEquals(2560f, size.width)
            assertEquals(1400f, size.height)
        }
    }

    fun GraphicsLayerScope.assertCorrectDefaultValuesAreCorrect() {
        assertThat(scaleX).isEqualTo(1f)
        assertThat(scaleY).isEqualTo(1f)
        assertThat(alpha).isEqualTo(1f)
        assertThat(translationX).isEqualTo(0f)
        assertThat(translationY).isEqualTo(0f)
        assertThat(shadowElevation).isEqualTo(0f)
        assertThat(rotationX).isEqualTo(0f)
        assertThat(rotationY).isEqualTo(0f)
        assertThat(rotationZ).isEqualTo(0f)
        assertThat(cameraDistance).isEqualTo(DefaultCameraDistance)
        assertThat(transformOrigin).isEqualTo(TransformOrigin.Center)
        assertThat(shape).isEqualTo(RectangleShape)
        assertThat(clip).isEqualTo(false)
        assertThat(size).isEqualTo(Size.Unspecified)
    }
}
