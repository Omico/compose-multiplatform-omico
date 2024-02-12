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
package androidx.compose.ui.awt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.density
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.MouseEvent.BUTTON1_DOWN_MASK
import java.awt.event.MouseEvent.MOUSE_ENTERED
import java.awt.event.MouseEvent.MOUSE_MOVED
import java.awt.event.MouseEvent.MOUSE_PRESSED
import java.awt.event.MouseEvent.MOUSE_RELEASED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.junit.Assume
import org.junit.Test

class ComposeWindowTest {
    // bug https://github.com/JetBrains/compose-jb/issues/1448
    @Test
    fun `dispose window inside event handler`() = runApplicationTest {
        var isClickHappened = false

        val window = ComposeWindow()
        window.isUndecorated = true
        window.size = Dimension(200, 200)
        window.setContent {
            Box(modifier = Modifier.fillMaxSize().background(Color.Blue).clickable {
                isClickHappened = true
                window.dispose()
            })
        }

        window.isVisible = true
        awaitIdle()

        window.sendMouseEvent(MOUSE_PRESSED, x = 100, y = 50)
        window.sendMouseEvent(MOUSE_RELEASED, x = 100, y = 50)
        awaitIdle()

        assertThat(isClickHappened).isTrue()
    }

    @Test
    fun `don't override user preferred size`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(Dispatchers.Swing) {
            val window = ComposeWindow()
            try {
                window.preferredSize = Dimension(234, 345)
                window.isUndecorated = true
                assertThat(window.preferredSize).isEqualTo(Dimension(234, 345))
                window.pack()
                assertThat(window.size).isEqualTo(Dimension(234, 345))
            } finally {
                window.dispose()
            }
        }
    }

    @Test
    fun `pack to Compose content`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        runBlocking(Dispatchers.Swing) {
            val window = ComposeWindow()
            try {
                window.setContent {
                    Box(Modifier.requiredSize(300.dp, 400.dp))
                }
                window.isUndecorated = true

                window.pack()
                assertThat(window.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(window.size).isEqualTo(Dimension(300, 400))

                window.isVisible = true
                assertThat(window.preferredSize).isEqualTo(Dimension(300, 400))
                assertThat(window.size).isEqualTo(Dimension(300, 400))
            } finally {
                window.dispose()
            }
        }
    }

    @Test
    fun `a single layout pass at the window start`() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)

        val layoutPassConstraints = mutableListOf<Constraints>()

        runBlocking(Dispatchers.Swing) {
            val window = ComposeWindow()
            try {
                window.size = Dimension(300, 400)
                window.setContent {
                    Box(Modifier.fillMaxSize().layout { _, constraints ->
                        layoutPassConstraints.add(constraints)
                        layout(0, 0) {}
                    })
                }

                window.isUndecorated = true
                window.isVisible = true
                window.paint(window.graphics)
                assertThat(layoutPassConstraints).isEqualTo(
                    listOf(
                        Constraints.fixed(
                            width = (300 * window.density.density).toInt(),
                            height = (400 * window.density.density).toInt(),
                        )
                    )
                )
            } finally {
                window.dispose()
            }
        }
    }

    @Test
    fun `dispose window in event handler`() = runApplicationTest {
        val window = ComposeWindow()
        try {
            window.size = Dimension(300, 400)
            window.setContent {
                Box(modifier = Modifier.fillMaxSize().background(Color.Blue).clickable {
                    window.dispose()
                })
            }
            window.isVisible = true
            window.sendMouseEvent(MOUSE_ENTERED, x = 100, y = 50)
            awaitIdle()
            window.sendMouseEvent(MOUSE_MOVED, x = 100, y = 50)
            awaitIdle()
            window.sendMouseEvent(MOUSE_PRESSED, x = 100, y = 50, modifiers = BUTTON1_DOWN_MASK)
            awaitIdle()
            window.sendMouseEvent(MOUSE_RELEASED, x = 100, y = 50)
            awaitIdle()
        } finally {
            window.dispose()
        }
    }
}
