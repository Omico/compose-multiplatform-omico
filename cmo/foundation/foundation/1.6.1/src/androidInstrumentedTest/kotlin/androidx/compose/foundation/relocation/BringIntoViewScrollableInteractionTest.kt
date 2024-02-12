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

package androidx.compose.foundation.relocation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollingLayoutElement
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class BringIntoViewScrollableInteractionTest(private val orientation: Orientation) {

    @get:Rule
    val rule = createComposeRule()

    private val parentBox = "parent box"
    private val childBox = "child box"

    /**
     * Captures a scope from inside the composition for [runBlockingAndAwaitIdle].
     * Make sure to call [setContentAndInitialize] instead of calling `rule.setContent` to
     * initialize this.
     */
    private lateinit var testScope: CoroutineScope

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Orientation> = arrayOf(Horizontal, Vertical)
    }

    @Test
    fun noScrollableParent_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.size(100.toDp(), 50.toDp())
                            Vertical -> Modifier.size(50.toDp(), 100.toDp())
                        }
                    )
                    .testTag(parentBox)
                    .background(LightGray)
            ) {
                Box(
                    Modifier
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun noScrollableParent_itemNotVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.size(100.toDp(), 50.toDp())
                            Vertical -> Modifier.size(50.toDp(), 100.toDp())
                        }
                    )
                    .testTag(parentBox)
                    .background(LightGray)
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.offset(x = 150.toDp())
                                Vertical -> Modifier.offset(y = 150.toDp())
                            }
                        )
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemAtLeadingEdge_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(rememberScrollState())

                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemAtTrailingEdge_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(rememberScrollState())

                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.offset(x = 50.toDp())
                                Vertical -> Modifier.offset(y = 50.toDp())
                            }
                        )
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemAtCenter_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(rememberScrollState())

                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.offset(x = 25.toDp())
                                Vertical -> Modifier.offset(y = 25.toDp())
                            }
                        )
                        .size(50.toDp())
                        .background(Blue)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                )
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemBiggerThanParentAtLeadingEdge_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        setContentAndInitialize {
            Box(
                Modifier
                    .size(50.toDp())
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(rememberScrollState())
                            Vertical -> Modifier.verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                // Using a multi-colored item to make sure we can assert that the right part of
                // the item is visible.
                RowOrColumn(
                    Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                ) {
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Blue)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Green)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Red)
                    )
                }
            }
        }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemBiggerThanParentAtTrailingEdge_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .size(50.toDp())
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(scrollState)
                            Vertical -> Modifier.verticalScroll(scrollState)
                        }
                    )
            ) {
                // Using a multi-colored item to make sure we can assert that the right part of
                // the item is visible.
                RowOrColumn(
                    Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                ) {
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Red)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Green)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Blue)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun itemBiggerThanParentAtCenter_alreadyVisible_noChange() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .size(50.toDp())
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(scrollState)
                            Vertical -> Modifier.verticalScroll(scrollState)
                        }
                    )
            ) {
                // Using a multi-colored item to make sure we can assert that the right part of
                // the item is visible.
                RowOrColumn(
                    Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .testTag(childBox)
                ) {
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Green)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Blue)
                    )
                    Box(
                        Modifier
                            .size(50.toDp())
                            .background(Red)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue / 2) }
        val startingBounds = getUnclippedBoundsInRoot(childBox)

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        assertThat(getUnclippedBoundsInRoot(childBox)).isEqualTo(startingBounds)
        assertChildMaxInView()
    }

    @Test
    fun childBeforeVisibleBounds_parentIsScrolledSoThatLeadingEdgeOfChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(scrollState)

                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(scrollState)
                        }
                    )
            ) {
                Box(
                    when (orientation) {
                        Horizontal -> Modifier.size(200.toDp(), 50.toDp())
                        Vertical -> Modifier.size(50.toDp(), 200.toDp())
                    }
                ) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 50.toDp())
                                    Vertical -> Modifier.offset(y = 50.toDp())
                                }
                            )
                            .size(50.toDp())
                            .background(Blue)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .testTag(childBox)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    @Test
    fun childAfterVisibleBounds_parentIsScrolledSoThatTrailingEdgeOfChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(scrollState)

                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(scrollState)
                        }
                    )
            ) {
                Box(
                    when (orientation) {
                        Horizontal -> Modifier.size(200.toDp(), 50.toDp())
                        Vertical -> Modifier.size(50.toDp(), 200.toDp())
                    }
                ) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 150.toDp())
                                    Vertical -> Modifier.offset(y = 150.toDp())
                                }
                            )
                            .size(50.toDp())
                            .background(Blue)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .testTag(childBox)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(
            expectedLeft = if (orientation == Horizontal) 50.toDp() else 0.toDp(),
            expectedTop = if (orientation == Horizontal) 0.toDp() else 50.toDp()
        )
        assertChildMaxInView()
    }

    @Test
    fun childPartiallyVisible_parentIsScrolledSoThatLeadingEdgeOfChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(scrollState)

                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(scrollState)
                        }
                    )
            ) {
                Box(Modifier.size(200.toDp())) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 25.toDp())
                                    Vertical -> Modifier.offset(y = 25.toDp())
                                }
                            )
                            .size(50.toDp())
                            .background(Blue)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .testTag(childBox)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue / 2) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    @Test
    fun childPartiallyVisible_parentIsScrolledSoThatTrailingEdgeOfChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var scrollState: ScrollState
        setContentAndInitialize {
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(scrollState)

                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(scrollState)
                        }
                    )
            ) {
                Box(
                    when (orientation) {
                        Horizontal -> Modifier.size(200.toDp(), 50.toDp())
                        Vertical -> Modifier.size(50.toDp(), 200.toDp())
                    }
                ) {
                    Box(
                        Modifier
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(x = 150.toDp())
                                    Vertical -> Modifier.offset(y = 150.toDp())
                                }
                            )
                            .size(50.toDp())
                            .background(Blue)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .testTag(childBox)
                    )
                }
            }
        }
        runBlockingAndAwaitIdle { scrollState.scrollTo(scrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(
            expectedLeft = if (orientation == Horizontal) 50.toDp() else 0.toDp(),
            expectedTop = if (orientation == Horizontal) 0.toDp() else 50.toDp()
        )
        assertChildMaxInView()
    }

    @Test
    fun multipleParentsAreScrolledSoThatChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var parentScrollState: ScrollState
        lateinit var grandParentScrollState: ScrollState
        setContentAndInitialize {
            parentScrollState = rememberScrollState()
            grandParentScrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .horizontalScroll(grandParentScrollState)

                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .verticalScroll(grandParentScrollState)
                        }
                    )
            ) {
                Box(
                    Modifier
                        .background(LightGray)
                        .then(
                            when (orientation) {
                                Horizontal ->
                                    Modifier
                                        .size(200.toDp(), 50.toDp())
                                        .horizontalScroll(parentScrollState)

                                Vertical ->
                                    Modifier
                                        .size(50.toDp(), 200.toDp())
                                        .verticalScroll(parentScrollState)
                            }
                        )
                ) {
                    Box(
                        when (orientation) {
                            Horizontal -> Modifier.size(400.toDp(), 50.toDp())
                            Vertical -> Modifier.size(50.toDp(), 400.toDp())
                        }
                    ) {
                        Box(
                            Modifier
                                .then(
                                    when (orientation) {
                                        Horizontal -> Modifier.offset(x = 25.toDp())
                                        Vertical -> Modifier.offset(y = 25.toDp())
                                    }
                                )
                                .size(50.toDp())
                                .background(Blue)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .testTag(childBox)
                        )
                    }
                }
            }
        }
        runBlockingAndAwaitIdle { parentScrollState.scrollTo(parentScrollState.maxValue) }
        runBlockingAndAwaitIdle { grandParentScrollState.scrollTo(grandParentScrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    @Test
    fun multipleParentsAreScrolledInDifferentDirectionsSoThatChildIsVisible() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var parentScrollState: ScrollState
        lateinit var grandParentScrollState: ScrollState
        setContentAndInitialize {
            parentScrollState = rememberScrollState()
            grandParentScrollState = rememberScrollState()
            Box(
                Modifier
                    .testTag(parentBox)
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal ->
                                Modifier
                                    .size(100.toDp(), 50.toDp())
                                    .verticalScroll(grandParentScrollState)

                            Vertical ->
                                Modifier
                                    .size(50.toDp(), 100.toDp())
                                    .horizontalScroll(grandParentScrollState)
                        }
                    )
            ) {
                Box(
                    Modifier
                        .size(100.toDp())
                        .background(LightGray)
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.horizontalScroll(parentScrollState)
                                Vertical -> Modifier.verticalScroll(parentScrollState)
                            }
                        )
                ) {
                    Box(Modifier.size(200.toDp())) {
                        Box(
                            Modifier
                                .offset(x = 25.toDp(), y = 25.toDp())
                                .size(50.toDp())
                                .background(Blue)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .testTag(childBox)
                        )
                    }
                }
            }
        }
        runBlockingAndAwaitIdle { parentScrollState.scrollTo(parentScrollState.maxValue) }
        runBlockingAndAwaitIdle { grandParentScrollState.scrollTo(grandParentScrollState.maxValue) }

        // Act.
        runBlockingAndAwaitIdle { bringIntoViewRequester.bringIntoView() }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    @Test
    fun specifiedPartOfComponentBroughtOnScreen() {
        // Arrange.
        val bringIntoViewRequester = BringIntoViewRequester()
        lateinit var density: Density
        setContentAndInitialize {
            density = LocalDensity.current
            Box(
                Modifier
                    .testTag(parentBox)
                    .size(50.toDp())
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(rememberScrollState())
                            Vertical -> Modifier.verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.size(150.toDp(), 50.toDp())
                                Vertical -> Modifier.size(50.toDp(), 150.toDp())
                            }
                        )
                        .bringIntoViewRequester(bringIntoViewRequester)
                ) {
                    Box(
                        Modifier
                            .size(50.toDp())
                            .then(
                                when (orientation) {
                                    Horizontal -> Modifier.offset(50.toDp(), 0.toDp())
                                    Vertical -> Modifier.offset(0.toDp(), 50.toDp())
                                }
                            )
                            .background(Blue)
                            .testTag(childBox)
                    )
                }
            }
        }

        // Act.
        runBlockingAndAwaitIdle {
            val rect = with(density) {
                when (orientation) {
                    Horizontal -> DpRect(50.toDp(), 0.toDp(), 100.toDp(), 50.toDp()).toRect()
                    Vertical -> DpRect(0.toDp(), 50.toDp(), 50.toDp(), 100.toDp()).toRect()
                }
            }
            bringIntoViewRequester.bringIntoView(rect)
        }

        // Assert.
        rule.onNodeWithTag(childBox).assertPositionInRootIsEqualTo(0.toDp(), 0.toDp())
        assertChildMaxInView()
    }

    /** See b/241591211. */
    @Test
    fun doesNotCrashWhenCoordinatesDetachedDuringOperation() {
        val requests = mutableListOf<() -> Rect?>()
        val responder = object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = localRect

            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                requests += localRect
            }
        }
        val requester = BringIntoViewRequester()
        var coordinates: LayoutCoordinates? = null
        var attach by mutableStateOf(true)
        setContentAndInitialize {
            if (attach) {
                Box(
                    modifier = Modifier
                        .bringIntoViewResponder(responder)
                        .bringIntoViewRequester(requester)
                        .onPlaced { coordinates = it }
                        .size(10.toDp())
                )

                LaunchedEffect(Unit) {
                    // Wait a frame to allow the modifiers to be wired up and the coordinates to get
                    // attached.
                    withFrameMillis {}
                    requester.bringIntoView()
                }
            }
        }

        rule.runOnIdle {
            assertThat(requests).hasSize(1)
            assertThat(coordinates?.isAttached).isTrue()
        }

        attach = false

        rule.runOnIdle {
            assertThat(coordinates?.isAttached).isFalse()
            // This call should not crash.
            requests.single().invoke()
        }
    }

    @Test
    fun bringIntoView_concurrentOverlappingRequests_completeInGeometricOrder() {
        // Arrange.
        val childA = BringIntoViewRequester()
        val childB = BringIntoViewRequester()
        val childC = BringIntoViewRequester()
        val completedRequests = mutableListOf<BringIntoViewRequester>()
        setContentAndInitialize {
            Box(
                Modifier
                    .testTag(parentBox)
                    .size(100.toDp())
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(rememberScrollState())
                            Vertical -> Modifier.verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                // Nested boxes each with their own requester.
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.padding(start = 100.toDp())
                                Vertical -> Modifier.padding(top = 100.toDp())
                            }
                        )
                        .size(100.toDp())
                        .background(Green.copy(alpha = 0.25f))
                        .bringIntoViewRequester(childA)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .size(50.toDp())
                            .background(Green.copy(alpha = 0.25f))
                            .bringIntoViewRequester(childB)
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .size(25.toDp())
                                .background(Green.copy(alpha = 0.25f))
                                .bringIntoViewRequester(childC)
                        )
                    }
                }
            }
        }

        // Act.
        // Launch requests from biggest to smallest.
        listOf(childC, childB, childA).forEach {
            testScope.launch {
                it.bringIntoView()
                completedRequests += it
            }
        }
        rule.waitForIdle()

        // Assert.
        // The innermost request will be the first one to fully come into view, so it should
        // complete first, and the outermost one should complete last.
        assertThat(completedRequests).containsExactlyElementsIn(
            listOf(childC, childB, childA)
        ).inOrder()
    }

    @Test
    fun bringIntoView_concurrentOverlappingRequests_completeInGeometricOrder_whenReversed() {
        // Arrange.
        val childA = BringIntoViewRequester()
        val childB = BringIntoViewRequester()
        val childC = BringIntoViewRequester()
        val completedRequests = mutableListOf<BringIntoViewRequester>()
        setContentAndInitialize {
            Box(
                Modifier
                    .testTag(parentBox)
                    .size(100.toDp())
                    .background(LightGray)
                    .then(
                        when (orientation) {
                            Horizontal -> Modifier.horizontalScroll(rememberScrollState())
                            Vertical -> Modifier.verticalScroll(rememberScrollState())
                        }
                    )
            ) {
                // Nested boxes each with their own requester.
                Box(
                    Modifier
                        .then(
                            when (orientation) {
                                Horizontal -> Modifier.padding(start = 100.toDp())
                                Vertical -> Modifier.padding(top = 100.toDp())
                            }
                        )
                        .size(100.toDp())
                        .background(Green.copy(alpha = 0.25f))
                        .bringIntoViewRequester(childA)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .size(50.toDp())
                            .background(Green.copy(alpha = 0.25f))
                            .bringIntoViewRequester(childB)
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .size(25.toDp())
                                .background(Green.copy(alpha = 0.25f))
                                .bringIntoViewRequester(childC)
                        )
                    }
                }
            }
        }

        // Act.
        // Launch requests from biggest to smallest.
        listOf(childA, childB, childC).forEach {
            testScope.launch {
                it.bringIntoView()
                completedRequests += it
            }
        }
        rule.waitForIdle()

        // Assert.
        // The innermost request will be the first one to fully come into view, so it should
        // complete first, and the outermost one should complete last.
        assertThat(completedRequests).containsExactlyElementsIn(
            listOf(childC, childB, childA)
        ).inOrder()
    }

    @Test
    fun bringIntoViewScroller_childIsAtTopOfParent_shouldReturnCorrectValues() {
        val bringIntoViewRequester = BringIntoViewRequester()
        val bringIntoViewItemCoordinates = mutableStateOf<LayoutCoordinates?>(null)

        bringIntoViewScrollerTest_wrapper(
            requester = bringIntoViewRequester,
            childCoordinates = bringIntoViewItemCoordinates,
            expectedChildSize = 10.dp // child is visible
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .onPlaced { bringIntoViewItemCoordinates.value = it }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
            )
        }
    }

    @Test
    fun bringIntoViewScroller_childIsInTheMiddleOfParent_shouldReturnCorrectValues() {
        val bringIntoViewRequester = BringIntoViewRequester()
        val bringIntoViewItemCoordinates = mutableStateOf<LayoutCoordinates?>(null)

        bringIntoViewScrollerTest_wrapper(
            requester = bringIntoViewRequester,
            childCoordinates = bringIntoViewItemCoordinates,
            expectedChildSize = 10.dp // child is visible
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .onPlaced { bringIntoViewItemCoordinates.value = it }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
            )
        }
    }

    @Test
    fun bringIntoViewScroller_childIsPartOutOfBoundsOfParent_shouldReturnCorrectValues() {
        val bringIntoViewRequester = BringIntoViewRequester()
        val bringIntoViewItemCoordinates = mutableStateOf<LayoutCoordinates?>(null)

        bringIntoViewScrollerTest_wrapper(
            requester = bringIntoViewRequester,
            childCoordinates = bringIntoViewItemCoordinates,
            expectedChildSize = 10.dp // child is part visible
        ) {
            Box(
                modifier = Modifier
                    .size(195.dp)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .onPlaced { bringIntoViewItemCoordinates.value = it }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }
    }

    @Test
    fun bringIntoViewScroller_childIsOutOfBoundsOfParent_shouldReturnCorrectValues() {
        val bringIntoViewRequester = BringIntoViewRequester()
        val bringIntoViewItemCoordinates = mutableStateOf<LayoutCoordinates?>(null)

        bringIntoViewScrollerTest_wrapper(
            requester = bringIntoViewRequester,
            childCoordinates = bringIntoViewItemCoordinates,
            expectedChildSize = 10.dp // child is not visible
        ) {
            Box(
                modifier = Modifier
                    .size(205.dp)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .onPlaced { bringIntoViewItemCoordinates.value = it }
                    .bringIntoViewRequester(bringIntoViewRequester)
            )
        }
    }

    private fun bringIntoViewScrollerTest_wrapper(
        requester: BringIntoViewRequester,
        expectedChildSize: Dp,
        childCoordinates: State<LayoutCoordinates?>,
        content: @Composable () -> Unit
    ) {

        val containerSize = 200.dp

        fun calculateExpectedChildOffset(): Int {
            return if (orientation == Horizontal) {
                childCoordinates.value?.positionInParent()?.x
            } else {
                childCoordinates.value?.positionInParent()?.y
            }?.toInt() ?: 0
        }

        val expectedContainerSize = with(rule.density) { containerSize.roundToPx() }
        val customBringIntoViewSpec = object : BringIntoViewSpec {
            override val scrollAnimationSpec: AnimationSpec<Float> = spring()

            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                assertThat(containerSize).isEqualTo(expectedContainerSize)
                assertThat(size).isEqualTo(with(rule.density) { expectedChildSize.roundToPx() })
                assertThat(offset).isEqualTo(calculateExpectedChildOffset())
                return 0f
            }
        }

        rule.setContent {
            testScope = rememberCoroutineScope()
            val state = rememberScrollState()
            RowOrColumn(
                modifier = Modifier
                    .size(containerSize)
                    .scrollable(
                        state = state,
                        overscrollEffect = null,
                        orientation = orientation,
                        bringIntoViewSpec = customBringIntoViewSpec
                    )
                    .then(ScrollingLayoutElement(state, false, orientation == Vertical))
            ) {
                content()
            }
        }

        testScope.launch {
            requester.bringIntoView()
        }

        rule.waitForIdle()
    }

    @Test
    fun bringIntoViewScroller_shouldStopScrollingWhenReceivingZero() {
        val bringIntoViewRequests = listOf(300f, 150f, 0f)
        val scrollState = ScrollState(0)
        var requestsFulfilledScroll = 0
        val customBringIntoViewSpec = object : BringIntoViewSpec {
            var index = 0

            override val scrollAnimationSpec: AnimationSpec<Float> = spring()

            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                return bringIntoViewRequests[index].also {
                    index = (index + 1)
                    if (index > 2) {
                        requestsFulfilledScroll = scrollState.value
                        index = 2
                    }
                }
            }
        }

        val requester = BringIntoViewRequester()

        rule.setContent {
            testScope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scrollable(
                        state = scrollState,
                        overscrollEffect = null,
                        orientation = orientation,
                        bringIntoViewSpec = customBringIntoViewSpec
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .bringIntoViewRequester(requester)
                )
            }
        }

        testScope.launch {
            requester.bringIntoView()
        }

        rule.waitForIdle()

        assertThat(scrollState.value).isEqualTo(requestsFulfilledScroll)
    }

    // TODO(b/222093277) Once the test runtime supports layout calls between frames, write more
    //  tests for intermediate state changes, including request cancellation, non-overlapping
    //  request interruption, etc.

    private fun setContentAndInitialize(content: @Composable () -> Unit) {
        rule.setContent {
            testScope = rememberCoroutineScope()
            content()
        }
    }

    /**
     * Sizes and offsets of the composables in these tests must be specified using this function.
     * If they're specified using `xx.dp` syntax, a rounding error somewhere in the layout system
     * will cause the pixel values to be off-by-one.
     */
    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    /**
     * Returns the bounds of the node with [tag], without performing any clipping by any parents.
     */
    @Suppress("SameParameterValue")
    private fun getUnclippedBoundsInRoot(tag: String): Rect {
        val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
        return Rect(node.positionInRoot, node.size.toSize())
    }

    @Composable
    private fun RowOrColumn(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        when (orientation) {
            Horizontal -> Row(modifier) { content() }
            Vertical -> Column(modifier) { content() }
        }
    }

    private fun runBlockingAndAwaitIdle(block: suspend CoroutineScope.() -> Unit) {
        val job = testScope.launch(block = block)
        rule.waitForIdle()
        runBlocking {
            job.join()
        }
    }

    /**
     * Asserts that as much of the child (identified by [childBox]) as can fit in the viewport
     * (identified by [parentBox]) is visible. This is the min of the child size and the viewport
     * size.
     */
    private fun assertChildMaxInView() {
        val parentNode = rule.onNodeWithTag(parentBox).fetchSemanticsNode()
        val childNode = rule.onNodeWithTag(childBox).fetchSemanticsNode()

        // BoundsInRoot returns the clipped bounds.
        val visibleBounds: IntSize = childNode.boundsInRoot.size.run {
            IntSize(width.roundToInt(), height.roundToInt())
        }
        val expectedVisibleBounds = IntSize(
            width = minOf(parentNode.size.width, childNode.size.width),
            height = minOf(parentNode.size.height, childNode.size.height)
        )

        assertThat(visibleBounds).isEqualTo(expectedVisibleBounds)
    }
}
