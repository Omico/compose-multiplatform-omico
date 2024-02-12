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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.TransformEvent.TransformDelta
import androidx.compose.foundation.gestures.TransformEvent.TransformStarted
import androidx.compose.foundation.gestures.TransformEvent.TransformStopped
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Enable transformation gestures of the modified UI element.
 *
 * Users should update their state themselves using default [TransformableState] and its
 * `onTransformation` callback or by implementing [TransformableState] interface manually and
 * reflect their own state in UI when using this component.
 *
 * @sample androidx.compose.foundation.samples.TransformableSample
 *
 * @param state [TransformableState] of the transformable. Defines how transformation events will be
 * interpreted by the user land logic, contains useful information about on-going events and
 * provides animation capabilities.
 * @param lockRotationOnZoomPan If `true`, rotation is allowed only if touch slop is detected for
 * rotation before pan or zoom motions. If not, pan and zoom gestures will be detected, but rotation
 * gestures will not be. If `false`, once touch slop is reached, all three gestures are detected.
 * @param enabled whether zooming by gestures is enabled or not
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.transformable(
    state: TransformableState,
    lockRotationOnZoomPan: Boolean = false,
    enabled: Boolean = true
) = transformable(state, { true }, lockRotationOnZoomPan, enabled)

/**
 * Enable transformation gestures of the modified UI element.
 *
 * Users should update their state themselves using default [TransformableState] and its
 * `onTransformation` callback or by implementing [TransformableState] interface manually and
 * reflect their own state in UI when using this component.
 *
 * This overload of transformable modifier provides [canPan] parameter, which allows the caller to
 * control when the pan can start. making pan gesture to not to start when the scale is 1f makes
 * transformable modifiers to work well within the scrollable container. See example:
 * @sample androidx.compose.foundation.samples.TransformableSampleInsideScroll
 *
 * @param state [TransformableState] of the transformable. Defines how transformation events will be
 * interpreted by the user land logic, contains useful information about on-going events and
 * provides animation capabilities.
 * @param canPan whether the pan gesture can be performed or not given the pan offset
 * @param lockRotationOnZoomPan If `true`, rotation is allowed only if touch slop is detected for
 * rotation before pan or zoom motions. If not, pan and zoom gestures will be detected, but rotation
 * gestures will not be. If `false`, once touch slop is reached, all three gestures are detected.
 * @param enabled whether zooming by gestures is enabled or not
 */
@ExperimentalFoundationApi
fun Modifier.transformable(
    state: TransformableState,
    canPan: (Offset) -> Boolean,
    lockRotationOnZoomPan: Boolean = false,
    enabled: Boolean = true
) = this then TransformableElement(state, canPan, lockRotationOnZoomPan, enabled)

private class TransformableElement(
    private val state: TransformableState,
    private val canPan: (Offset) -> Boolean,
    private val lockRotationOnZoomPan: Boolean,
    private val enabled: Boolean
) : ModifierNodeElement<TransformableNode>() {
    override fun create(): TransformableNode = TransformableNode(
        state, canPan, lockRotationOnZoomPan, enabled
    )

    override fun update(node: TransformableNode) {
        node.update(state, canPan, lockRotationOnZoomPan, enabled)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as TransformableElement

        if (state != other.state) return false
        if (canPan != other.canPan) return false
        if (lockRotationOnZoomPan != other.lockRotationOnZoomPan) return false
        if (enabled != other.enabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + canPan.hashCode()
        result = 31 * result + lockRotationOnZoomPan.hashCode()
        result = 31 * result + enabled.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "transformable"
        properties["state"] = state
        properties["canPan"] = canPan
        properties["enabled"] = enabled
        properties["lockRotationOnZoomPan"] = lockRotationOnZoomPan
    }
}

private class TransformableNode(
    private var state: TransformableState,
    private var canPan: (Offset) -> Boolean,
    private var lockRotationOnZoomPan: Boolean,
    private var enabled: Boolean
) : DelegatingNode() {

    private val updatedCanPan: (Offset) -> Boolean = { canPan.invoke(it) }
    private val channel = Channel<TransformEvent>(capacity = Channel.UNLIMITED)

    private val pointerInputNode = delegate(SuspendingPointerInputModifierNode {
        if (!enabled) return@SuspendingPointerInputModifierNode
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) {
                while (isActive) {
                    var event = channel.receive()
                    if (event !is TransformStarted) continue
                    try {
                        state.transform(MutatePriority.UserInput) {
                            while (event !is TransformStopped) {
                                (event as? TransformDelta)?.let {
                                    transformBy(it.zoomChange, it.panChange, it.rotationChange)
                                }
                                event = channel.receive()
                            }
                        }
                    } catch (_: CancellationException) {
                        // ignore the cancellation and start over again.
                    }
                }
            }
            awaitEachGesture {
                try {
                    detectZoom(lockRotationOnZoomPan, channel, updatedCanPan)
                } catch (exception: CancellationException) {
                    if (!isActive) throw exception
                } finally {
                    channel.trySend(TransformStopped)
                }
            }
        }
    })

    fun update(
        state: TransformableState,
        canPan: (Offset) -> Boolean,
        lockRotationOnZoomPan: Boolean,
        enabled: Boolean
    ) {
        this.canPan = canPan
        val needsReset = this.state != state ||
            this.enabled != enabled ||
            this.lockRotationOnZoomPan != lockRotationOnZoomPan
        if (needsReset) {
            this.state = state
            this.enabled = enabled
            this.lockRotationOnZoomPan = lockRotationOnZoomPan
            pointerInputNode.resetPointerInputHandler()
        }
    }
}

private suspend fun AwaitPointerEventScope.detectZoom(
    panZoomLock: Boolean,
    channel: Channel<TransformEvent>,
    canPan: (Offset) -> Boolean
) {
    var rotation = 0f
    var zoom = 1f
    var pan = Offset.Zero
    var pastTouchSlop = false
    val touchSlop = viewConfiguration.touchSlop
    var lockedToPanZoom = false
    awaitFirstDown(requireUnconsumed = false)
    do {
        val event = awaitPointerEvent()
        val canceled = event.changes.fastAny { it.isConsumed }
        if (!canceled) {
            val zoomChange = event.calculateZoom()
            val rotationChange = event.calculateRotation()
            val panChange = event.calculatePan()

            if (!pastTouchSlop) {
                zoom *= zoomChange
                rotation += rotationChange
                pan += panChange

                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1 - zoom) * centroidSize
                val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                val panMotion = pan.getDistance()

                if (zoomMotion > touchSlop ||
                    rotationMotion > touchSlop ||
                    (panMotion > touchSlop && canPan.invoke(panChange))
                ) {
                    pastTouchSlop = true
                    lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    channel.trySend(TransformStarted)
                }
            }

            if (pastTouchSlop) {
                val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                if (effectiveRotation != 0f ||
                    zoomChange != 1f ||
                    (panChange != Offset.Zero && canPan.invoke(panChange))
                ) {
                    channel.trySend(TransformDelta(zoomChange, panChange, effectiveRotation))
                }
                event.changes.fastForEach {
                    if (it.positionChanged()) {
                        it.consume()
                    }
                }
            }
        } else {
            channel.trySend(TransformStopped)
        }
        val finalEvent = awaitPointerEvent(pass = PointerEventPass.Final)
        // someone consumed while we were waiting for touch slop
        val finallyCanceled = finalEvent.changes.fastAny { it.isConsumed } && !pastTouchSlop
    } while (!canceled && !finallyCanceled && event.changes.fastAny { it.pressed })
}

private sealed class TransformEvent {
    object TransformStarted : TransformEvent()
    object TransformStopped : TransformEvent()
    class TransformDelta(
        val zoomChange: Float,
        val panChange: Offset,
        val rotationChange: Float
    ) : TransformEvent()
}
