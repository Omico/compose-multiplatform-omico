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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PaintTest {

    @Test
    fun testPaintAntiAlias() {
        val paint = Paint()
        assertTrue(paint.isAntiAlias)
    }

    @Test
    fun testNullPathEffectAssignmentDoesNotCrash() {
        val paint = Paint()
        try {
            paint.pathEffect = null
        } catch (e: NullPointerException) {
            fail("Null path effect should not throw")
        }
    }

    @Test
    fun testPathGetterSetter() {
        val paint = Paint()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 20f)
        paint.pathEffect = pathEffect
        assertTrue(pathEffect === paint.pathEffect)
    }

    @Test
    fun testDitheringEnabledByDefault() {
        assertTrue(Paint().asFrameworkPaint().isDither)
    }

    @Test
    fun testFilterBitmapEnabledByDefault() {
        assertTrue(Paint().asFrameworkPaint().isFilterBitmap)
    }

    @Test
    fun testToComposePaintForColor() {
        val nativePaint = android.graphics.Paint()
        val composePaint = nativePaint.asComposePaint()
        composePaint.color = Color(android.graphics.Color.GREEN)
        assertEquals(nativePaint.color, android.graphics.Color.GREEN)
    }

    @Test
    fun testToComposePaintForShader() {
        val nativePaint = android.graphics.Paint()
        val composePaint = nativePaint.asComposePaint()
        val green = android.graphics.Color.GREEN
        val red = android.graphics.Color.RED
        val shader = android.graphics.LinearGradient(
            0f,
            0f,
            1f,
            1f,
            green,
            red,
            android.graphics.Shader.TileMode.MIRROR
        )
        composePaint.shader = shader
        assertSame(composePaint.shader, nativePaint.shader)
    }
}
