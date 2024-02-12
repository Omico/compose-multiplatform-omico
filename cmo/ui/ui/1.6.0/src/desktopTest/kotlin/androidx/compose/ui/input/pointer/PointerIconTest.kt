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

package androidx.compose.ui.input.pointer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.awt.Cursor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PointerIconTest {
    private val window = TestComposeWindow(width = 100, height = 100, density = Density(1f))

    private val iconService = object : PointerIconService {
        private var currentIcon: PointerIcon = PointerIcon.Default
        override fun getIcon(): PointerIcon {
            return currentIcon
        }

        override fun setIcon(value: PointerIcon?) {
            currentIcon = value ?: PointerIcon.Default
        }
    }

    @Test
    fun basicTest() {
        window.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        window.onMouseMoved(
            x = 5,
            y = 5
        )
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Text)
    }

    @Test
    fun commitsToComponent() {
        window.setContent {
            Box(
                modifier = Modifier
                    .size(30.dp, 30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Text)
                        .size(10.dp, 10.dp)
                )
            }
        }

        window.onMouseMoved(
            x = 5,
            y = 5
        )
        assertThat(window.currentCursor.type).isEqualTo(Cursor.TEXT_CURSOR)
    }

    @Test
    fun preservedIfSameEventDispatchedTwice() {
        window.setContent {
            Box(
                modifier = Modifier
                    .size(30.dp, 30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Text)
                        .size(10.dp, 10.dp)
                )
            }
        }

        window.onMouseMoved(
            x = 5,
            y = 5
        )
        window.onMouseMoved(
            x = 5,
            y = 5
        )
        assertThat(window.currentCursor.type).isEqualTo(Cursor.TEXT_CURSOR)
    }

    @Test
    fun parentWins() {
        window.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand, true)
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        window.onMouseMoved(
            x = 5,
            y = 5
        )
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Hand)

        window.onMouseMoved(
            x = 15,
            y = 15
        )
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Hand)
    }

    @Test
    fun childWins() {
        window.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .size(30.dp, 30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Text)
                            .size(10.dp, 10.dp)
                    )
                }
            }
        }

        window.onMouseMoved(
            x = 5,
            y = 5
        )
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Text)

        window.onMouseMoved(
            x = 15,
            y = 15
        )
        assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Hand)
    }
}
