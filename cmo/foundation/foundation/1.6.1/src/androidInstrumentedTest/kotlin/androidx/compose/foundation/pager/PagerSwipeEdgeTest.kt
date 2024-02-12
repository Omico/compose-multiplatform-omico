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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PagerSwipeEdgeTest(
    val config: ParamConfig
) : BasePagerTest(config) {

    @Test
    fun swipePageTowardsEdge_shouldNotMove() {
        // Arrange
        createPager(modifier = Modifier.fillMaxSize())
        val delta = pagerSize * 0.4f * scrollForwardSign

        // Act - backward
        rule.onNodeWithTag("0").performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 1.5f * MinFlingVelocityDp.toPx() },
                delta * -1.0f
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("0").assertIsDisplayed()
        confirmPageIsInCorrectPosition(0)

        // Act - forward
        onPager().performTouchInput {
            swipeWithVelocityAcrossMainAxis(
                with(rule.density) { 1.5f * MinFlingVelocityDp.toPx() },
                delta
            )
        }
        rule.waitForIdle()

        // Assert
        rule.onNodeWithTag("1").assertIsDisplayed()
        confirmPageIsInCorrectPosition(1)
    }

    @Test
    fun scrollForwardAtTheLastPage_withSpacing_shouldNotMovePage() {

        createPager(
            modifier = Modifier.fillMaxSize(),
            initialPage = DefaultPageCount - 1,
            pageSpacing = 40.dp // use a large spacing
        )

        val delta = pageSize * 0.4f * scrollForwardSign
        val offsetDelta = if (vertical) Offset(0f, delta) else Offset(delta, 0f)

        onPager().performTouchInput {
            down(center)
            moveBy(offsetDelta)
        }

        assertTrue { pagerState.currentPageOffsetFraction == 0.0f } // page didn't move
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (reverseLayout in TestReverseLayout) {
                    for (layoutDirection in TestLayoutDirection) {
                        add(
                            ParamConfig(
                                orientation = orientation,
                                reverseLayout = reverseLayout,
                                layoutDirection = layoutDirection
                            )
                        )
                    }
                }
            }
        }
    }
}
