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

package androidx.compose.ui.platform

import android.graphics.Rect
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.test.setViewLayerTypeForApi28
import androidx.core.view.doOnLayout
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.test.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidUiDispatcherTest {
    @get:Rule
    val rule = activityScenarioRule<AppCompatActivity>()

    @Before
    fun setup() {
        setViewLayerTypeForApi28()
    }

    @Test
    fun currentThreadIsMainOnMainThread() = runBlocking(Dispatchers.Main) {
        assertSame(AndroidUiDispatcher.Main, AndroidUiDispatcher.CurrentThread)
    }

    @Test
    fun runsBeforeFrameCallback() = runBlocking(Dispatchers.Main) {
        rule.scenario.onActivity {
            // Force creation of decor view to ensure we have a frame scheduled
            it.window.decorView
        }

        var ranOnUiDispatcher = false
        launch(AndroidUiDispatcher.Main) { ranOnUiDispatcher = true }

        val choreographerResult = CompletableDeferred<Boolean>()
        Choreographer.getInstance().postFrameCallback {
            choreographerResult.complete(ranOnUiDispatcher)
        }

        assertTrue("UI dispatcher ran before choreographer frame", choreographerResult.await())
    }

    /**
     * Verify that [AndroidUiDispatcher] will run a resumed continuation before
     * the next frame is drawn, even if that continuation is resumed during the dispatch of
     * batched input. Batched input is dispatched during an atomic sequence of events handled
     * by the [Choreographer] of input => animation callbacks => measure => layout => draw,
     * which will cause dispatchers that schedule entirely based on [android.os.Handler] messages
     * to miss the current frame.
     *
     * This test also verifies that a call to [AndroidUiDispatcher.frameClock]'s
     * [MonotonicFrameClock.withFrameNanos] will resume in time to make the current frame if called
     * from the situation described above, and that subsequent calls will wait until the next frame.
     */
    @FlakyTest(bugId = 255972660)
    @Test
    fun runsBeforeFrameDispatchedByInput() = runBlocking {
        val ranInputJobOnFrame = CompletableDeferred<Int>()
        val viewTouchedOnFrame = CompletableDeferred<Int>()
        val withFrameOnFrame = CompletableDeferred<Int>()
        val withFrameSecondCall = CompletableDeferred<Int>()
        val layoutRect = CompletableDeferred<Rect>()
        var preDrawCount = 0
        rule.scenario.onActivity { activity ->
            activity.setContentView(
                View(activity).apply {
                    setOnTouchListener { _, motionEvent ->
                        if (motionEvent.action != MotionEvent.ACTION_UP) {
                            return@setOnTouchListener true
                        }
                        viewTouchedOnFrame.complete(preDrawCount)
                        // Use the frame clock provided by AndroidUiDispatcher.Main
                        launch(AndroidUiDispatcher.Main) {
                            ranInputJobOnFrame.complete(preDrawCount)
                            withFrameNanos {
                                withFrameOnFrame.complete(preDrawCount)
                            }
                            withFrameNanos {
                                withFrameSecondCall.complete(preDrawCount)
                            }
                        }
                        invalidate()
                        true
                    }
                    viewTreeObserver.addOnPreDrawListener {
                        preDrawCount++
                        true
                    }
                    doOnLayout { view ->
                        val rect = Rect()
                        view.getGlobalVisibleRect(rect)
                        layoutRect.complete(rect)
                    }
                },
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
        }

        with(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())) {
            // Use a swipe event with many steps to force the input dispatcher into batched mode.
            // A simple click here will not force batched mode of input dispatch to the view
            // hierarchy and the test will pass using a solely Handler-based dispatcher.
            // Batched mode will wait to dispatch events until the frame begins, causing
            // a Handler message to miss the frame, but AndroidUiDispatchers.Main should resume
            // in the same frame if the resume was triggered by the input event.
            val rect = layoutRect.await()
            swipe(rect.left + 1, rect.top + 1, rect.right - 1, rect.bottom - 1, 30)
            waitForIdle()
        }

        assertNotNull(
            "Timeout exceeded waiting for response to input events",
            withTimeoutOrNull(5_000) {
                val viewTouched = viewTouchedOnFrame.await()
                val inputJob = ranInputJobOnFrame.await()
                assertNotEquals(0, viewTouched)
                assertNotEquals(0, inputJob)
                assertEquals(
                    "touch and launched job resume happened on same frame",
                    viewTouched,
                    inputJob
                )
                assertEquals(
                    "withFrame ran on the same frame where it was called",
                    inputJob,
                    withFrameOnFrame.await()
                )
                assertEquals(
                    "second withFrame call was invoked on the very next frame",
                    inputJob + 1,
                    withFrameSecondCall.await()
                )
            }
        )
    }

    /**
     * Test that an AndroidUiDispatcher can be wrapped by another ContinuationInterceptor
     * without breaking the MonotonicFrameClock's ability to coordinate with its
     * original dispatcher.
     *
     * Construct a situation where the Choreographer contains three frame callbacks:
     * 1) checkpoint 1
     * 2) the AndroidUiDispatcher awaiting-frame callback
     * 3) checkpoint 2
     * Confirm that a call to withFrameNanos made *after* these three frame callbacks
     * are enqueued runs *before* checkpoint 2, indicating that it ran with the original
     * dispatcher's awaiting-frame callback, even though we wrapped the dispatcher.
     */
    @Test
    fun wrappedDispatcherPostsToDispatcherFrameClock() = runBlocking(Dispatchers.Main) {
        val uiDispatcherContext = AndroidUiDispatcher.Main
        val uiDispatcher = uiDispatcherContext[ContinuationInterceptor] as CoroutineDispatcher
        val wrapperDispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                uiDispatcher.dispatch(context, block)
            }
        }

        val choreographer = Choreographer.getInstance()

        val expectCount = AtomicInteger(1)
        fun expect(value: Int) {
            while (true) {
                val old = expectCount.get()
                if (old != value) fail("expected sequence $old but encountered $value")
                if (expectCount.compareAndSet(value, value + 1)) break
            }
        }

        choreographer.postFrameCallback {
            expect(1)
        }

        launch(uiDispatcherContext, start = CoroutineStart.UNDISPATCHED) {
            withFrameNanos {
                expect(2)
            }
        }

        choreographer.postFrameCallback {
            expect(4)
        }

        withContext(uiDispatcherContext + wrapperDispatcher) {
            withFrameNanos {
                expect(3)
            }
            expect(5)
        }
    }
}
