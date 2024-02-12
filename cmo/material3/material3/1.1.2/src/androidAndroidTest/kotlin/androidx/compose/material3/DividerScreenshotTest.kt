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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class DividerScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val Tag = "Divider"

    @Test
    fun lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.testTag(Tag)) {
                Spacer(Modifier.size(10.dp))
                Divider()
                Spacer(Modifier.size(10.dp))
            }
        }
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "divider_lightTheme")
    }

    @Test
    fun darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Column(Modifier.testTag(Tag)) {
                Spacer(Modifier.size(10.dp))
                Divider()
                Spacer(Modifier.size(10.dp))
            }
        }
        composeTestRule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "divider_darkTheme")
    }
}
