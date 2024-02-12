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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberLazyListBeyondBoundsState(
    state: LazyListState,
    beyondBoundsItemCount: Int
): LazyLayoutBeyondBoundsState {
    return remember(state, beyondBoundsItemCount) {
        LazyListBeyondBoundsState(state, beyondBoundsItemCount)
    }
}

internal class LazyListBeyondBoundsState(
    val state: LazyListState,
    val beyondBoundsItemCount: Int
) : LazyLayoutBeyondBoundsState {

    override fun remeasure() {
        state.remeasurement?.forceRemeasure()
    }

    override val itemCount: Int
        get() = state.layoutInfo.totalItemsCount
    override val hasVisibleItems: Boolean
        get() = state.layoutInfo.visibleItemsInfo.isNotEmpty()
    override val firstPlacedIndex: Int
        get() = maxOf(0, state.firstVisibleItemIndex - beyondBoundsItemCount)
    override val lastPlacedIndex: Int
        get() = minOf(
            itemCount - 1,
            state.layoutInfo.visibleItemsInfo.last().index + beyondBoundsItemCount
        )
}
