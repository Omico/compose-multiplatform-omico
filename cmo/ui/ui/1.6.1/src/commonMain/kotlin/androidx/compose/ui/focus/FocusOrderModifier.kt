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

package androidx.compose.ui.focus

import androidx.compose.ui.Modifier
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * A [modifier][Modifier.Element] that can be used to set a custom focus traversal order.
 *
 * @see Modifier.focusOrder
 */
@Deprecated("Use Modifier.focusProperties() instead")
@JvmDefaultWithCompatibility
interface FocusOrderModifier : Modifier.Element {

    /**
     * Populates the [next][FocusOrder.next] / [left][FocusOrder.left] /
     * [right][FocusOrder.right] / [up][FocusOrder.up] / [down][FocusOrder.down] items if
     * you don't want to use the default focus traversal order.
     */
    @Suppress("DEPRECATION")
    fun populateFocusOrder(focusOrder: FocusOrder)
}

/**
 * Specifies custom focus destinations that are used instead of the default focus traversal order.
 *
 * @sample androidx.compose.ui.samples.CustomFocusOrderSample
 */
@Deprecated("Use FocusProperties instead")
class FocusOrder internal constructor(private val focusProperties: FocusProperties) {
    @Suppress("unused")
    constructor() : this(FocusPropertiesImpl())

    /**
     * A custom item to be used when the user requests a focus moves to the "next" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var next: FocusRequester
        get() = focusProperties.next
        set(next) {
            focusProperties.next = next
        }

    /**
     * A custom item to be used when the user requests a focus moves to the "previous" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var previous: FocusRequester
        get() = focusProperties.previous
        set(previous) {
            focusProperties.previous = previous
        }

    /**
     *  A custom item to be used when the user moves focus "up".
     *
     *  @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var up: FocusRequester
        get() = focusProperties.up
        set(up) {
            focusProperties.up = up
        }

    /**
     *  A custom item to be used when the user moves focus "down".
     *
     *  @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var down: FocusRequester
        get() = focusProperties.down
        set(down) {
            focusProperties.down = down
        }

    /**
     * A custom item to be used when the user requests a focus moves to the "left" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var left: FocusRequester
        get() = focusProperties.left
        set(left) {
            focusProperties.left = left
        }

    /**
     * A custom item to be used when the user requests a focus moves to the "right" item.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var right: FocusRequester
        get() = focusProperties.right
        set(right) {
            focusProperties.right = right
        }

    /**
     * A custom item to be used when the user requests a focus moves to the "left" in LTR mode and
     * "right" in RTL mode.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var start: FocusRequester
        get() = focusProperties.start
        set(start) {
            focusProperties.start = start
        }

    /**
     * A custom item to be used when the user requests a focus moves to the "right" in LTR mode
     * and "left" in RTL mode.
     *
     * @sample androidx.compose.ui.samples.CustomFocusOrderSample
     */
    var end: FocusRequester
        get() = focusProperties.end
        set(end) {
            focusProperties.end = end
        }
}

/**
 * Use this modifier to specify a custom focus traversal order.
 *
 * @param focusOrderReceiver Specifies [FocusRequester]s that are used when the user wants
 * to move the current focus to the [next][FocusOrder.next] item, or wants to move
 * focus [left][FocusOrder.left], [right][FocusOrder.right], [up][FocusOrder.up] or
 * [down][FocusOrder.down].
 *
 * @sample androidx.compose.ui.samples.CustomFocusOrderSample
 */
@Deprecated(
    "Use focusProperties() instead",
    ReplaceWith(
        "this.focusProperties(focusOrderReceiver)",
        "androidx.compose.ui.focus.focusProperties"
    )
)
fun Modifier.focusOrder(
    @Suppress("DEPRECATION")
    focusOrderReceiver: FocusOrder.() -> Unit
): Modifier {
    val scope = FocusOrderToProperties(focusOrderReceiver)
    return focusProperties { scope.apply(this) }
}

/**
 * A modifier that lets you specify a [FocusRequester] for the current composable so that this
 * [focusRequester] can be used by another composable to specify a custom focus order.
 *
 * @sample androidx.compose.ui.samples.CustomFocusOrderSample
 */
@Deprecated(
    "Use focusRequester() instead",
    ReplaceWith("this.focusRequester(focusRequester)", "androidx.compose.ui.focus.focusRequester")
)
fun Modifier.focusOrder(focusRequester: FocusRequester): Modifier = focusRequester(focusRequester)

/**
 * A modifier that lets you specify a [FocusRequester] for the current composable along with
 * [focusOrder].
 */
@Deprecated(
    "Use focusProperties() and focusRequester() instead",
    ReplaceWith(
        "this.focusRequester(focusRequester).focusProperties(focusOrderReceiver)",
        "androidx.compose.ui.focus.focusProperties, androidx.compose.ui.focus.focusRequester"
    )
)
fun Modifier.focusOrder(
    focusRequester: FocusRequester,
    @Suppress("DEPRECATION")
    focusOrderReceiver: FocusOrder.() -> Unit
): Modifier {
    val scope = FocusOrderToProperties(focusOrderReceiver)
    return this
        .focusRequester(focusRequester)
        .focusProperties { scope.apply(this) }
}

@Suppress("DEPRECATION")
internal class FocusOrderToProperties(
    val focusOrderReceiver: FocusOrder.() -> Unit
) : FocusPropertiesScope {
    override fun apply(focusProperties: FocusProperties) {
        focusOrderReceiver(FocusOrder(focusProperties))
    }
}

// Note: Implementing function interface is prohibited in K/JS (class A: () -> Unit)
// therefore we workaround this limitation by inheriting a fun interface instead
internal fun interface FocusPropertiesScope {
    fun apply(focusProperties: FocusProperties)
}
