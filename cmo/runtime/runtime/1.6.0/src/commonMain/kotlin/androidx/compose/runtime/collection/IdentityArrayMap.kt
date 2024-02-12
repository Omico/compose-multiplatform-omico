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

package androidx.compose.runtime.collection

import androidx.compose.runtime.identityHashCode

internal class IdentityArrayMap<Key : Any, Value : Any?>(capacity: Int = 16) {
    var keys = arrayOfNulls<Any?>(capacity)
        private set
    var values = arrayOfNulls<Any?>(capacity)
        private set
    var size = 0
        private set

    fun isEmpty() = size == 0
    fun isNotEmpty() = size > 0

    operator fun contains(key: Key): Boolean = find(key) >= 0

    operator fun get(key: Key): Value? {
        val index = find(key)
        @Suppress("UNCHECKED_CAST")
        return if (index >= 0) values[index] as Value else null
    }

    operator fun set(key: Key, value: Value) {
        val keys = keys
        val values = values
        val size = size

        val index = find(key)
        if (index >= 0) {
            values[index] = value
        } else {
            val insertIndex = -(index + 1)
            val resize = size == keys.size
            val destKeys = if (resize) {
                arrayOfNulls(size * 2)
            } else keys
            keys.copyInto(
                destination = destKeys,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            if (resize) {
                keys.copyInto(
                    destination = destKeys,
                    endIndex = insertIndex
                )
            }
            destKeys[insertIndex] = key
            this.keys = destKeys
            val destValues = if (resize) {
                arrayOfNulls(size * 2)
            } else values
            values.copyInto(
                destination = destValues,
                destinationOffset = insertIndex + 1,
                startIndex = insertIndex,
                endIndex = size
            )
            if (resize) {
                values.copyInto(
                    destination = destValues,
                    endIndex = insertIndex
                )
            }
            destValues[insertIndex] = value
            this.values = destValues
            this.size++
        }
    }

    fun remove(key: Key): Value? {
        val index = find(key)
        if (index >= 0) {
            val value = values[index]
            val size = size
            val keys = keys
            val values = values
            keys.copyInto(
                destination = keys,
                destinationOffset = index,
                startIndex = index + 1,
                endIndex = size
            )
            values.copyInto(
                destination = values,
                destinationOffset = index,
                startIndex = index + 1,
                endIndex = size
            )
            val newSize = size - 1
            keys[newSize] = null
            values[newSize] = null
            this.size = newSize
            @Suppress("UNCHECKED_CAST")
            return value as Value
        }
        return null
    }

    fun clear() {
        size = 0
        keys.fill(null)
        values.fill(null)
    }

    inline fun removeIf(block: (key: Key, value: Value) -> Boolean) {
        var current = 0
        for (index in 0 until size) {
            @Suppress("UNCHECKED_CAST")
            val key = keys[index] as Key
            @Suppress("UNCHECKED_CAST")
            val value = values[index] as Value
            if (!block(key, value)) {
                if (current != index) {
                    keys[current] = key
                    values[current] = values[index]
                }
                current++
            }
        }
        if (size > current) {
            for (index in current until size) {
                keys[index] = null
                values[index] = null
            }
            size = current
        }
    }

    inline fun removeValueIf(block: (value: Value) -> Boolean) {
        removeIf { _, value -> block(value) }
    }

    inline fun forEach(block: (key: Key, value: Value) -> Unit) {
        for (index in 0 until size) {
            @Suppress("UNCHECKED_CAST")
            block(keys[index] as Key, values[index] as Value)
        }
    }

    /**
     * Returns the index into [keys] of the found [key], or the negative index - 1 of the
     * position in which it would be if it were found.
     */
    private fun find(key: Any?): Int {
        val keyIdentity = identityHashCode(key)
        var low = 0
        var high = size - 1

        val keys = keys
        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midKey = keys[mid]
            val midKeyHash = identityHashCode(midKey)
            when {
                midKeyHash < keyIdentity -> low = mid + 1
                midKeyHash > keyIdentity -> high = mid - 1
                key === midKey -> return mid
                else -> return findExactIndex(mid, key, keyIdentity)
            }
        }
        return -(low + 1)
    }

    /**
     * When multiple keys share the same [identityHashCode], then we must find the specific
     * index of the target item. This method assumes that [midIndex] has already been checked
     * for an exact match for [key], but will look at nearby values to find the exact item index.
     * If no match is found, the negative index - 1 of the position in which it would be will
     * be returned, which is always after the last key with the same [identityHashCode].
     */
    private fun findExactIndex(midIndex: Int, key: Any?, keyHash: Int): Int {
        val keys = keys
        val size = size

        // hunt down first
        for (i in midIndex - 1 downTo 0) {
            val k = keys[i]
            if (k === key) {
                return i
            }
            if (identityHashCode(k) != keyHash) {
                break // we've gone too far
            }
        }

        for (i in midIndex + 1 until size) {
            val k = keys[i]
            if (k === key) {
                return i
            }
            if (identityHashCode(k) != keyHash) {
                // We've gone too far. We should insert here.
                return -(i + 1)
            }
        }

        // We should insert at the end
        return -(size + 1)
    }

    @Suppress("UNCHECKED_CAST")
    fun asMap(): Map<Key, Value> = object : Map<Key, Value> {
        override val entries: Set<Map.Entry<Key, Value>>
            get() = object : Set<Map.Entry<Key, Value>> {
                override val size: Int get() = this@IdentityArrayMap.size
                override fun isEmpty(): Boolean = this@IdentityArrayMap.isEmpty()
                override fun iterator(): Iterator<Map.Entry<Key, Value>> =
                    sequence<Map.Entry<Key, Value>> {
                        for (index in 0 until this@IdentityArrayMap.size) {
                            yield(
                                object : Map.Entry<Key, Value> {
                                    override val key: Key =
                                        this@IdentityArrayMap.keys[index] as Key
                                    override val value: Value =
                                        this@IdentityArrayMap.values[index] as Value
                                }
                            )
                        }
                    }.iterator()
                override fun containsAll(elements: Collection<Map.Entry<Key, Value>>): Boolean =
                    elements.all { contains(it) }

                override fun contains(element: Map.Entry<Key, Value>): Boolean =
                    this@IdentityArrayMap[element.key] === element.value
            }

        override val keys: Set<Key> get() = object : Set<Key> {
            override val size: Int get() = this@IdentityArrayMap.size
            override fun isEmpty(): Boolean = this@IdentityArrayMap.isEmpty()
            override fun iterator(): Iterator<Key> = sequence {
                for (index in 0 until this@IdentityArrayMap.size) {
                    yield(this@IdentityArrayMap.keys[index] as Key)
                }
            }.iterator()

            override fun containsAll(elements: Collection<Key>): Boolean {
                for (key in elements) {
                    if (!contains(key)) return false
                }
                return true
            }

            override fun contains(element: Key): Boolean = this@IdentityArrayMap.contains(element)
        }

        override val size: Int get() = this@IdentityArrayMap.size
        override val values: Collection<Value> get() = object : Collection<Value> {
            override val size: Int get() = this@IdentityArrayMap.size
            override fun isEmpty(): Boolean = this@IdentityArrayMap.isEmpty()
            override fun iterator(): Iterator<Value> = sequence {
                for (index in 0 until this@IdentityArrayMap.size) {
                    yield(this@IdentityArrayMap.values[index] as Value)
                }
            }.iterator()

            override fun containsAll(elements: Collection<Value>): Boolean {
                for (value in elements) {
                    if (!contains(value)) return false
                }
                return true
            }

            override fun contains(element: Value): Boolean {
                for (index in 0 until this@IdentityArrayMap.size) {
                    if (this@IdentityArrayMap.values[index] == element) return true
                }
                return false
            }
        }

        override fun isEmpty(): Boolean = this@IdentityArrayMap.isEmpty()
        override fun get(key: Key): Value? = this@IdentityArrayMap[key]
        override fun containsValue(value: Value): Boolean =
            this@IdentityArrayMap.values.contains(value)
        override fun containsKey(key: Key): Boolean =
            this@IdentityArrayMap[key] != null
    }
}
