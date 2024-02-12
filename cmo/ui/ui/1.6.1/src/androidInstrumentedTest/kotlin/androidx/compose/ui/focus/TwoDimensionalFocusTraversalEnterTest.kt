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

package androidx.compose.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection.Companion.Enter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TwoDimensionalFocusTraversalEnterTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private val initialFocus: FocusRequester = FocusRequester()
    private val focusedItem = mutableStateOf(false)

    /**
     *      ________________   ____________
     *     |  focusedItem  |  | otherItem |
     *     |_______________|  |___________|
     */
    @Test
    fun moveFocusEnter_noChildren_doesNotMoveFocus() {
        // Arrange.
        val otherItem = mutableStateOf(false)
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 10, 10, initialFocus)
            FocusableBox(otherItem, 10, 0, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(focusedItem.value).isTrue()
            assertThat(otherItem.value).isFalse()
        }
    }

    /**
     *      ___________________   ____________
     *     |    focusedItem   |  |           |
     *     |  ______________  |  |           |
     *     | | deactivated |  |  | otherItem |
     *     | |_____________|  |  |           |
     *     |__________________|  |___________|
     */
    @Test
    fun moveFocusEnter_deactivatedChild_doesNotMoveFocus() {
        // Arrange.
        val (focusedItem, deactivatedItem, otherItem) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 10, 10, initialFocus) {
                FocusableBox(deactivatedItem, 10, 0, 10, 10, deactivated = true)
            }
            FocusableBox(otherItem, 10, 0, 10, 10)
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(focusedItem.value).isTrue()
            assertThat(deactivatedItem.value).isFalse()
            assertThat(otherItem.value).isFalse()
        }
    }

    /**
     *      _______________
     *     |  focusedItem |
     *     |   _________  |
     *     |  | child  |  |
     *     |  |________|  |
     *     |______________|
     */
    @Test
    fun moveFocusEnter_focusesOnChild() {
        // Arrange.
        val child = mutableStateOf(false)
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus) {
                FocusableBox(child, 10, 10, 10, 10)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(child.value).isTrue()
        }
    }

    /**
     *      _________________________
     *     |  focusedItem           |
     *     |   ___________________  |
     *     |  |  child           |  |
     *     |  |   _____________  |  |
     *     |  |  | grandchild |  |  |
     *     |  |  |____________|  |  |
     *     |  |__________________|  |
     *     |________________________|
     */
    @Test
    fun moveFocusEnter_focusesOnImmediateChild() {
        // Arrange.
        val (child, grandchild) = List(2) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus) {
                FocusableBox(child, 10, 10, 10, 10) {
                    FocusableBox(grandchild, 10, 10, 10, 10)
                }
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(child.value).isTrue()
            assertThat(grandchild.value).isFalse()
        }
    }

    /**
     *      _________________________
     *     |  focusedItem           |
     *     |   ___________________  |
     *     |  |  child           |  |
     *     |  |   _____________  |  |
     *     |  |  | grandchild |  |  |
     *     |  |  |____________|  |  |
     *     |  |__________________|  |
     *     |________________________|
     */
    @Test
    fun moveFocusEnter_skipsImmediateDeactivatedChild() {
        // Arrange.
        val (child, grandchild) = List(2) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus) {
                FocusableBox(child, 10, 10, 10, 10, deactivated = true) {
                    FocusableBox(grandchild, 10, 10, 10, 10)
                }
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(child.value).isFalse()
            assertThat(grandchild.value).isTrue()
        }
    }

    /**
     *      _________________________
     *     |  focusedItem           |
     *     |   ___________________  |
     *     |  |  child           |  |
     *     |  |   _____________  |  |
     *     |  |  | grandchild |  |  |
     *     |  |  |____________|  |  |
     *     |  |__________________|  |
     *     |________________________|
     */
    @Test
    fun moveFocusEnter_deactivatedChild_withCustomEnter_canCancelFocus() {
        // Arrange.
        val (child, grandchild) = List(2) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus) {
                val customEnter = Modifier.focusProperties { enter = { FocusRequester.Cancel } }
                FocusableBox(child, 10, 10, 10, 10, deactivated = true, modifier = customEnter) {
                    FocusableBox(grandchild, 10, 10, 10, 10)
                }
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isFalse()
            assertThat(focusedItem.value).isTrue()
            assertThat(child.value).isFalse()
            assertThat(grandchild.value).isFalse()
        }
    }

    /**
     *      __________________________________________
     *     |  focusedItem                            |
     *     |   ____________________________________  |
     *     |  |  child                            |  |
     *     |  |   ______________   ______________ |  |
     *     |  |  | grandchild1 |  | grandchild2 | |  |
     *     |  |  |_____________|  |_____________| |  |
     *     |  |___________________________________|  |
     *     |_________________________________________|
     */
    @Test
    fun moveFocusEnter_deactivatedChild_withCustomEnter_canRedirectFocusEnter() {
        // Arrange.
        val (child, grandchild1, grandchild2) = List(3) { mutableStateOf(false) }
        val grandchild2Requester = FocusRequester()
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 30, 30, initialFocus) {
                val customEnter = Modifier.focusProperties { enter = { grandchild2Requester } }
                FocusableBox(child, 10, 10, 10, 10, deactivated = true, modifier = customEnter) {
                    FocusableBox(grandchild1, 10, 10, 10, 10)
                    FocusableBox(grandchild2, 10, 10, 10, 10, grandchild2Requester)
                }
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(child.value).isFalse()
            assertThat(grandchild1.value).isFalse()
            assertThat(grandchild2.value).isTrue()
        }
    }

    // TODO(b/176847718): After RTL support is added, add a similar test where the topRight child
    //  is focused.
    /**
     *      _______________________________________
     *     |  focusedItem                         |
     *     |   _________   _________   _________  |
     *     |  | child1 |  | child2 |  | child3 |  |
     *     |  |________|  |________|  |________|  |
     *     |   _________   _________   _________  |
     *     |  | child4 |  | child5 |  | child6 |  |
     *     |  |________|  |________|  |________|  |
     *     |______________________________________|
     */
    @Test
    fun moveFocusEnter_topLeftChildIsFocused() {
        // Arrange.
        val children = List(6) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 70, 50, initialFocus) {
                FocusableBox(children[0], 10, 10, 10, 10)
                FocusableBox(children[1], 30, 10, 10, 10)
                FocusableBox(children[2], 50, 10, 10, 10)
                FocusableBox(children[3], 10, 30, 10, 10)
                FocusableBox(children[4], 30, 30, 10, 10)
                FocusableBox(children[5], 50, 30, 10, 10)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(children.values).isExactly(
                true, false, false,
                false, false, false
            )
        }
    }

    /**
     *      _______________________________________
     *     |  focusedItem                         |
     *     |   _________   _________   _________  |
     *     |  | child1 |  | child2 |  | child3 |  |
     *     |  |________|  |________|  |________|  |
     *     |   _________   _________   _________  |
     *     |  | child4 |  | child5 |  | child6 |  |
     *     |  |________|  |________|  |________|  |
     *     |______________________________________|
     */
    @Test
    fun moveFocusEnter_deactivatedTopLeftChildIsSkipped() {
        // Arrange.
        val children = List(6) { mutableStateOf(false) }
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 70, 50, initialFocus) {
                FocusableBox(children[0], 10, 10, 10, 10, deactivated = true)
                FocusableBox(children[1], 30, 10, 10, 10)
                FocusableBox(children[2], 50, 10, 10, 10)
                FocusableBox(children[3], 10, 30, 10, 10)
                FocusableBox(children[4], 30, 30, 10, 10)
                FocusableBox(children[5], 50, 30, 10, 10)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(children.values).isExactly(
                false, false, false,
                true, false, false
            )
        }
    }

    /**
     *      _______________________________________
     *     |  focusedItem                         |
     *     |   _________   _________   _________  |
     *     |  | child1 |  | child2 |  | child3 |  |
     *     |  |________|  |________|  |________|  |
     *     |   _________   _________   _________  |
     *     |  | child4 |  | child5 |  | child6 |  |
     *     |  |________|  |________|  |________|  |
     *     |______________________________________|
     */
    @Test
    fun moveFocusEnter_customChildIsFocused() {
        // Arrange.
        val children = List(6) { mutableStateOf(false) }
        val child3 = FocusRequester()
        val customFocusEnter = Modifier.focusProperties { enter = { child3 } }
        rule.setContentForTest {
            FocusableBox(focusedItem, 0, 0, 70, 50, initialFocus, modifier = customFocusEnter) {
                FocusableBox(children[0], 10, 10, 10, 10)
                FocusableBox(children[1], 30, 10, 10, 10)
                FocusableBox(children[2], 50, 10, 10, 10)
                FocusableBox(children[3], 10, 30, 10, 10, child3)
                FocusableBox(children[4], 30, 30, 10, 10)
                FocusableBox(children[5], 50, 30, 10, 10)
            }
        }

        // Act.
        val movedFocusSuccessfully = rule.runOnIdle { focusManager.moveFocus(Enter) }

        // Assert.
        rule.runOnIdle {
            assertThat(movedFocusSuccessfully).isTrue()
            assertThat(focusedItem.value).isFalse()
            assertThat(children.values).isExactly(
                false, false, false,
                true, false, false
            )
        }
    }

    private fun ComposeContentTestRule.setContentForTest(composable: @Composable () -> Unit) {
        setContent {
            focusManager = LocalFocusManager.current
            composable()
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }
}

private val List<MutableState<Boolean>>.values get() = this.map { it.value }
