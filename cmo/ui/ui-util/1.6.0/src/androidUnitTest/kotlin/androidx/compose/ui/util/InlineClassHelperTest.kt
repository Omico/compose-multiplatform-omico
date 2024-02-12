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

package androidx.compose.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InlineClassHelperTest {
    @Test
    fun packAndUnpackFloats() {
        val first = Float.MAX_VALUE
        val second = Float.MIN_VALUE
        val packed = packFloats(first, second)
        assertEquals(first, unpackFloat1(packed))
        assertEquals(second, unpackFloat2(packed))
    }

    @Test
    fun packAndUnpackNegativeAndPositiveFloats() {
        val first = -50f
        val second = 100f
        val packed = packFloats(first, second)
        assertEquals(first, unpackFloat1(packed))
        assertEquals(second, unpackFloat2(packed))
    }

    @Test
    fun packAndUnpackPositiveAndNegativeFloats() {
        val first = 50f
        val second = -100f
        val packed = packFloats(first, second)
        assertEquals(first, unpackFloat1(packed))
        assertEquals(second, unpackFloat2(packed))
    }

    @Test
    fun packAndUnpackNegativeFloats() {
        val first = -50f
        val second = -100f
        val packed = packFloats(first, second)
        assertEquals(first, unpackFloat1(packed))
        assertEquals(second, unpackFloat2(packed))
    }

    @Test
    fun packAndUnpackInts() {
        val first = Int.MAX_VALUE
        val second = Int.MIN_VALUE
        val packed = packInts(first, second)
        assertEquals(first, unpackInt1(packed))
        assertEquals(second, unpackInt2(packed))
    }

    @Test
    fun packAndUnpackNegativeAndPositiveInts() {
        val first = -50
        val second = 100
        val packed = packInts(first, second)
        assertEquals(first, unpackInt1(packed))
        assertEquals(second, unpackInt2(packed))
    }

    @Test
    fun packAndUnpackPositiveAndNegativeInts() {
        val first = 50
        val second = -100
        val packed = packInts(first, second)
        assertEquals(first, unpackInt1(packed))
        assertEquals(second, unpackInt2(packed))
    }

    @Test
    fun packAndUnpackNegativeInts() {
        val first = -50
        val second = -100
        val packed = packInts(first, second)
        assertEquals(first, unpackInt1(packed))
        assertEquals(second, unpackInt2(packed))
    }

    @Test
    fun rawBits() {
        val first = Float.NaN
        val second = multZero(Float.POSITIVE_INFINITY)
        val packed = packFloats(first, second)
        assertEquals(first.toRawBits(), unpackFloat1(packed).toRawBits())
        assertEquals(second.toRawBits(), unpackFloat2(packed).toRawBits())
    }

    fun multZero(value: Float) = value * 0f
}
