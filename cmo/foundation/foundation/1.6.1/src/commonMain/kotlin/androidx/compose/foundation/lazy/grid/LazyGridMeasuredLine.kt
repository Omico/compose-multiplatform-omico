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

package androidx.compose.foundation.lazy.grid

/**
 * Represents one measured line of the lazy list. Each item on the line can in fact consist of
 * multiple placeables if the user emit multiple layout nodes in the item callback.
 */
internal class LazyGridMeasuredLine constructor(
    val index: Int,
    val items: Array<LazyGridMeasuredItem>,
    private val slots: LazyGridSlots,
    private val spans: List<GridItemSpan>,
    private val isVertical: Boolean,
    /**
     * Spacing to be added after [mainAxisSize], in the main axis direction.
     */
    private val mainAxisSpacing: Int,
) {
    /**
     * Main axis size of the line - the max main axis size of the items on the line.
     */
    val mainAxisSize: Int

    /**
     * Sum of [mainAxisSpacing] and the max of the main axis sizes of the placeables on the line.
     */
    val mainAxisSizeWithSpacings: Int

    init {
        var maxMainAxis = 0
        items.forEach { item ->
            maxMainAxis = maxOf(maxMainAxis, item.mainAxisSize)
        }
        mainAxisSize = maxMainAxis
        mainAxisSizeWithSpacings = (maxMainAxis + mainAxisSpacing).coerceAtLeast(0)
    }

    /**
     * Whether this line contains any items.
     */
    fun isEmpty() = items.isEmpty()

    /**
     * Calculates positions for the [items] at [offset] main axis position.
     * If [reverseOrder] is true the [items] would be placed in the inverted order.
     */
    fun position(
        offset: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ): Array<LazyGridMeasuredItem> {
        var usedSpan = 0
        items.forEachIndexed { itemIndex, item ->
            val span = spans[itemIndex].currentLineSpan
            val startSlot = usedSpan

            item.position(
                mainAxisOffset = offset,
                crossAxisOffset = slots.positions[startSlot],
                layoutWidth = layoutWidth,
                layoutHeight = layoutHeight,
                row = if (isVertical) index else startSlot,
                column = if (isVertical) startSlot else index
            ).also {
                usedSpan += span
            }
        }
        return items
    }
}
