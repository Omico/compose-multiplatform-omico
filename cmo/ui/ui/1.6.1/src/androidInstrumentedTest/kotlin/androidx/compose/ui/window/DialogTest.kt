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
package androidx.compose.ui.window

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DialogTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    private val defaultText = "dialogText"

    @Test
    fun dialogTest_isShowingContent() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    @Ignore("100% failing b/179359518")
    fun dialogTest_isNotDismissed_whenClicked() {
        val textBeforeClick = "textBeforeClick"
        val textAfterClick = "textAfterClick"

        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }
            val text = remember { mutableStateOf(textBeforeClick) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    }
                ) {
                    BasicText(
                        text = text.value,
                        modifier = Modifier.clickable {
                            text.value = textAfterClick
                        }
                    )
                }
            }
        }

        rule.onNodeWithText(textBeforeClick)
            .assertIsDisplayed()
            // Click inside the dialog
            .performClick()

        // Check that the Clickable was pressed and that the Dialog is still visible, but with
        // the new text
        rule.onNodeWithText(textBeforeClick).assertDoesNotExist()
        rule.onNodeWithText(textAfterClick).assertIsDisplayed()
    }

    @FlakyTest(bugId = 179359518)
    @Test
    fun dialogTest_isDismissed_whenSpecified() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    }
                ) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click outside the dialog to dismiss it
        val outsideX = 0
        val outsideY = with(rule.density) {
            rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() / 2
        }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        rule.onNodeWithText(defaultText).assertDoesNotExist()
    }

    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click outside the dialog to try to dismiss it
        val outsideX = 0
        val outsideY = with(rule.density) {
            rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() / 2
        }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // The Dialog should still be visible
        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenDismissOnClickOutsideIsFalse() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    },
                    properties = DialogProperties(dismissOnClickOutside = false)
                ) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click outside the dialog to try to dismiss it
        val outsideX = 0
        val outsideY = with(rule.density) {
            rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() / 2
        }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // The Dialog should still be visible
        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @FlakyTest(bugId = 159364185)
    fun dialogTest_isDismissed_whenSpecified_backButtonPressed() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    }
                ) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click the back button to dismiss the Dialog
        Espresso.pressBack()

        rule.onNodeWithText(defaultText).assertDoesNotExist()
    }

    @FlakyTest(bugId = 159364185)
    fun dialogTest_isNotDismissed_whenNotSpecified_backButtonPressed() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click the back button to try to dismiss the dialog
        Espresso.pressBack()

        // The Dialog should still be visible
        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @FlakyTest(bugId = 159364185)
    fun dialogTest_isNotDismissed_whenDismissOnBackPressIsFalse() {
        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(
                    onDismissRequest = {
                        showDialog.value = false
                    },
                    properties = DialogProperties(dismissOnBackPress = false)
                ) {
                    BasicText(defaultText)
                }
            }
        }

        rule.onNodeWithText(defaultText).assertIsDisplayed()

        // Click the back button to try to dismiss the dialog
        Espresso.pressBack()

        // The Dialog should still be visible
        rule.onNodeWithText(defaultText).assertIsDisplayed()
    }

    @Ignore // b/266613263
    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    fun dialogTest_backHandler_isCalled_backButtonPressed() {
        if (Build.VERSION.SDK_INT == 33 && Build.VERSION.CODENAME != "REL") {
            return // b/262909049: Do not run this test on pre-release Android U.
        }

        val clickCountPrefix = "Click: "

        rule.setContent {
            val showDialog = remember { mutableStateOf(true) }

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    val clickCount = remember { mutableStateOf(0) }
                    BasicText(clickCountPrefix + clickCount.value)
                    BackHandler {
                        clickCount.value++
                    }
                }
            }
        }

        rule.onNodeWithText(clickCountPrefix + "0").assertIsDisplayed()

        // Click the back button to trigger the BackHandler
        Espresso.pressBack()

        rule.onNodeWithText(clickCountPrefix + "1").assertIsDisplayed()
    }

    @Test
    fun dialog_preservesCompositionLocals() {
        val compositionLocal = compositionLocalOf<Float> { error("unset") }
        var value = 0f
        rule.setContent {
            CompositionLocalProvider(compositionLocal provides 1f) {
                Dialog(onDismissRequest = {}) {
                    value = compositionLocal.current
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1f, value)
        }
    }

    @Test
    fun canFillScreenWidth_dependingOnProperty() {
        var box1Width = 0
        var box2Width = 0
        rule.setContent {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize().onSizeChanged { box1Width = it.width })
            }
            Dialog(onDismissRequest = {}) {
                Box(Modifier.fillMaxSize().onSizeChanged { box2Width = it.width })
            }
        }
        rule.runOnIdle {
            Truth.assertThat(box1Width).isEqualTo(
                (rule.activity.resources.configuration.screenWidthDp * rule.density.density)
                    .roundToInt()
            )
            Truth.assertThat(box2Width).isLessThan(box1Width)
        }
    }

    @Test
    fun canChangeSize() {
        var width by mutableStateOf(10.dp)
        var usePlatformDefaultWidth by mutableStateOf(false)
        var actualWidth = 0

        rule.setContent {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = usePlatformDefaultWidth)
            ) {
                Box(Modifier.size(width, 150.dp).onSizeChanged { actualWidth = it.width })
            }
        }
        rule.runOnIdle {
            Truth.assertThat(actualWidth).isEqualTo((10 * rule.density.density).roundToInt())
        }
        width = 20.dp
        rule.runOnIdle {
            Truth.assertThat(actualWidth).isEqualTo((20 * rule.density.density).roundToInt())
        }

        usePlatformDefaultWidth = true

        width = 30.dp
        rule.runOnIdle {
            Truth.assertThat(actualWidth).isEqualTo((30 * rule.density.density).roundToInt())
        }
        width = 40.dp
        rule.runOnIdle {
            Truth.assertThat(actualWidth).isEqualTo((40 * rule.density.density).roundToInt())
        }
    }
}
