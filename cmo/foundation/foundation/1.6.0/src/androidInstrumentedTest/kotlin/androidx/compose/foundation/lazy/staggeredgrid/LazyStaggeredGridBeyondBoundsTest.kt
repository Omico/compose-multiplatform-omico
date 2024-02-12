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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.list.PlacementComparator
import androidx.compose.foundation.lazy.list.TrackPlacedElement
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Above
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.After
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Before
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Below
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Left
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Right
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridBeyondBoundsTest(param: Param) {

    @get:Rule
    val rule = createComposeRule()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(
        val beyondBoundsLayoutDirection: BeyondBoundsLayout.LayoutDirection,
        val reverseLayout: Boolean,
        val layoutDirection: LayoutDirection,
    ) {
        override fun toString() = "beyondBoundsLayoutDirection=$beyondBoundsLayoutDirection " +
            "reverseLayout=$reverseLayout " +
            "layoutDirection=$layoutDirection"
    }

    private val beyondBoundsLayoutDirection = param.beyondBoundsLayoutDirection
    private val reverseLayout = param.reverseLayout
    private val layoutDirection = param.layoutDirection
    private val placedItems = sortedMapOf<Int, Rect>()
    private var beyondBoundsLayout: BeyondBoundsLayout? = null
    private lateinit var lazyStaggeredGridState: LazyStaggeredGridState
    private val placementComparator =
        PlacementComparator(beyondBoundsLayoutDirection, layoutDirection, reverseLayout)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = buildList {
            for (beyondBoundsLayoutDirection in listOf(Left, Right, Above, Below, Before, After)) {
                for (reverseLayout in listOf(false, true)) {
                    for (layoutDirection in listOf(Ltr, Rtl)) {
                        add(Param(beyondBoundsLayoutDirection, reverseLayout, layoutDirection))
                    }
                }
            }
        }
    }

    @Test
    fun onlyOneVisibleItemIsPlaced() {
        // Arrange.
        rule.setLazyContent(size = 10.toDp(), firstVisibleItem = 0) {
            items(100) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index)
                )
            }
        }

        // Assert.
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(0)
            assertThat(visibleItems).containsExactly(0)
        }
    }

    @Test
    fun onlyTwoVisibleItemsArePlaced() {
        // Arrange.
        rule.setLazyContent(size = 20.toDp(), firstVisibleItem = 0) {
            items(100) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index)
                )
            }
        }

        // Assert.
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(0, 1)
            assertThat(visibleItems).containsExactly(0, 1)
        }
    }

    @Test
    fun onlyThreeVisibleItemsArePlaced() {
        // Arrange.
        rule.setLazyContent(size = 30.toDp(), firstVisibleItem = 0) {
            items(100) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index)
                )
            }
        }

        // Assert.
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(0, 1, 2)
            assertThat(visibleItems).containsExactly(0, 1, 2)
        }
    }

    @Test
    fun emptyLazyList_doesNotCrash() {
        // Arrange.
        var addItems by mutableStateOf(true)
        lateinit var beyondBoundsLayoutRef: BeyondBoundsLayout
        rule.setLazyContent(size = 30.toDp(), firstVisibleItem = 0) {
            if (addItems) {
                item {
                    Box(
                        Modifier.modifierLocalConsumer {
                            beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                        }
                    )
                }
            }
        }
        rule.runOnIdle {
            beyondBoundsLayoutRef = beyondBoundsLayout!!
            addItems = false
        }

        // Act.
        val hasMoreContent = rule.runOnIdle {
            beyondBoundsLayoutRef.layout(beyondBoundsLayoutDirection) {
                hasMoreContent
            }
        }

        // Assert.
        rule.runOnIdle {
            assertThat(hasMoreContent).isFalse()
        }
    }

    @Test
    fun oneExtraItemBeyondVisibleBounds() {
        // Arrange.
        rule.setLazyContent(size = 30.toDp(), firstVisibleItem = 5) {
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index)
                )
            }
            item {
                Box(Modifier
                    .size(10.toDp())
                    .trackPlaced(5)
                    .modifierLocalConsumer {
                        beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                    }
                )
            }
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index + 6)
                )
            }
        }

        // Act.
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                // Assert that the beyond bounds items are present.
                if (expectedExtraItemsBeforeVisibleBounds()) {
                    assertThat(placedItems.keys).containsExactly(4, 5, 6, 7)
                } else {
                    assertThat(placedItems.keys).containsExactly(5, 6, 7, 8)
                }
                assertThat(visibleItems).containsExactly(5, 6, 7)

                assertThat(placedItems.values).isInOrder(placementComparator)

                // Just return true so that we stop as soon as we run this once.
                // This should result in one extra item being added.
                true
            }
        }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(5, 6, 7)
            assertThat(visibleItems).containsExactly(5, 6, 7)
        }
    }

    @Test
    fun oneExtraItemBeyondVisibleBounds_multipleCells() {
        val itemSize = 50
        val itemSizeDp = itemSize.toDp()
        // Arrange.
        rule.setLazyContent(cells = 2, size = itemSizeDp * 3, firstVisibleItem = 10) {
            // item | item  | x5
            // item | local | x1
            // item | item  | x5
            items(11) { index ->
                Box(
                    Modifier
                        .size(itemSizeDp)
                        .trackPlaced(index)
                )
            }
            item {
                Box(Modifier
                    .size(itemSizeDp)
                    .trackPlaced(11)
                    .modifierLocalConsumer {
                        beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                    }
                )
            }
            items(10) { index ->
                Box(
                    Modifier
                        .size(itemSizeDp)
                        .trackPlaced(index + 12)
                )
            }
        }

        // Act.
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                // Assert that the beyond bounds items are present.
                if (expectedExtraItemsBeforeVisibleBounds()) {
                    assertThat(placedItems.keys).containsExactly(9, 10, 11, 12, 13, 14, 15)
                } else {
                    assertThat(placedItems.keys).containsExactly(10, 11, 12, 13, 14, 15, 16)
                }
                assertThat(visibleItems).containsExactly(10, 11, 12, 13, 14, 15)

                assertThat(placedItems.values).isInOrder(placementComparator)

                // Just return true so that we stop as soon as we run this once.
                // This should result in one extra item being added.
                true
            }
        }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(10, 11, 12, 13, 14, 15)
            assertThat(visibleItems).containsExactly(10, 11, 12, 13, 14, 15)
        }
    }

    @Test
    fun oneExtraItemBeyondVisibleBounds_multipleCells_staggered() {
        val itemSize = 50
        val itemSizeDp = itemSize.toDp()
        // Arrange.
        rule.setLazyContent(cells = 3, size = itemSizeDp * 2, firstVisibleItem = 4) {
            // -------------
            // |   | 1 |   |
            // | 0 |---| 2 |
            // |   | 3 |   |
            // |-----------|
            // |     4     |
            // |-----------|
            // |   | 6 |   |
            // | 5 |---| 7 |
            // |   | 8 |   |
            // -------------
            items(4) { index ->
                Box(
                    Modifier
                        .size(itemSizeDp * if (index % 2 == 0) 2f else 1f)
                        .trackPlaced(index)
                )
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(Modifier
                    .size(itemSizeDp)
                    .trackPlaced(4)
                    .modifierLocalConsumer {
                        beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                    }
                )
            }
            items(4) { index ->
                Box(
                    Modifier
                        .size(itemSizeDp * if (index % 2 == 0) 2f else 1f)
                        .trackPlaced(index + 5)
                )
            }
        }

        // Act.
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                // Assert that the beyond bounds items are present.
                if (expectedExtraItemsBeforeVisibleBounds()) {
                    assertThat(placedItems.keys).containsExactly(3, 4, 5, 6, 7)
                    assertThat(visibleItems).containsExactly(4, 5, 6, 7)
                } else {
                    assertThat(placedItems.keys).containsExactly(4, 5, 6, 7, 8)
                    assertThat(visibleItems).containsExactly(4, 5, 6, 7)
                }
                // Just return true so that we stop as soon as we run this once.
                // This should result in one extra item being added.
                true
            }
        }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(4, 5, 6, 7)
            assertThat(visibleItems).containsExactly(4, 5, 6, 7)
        }
    }

    @Test
    fun twoExtraItemsBeyondVisibleBounds() {
        // Arrange.
        var extraItemCount = 2
        rule.setLazyContent(size = 30.toDp(), firstVisibleItem = 5) {
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index)
                )
            }
            item {
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(5)
                        .modifierLocalConsumer {
                            beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                        }
                )
            }
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index + 6)
                )
            }
        }

        // Act.
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                if (--extraItemCount > 0) {
                    // Return null to continue the search.
                    null
                } else {
                    // Assert that the beyond bounds items are present.
                    if (expectedExtraItemsBeforeVisibleBounds()) {
                        assertThat(placedItems.keys).containsExactly(3, 4, 5, 6, 7)
                    } else {
                        assertThat(placedItems.keys).containsExactly(5, 6, 7, 8, 9)
                    }
                    assertThat(visibleItems).containsExactly(5, 6, 7)

                    assertThat(placedItems.values).isInOrder(placementComparator)

                    // Return true to stop the search.
                    true
                }
            }
        }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(5, 6, 7)
            assertThat(visibleItems).containsExactly(5, 6, 7)
        }
    }

    @Test
    fun allBeyondBoundsItemsInSpecifiedDirection() {
        // Arrange.
        rule.setLazyContent(size = 30.toDp(), firstVisibleItem = 5) {
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index)
                )
            }
            item {
                Box(
                    Modifier
                        .size(10.toDp())
                        .modifierLocalConsumer {
                            beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                        }
                        .trackPlaced(5)
                )
            }
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index + 6)
                )
            }
        }

        // Act.
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                if (hasMoreContent) {
                    // Just return null so that we keep adding more items till we reach the end.
                    null
                } else {
                    // Assert that the beyond bounds items are present.
                    if (expectedExtraItemsBeforeVisibleBounds()) {
                        assertThat(placedItems.keys).containsExactly(0, 1, 2, 3, 4, 5, 6, 7)
                    } else {
                        assertThat(placedItems.keys).containsExactly(5, 6, 7, 8, 9, 10)
                    }
                    assertThat(visibleItems).containsExactly(5, 6, 7)

                    // Verify if the placed item offsets are in order.
                    assertThat(placedItems.toSortedMap().values).isInOrder(placementComparator)

                    // Return true to end the search.
                    true
                }
            }
        }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(5, 6, 7)
        }
    }

    @Test
    fun beyondBoundsLayoutRequest_inDirectionPerpendicularToLazyListOrientation() {
        // Arrange.
        var beyondBoundsLayoutCount = 0
        rule.setLazyContentInPerpendicularDirection(size = 30.toDp(), firstVisibleItem = 5) {
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index)
                )
            }
            item {
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(5)
                        .modifierLocalConsumer {
                            beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                        }
                )
            }
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index + 6)
                )
            }
        }
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(5, 6, 7)
            assertThat(visibleItems).containsExactly(5, 6, 7)
        }

        // Act.
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                beyondBoundsLayoutCount++
                when (beyondBoundsLayoutDirection) {
                    Left, Right, Above, Below -> {
                        assertThat(placedItems.keys).containsExactly(5, 6, 7)
                        assertThat(visibleItems).containsExactly(5, 6, 7)
                    }
                    Before, After -> {
                        if (expectedExtraItemsBeforeVisibleBounds()) {
                            assertThat(placedItems.keys).containsExactly(4, 5, 6, 7)
                            assertThat(visibleItems).containsExactly(5, 6, 7)
                        } else {
                            assertThat(placedItems.keys).containsExactly(5, 6, 7, 8)
                            assertThat(visibleItems).containsExactly(5, 6, 7)
                        }
                    }
                }
                // Just return true so that we stop as soon as we run this once.
                // This should result in one extra item being added.
                true
            }
        }

        rule.runOnIdle {
            when (beyondBoundsLayoutDirection) {
                Left, Right, Above, Below -> {
                    assertThat(beyondBoundsLayoutCount).isEqualTo(0)
                }
                Before, After -> {
                    assertThat(beyondBoundsLayoutCount).isEqualTo(1)

                    // Assert that the beyond bounds items are removed.
                    assertThat(placedItems.keys).containsExactly(5, 6, 7)
                    assertThat(visibleItems).containsExactly(5, 6, 7)
                }
                else -> error("Unsupported BeyondBoundsLayoutDirection")
            }
        }
    }

    @Test
    fun returningNullDoesNotCauseInfiniteLoop() {
        // Arrange.
        rule.setLazyContent(size = 30.toDp(), firstVisibleItem = 5) {
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index)
                )
            }
            item {
                Box(
                    Modifier
                        .size(10.toDp())
                        .modifierLocalConsumer {
                            beyondBoundsLayout = ModifierLocalBeyondBoundsLayout.current
                        }
                        .trackPlaced(5)
                )
            }
            items(5) { index ->
                Box(
                    Modifier
                        .size(10.toDp())
                        .trackPlaced(index + 6)
                )
            }
        }

        // Act.
        var count = 0
        rule.runOnUiThread {
            beyondBoundsLayout!!.layout(beyondBoundsLayoutDirection) {
                // Assert that we don't keep iterating when there is no ending condition.
                assertThat(count++).isLessThan(lazyStaggeredGridState.layoutInfo.totalItemsCount)
                // Always return null to continue the search.
                null
            }
        }

        // Assert that the beyond bounds items are removed.
        rule.runOnIdle {
            assertThat(placedItems.keys).containsExactly(5, 6, 7)
            assertThat(visibleItems).containsExactly(5, 6, 7)
        }
    }

    private fun ComposeContentTestRule.setLazyContent(
        size: Dp,
        firstVisibleItem: Int,
        cells: Int = 1,
        content: LazyStaggeredGridScope.() -> Unit
    ) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                lazyStaggeredGridState = rememberLazyStaggeredGridState(firstVisibleItem)
                when (beyondBoundsLayoutDirection) {
                    Left, Right, Before, After ->
                        LazyHorizontalStaggeredGrid(
                            rows = StaggeredGridCells.Fixed(cells),
                            modifier = Modifier.size(size),
                            state = lazyStaggeredGridState,
                            reverseLayout = reverseLayout,
                            content = content
                        )
                    Above, Below ->
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(cells),
                            modifier = Modifier.size(size),
                            state = lazyStaggeredGridState,
                            reverseLayout = reverseLayout,
                            content = content
                        )
                    else -> unsupportedDirection()
                }
            }
        }
    }

    private fun ComposeContentTestRule.setLazyContentInPerpendicularDirection(
        size: Dp,
        firstVisibleItem: Int,
        content: LazyStaggeredGridScope.() -> Unit
    ) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                lazyStaggeredGridState = rememberLazyStaggeredGridState(firstVisibleItem)
                when (beyondBoundsLayoutDirection) {
                    Left, Right, Before, After ->
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(1),
                            modifier = Modifier.size(size),
                            state = lazyStaggeredGridState,
                            reverseLayout = reverseLayout,
                            content = content
                        )
                    Above, Below ->
                        LazyHorizontalStaggeredGrid(
                            rows = StaggeredGridCells.Fixed(1),
                            modifier = Modifier.size(size),
                            state = lazyStaggeredGridState,
                            reverseLayout = reverseLayout,
                            content = content
                        )
                    else -> unsupportedDirection()
                }
            }
        }
    }

    private fun Int.toDp(): Dp = with(rule.density) { toDp() }

    private val visibleItems: List<Int>
        get() = lazyStaggeredGridState.layoutInfo.visibleItemsInfo.map { it.index }

    private fun expectedExtraItemsBeforeVisibleBounds() = when (beyondBoundsLayoutDirection) {
        Right -> if (layoutDirection == Ltr) reverseLayout else !reverseLayout
        Left -> if (layoutDirection == Ltr) !reverseLayout else reverseLayout
        Above -> !reverseLayout
        Below -> reverseLayout
        After -> false
        Before -> true
        else -> error("Unsupported BeyondBoundsDirection")
    }

    private fun unsupportedDirection(): Nothing = error(
        "Lazy list does not support beyond bounds layout for the specified direction"
    )

    private fun Modifier.trackPlaced(index: Int): Modifier =
        this then TrackPlacedElement(index, placedItems)
}
