/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime.collection

internal actual class IntMap<E> actual constructor(initialCapacity: Int) {
    private val DELETED = Any()
    private var keys = IntArray(initialCapacity)
    private var _size = 0
    private var values = Array<Any?>(initialCapacity) { null }

    /**
     * True if this map contains key
     */
    actual operator fun contains(key: Int): Boolean {
        val index = keys.binarySearch(_size, key)
        return index >= 0 && values[index] !== DELETED
    }

    /**
     * Get [key] or null
     */
    actual operator fun get(key: Int): E? {
        val index = keys.binarySearch(_size, key)
        return if (index >= 0 && values[index] !== DELETED) {
            @Suppress("UNCHECKED_CAST")
            values[index] as E
        } else {
            null
        }
    }

    /**
     * Get [key] or [valueIfAbsent]
     */
    actual fun get(key: Int, valueIfAbsent: E): E {
        val index = keys.binarySearch(_size, key)
        return if (index >= 0 && values[index] !== DELETED) {
            @Suppress("UNCHECKED_CAST")
            values[index] as E
        } else {
            valueIfAbsent
        }
    }

    /**
     * Set [key] to [value]
     */
    actual operator fun set(key: Int, value: E) {
        var index = keys.binarySearch(_size, key)
        if (index >= 0) {
            values[index] = value
        } else {
            index = -index - 1
            keys = keys.insert(_size, index, key)
            values = values.insert(_size, index, value)
            _size++
        }
    }

    /**
     * Remove key, if it exists
     *
     * Otherwise no op
     */
    actual fun remove(key: Int) {
        // note this never GCs
        val index = keys.binarySearch(_size, key)
        if (index >= 0) {
            values[index] = DELETED
            _size--
        }
    }

    /**
     * Clear this map
     */
    actual fun clear() {
        _size = 0
        for (i in keys.indices) {
            keys[i] = 0
        }
        for (i in values.indices) {
            values[i] = null
        }
    }

    /**
     * Current count of (key, value) pairs
     */
    actual val size: Int
        get() = _size
}

private fun IntArray.binarySearch(size: Int, value: Int): Int {
    var min = 0
    var max = size - 1
    while (min <= max) {
        val mid = (min + max) / 2
        val midValue = this[mid]
        if (midValue < value) {
            min = mid + 1
        } else if (midValue > value) {
            max = mid - 1
        } else {
            return mid
        }
    }
    return -(min + 1)
}

private fun IntArray.insert(currentSize: Int, index: Int, value: Int): IntArray {
    if (currentSize + 1 <= size) {
        if (index < currentSize) {
            System.arraycopy(this, index, this, index + 1, currentSize - index)
        }
        this[index] = value
        return this
    }

    val result = IntArray(size * 2)
    System.arraycopy(this, 0, result, 0, index)
    result[index] = value
    System.arraycopy(this, index, result, index + 1, size - index)
    return result
}

private fun Array<Any?>.insert(currentSize: Int, index: Int, value: Any?): Array<Any?> {
    if (currentSize + 1 <= size) {
        if (index < currentSize) {
            System.arraycopy(this, index, this, index + 1, currentSize - index)
        }
        this[index] = value
        return this
    }

    val result = Array<Any?>(size * 2) { null }
    System.arraycopy(this, 0, result, 0, index)
    result[index] = value
    System.arraycopy(this, index, result, index + 1, size - index)
    return result
}
