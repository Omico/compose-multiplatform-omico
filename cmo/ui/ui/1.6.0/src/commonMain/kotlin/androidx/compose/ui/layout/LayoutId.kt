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

package androidx.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density

/**
 * Tag the element with [layoutId] to identify the element within its parent.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.LayoutTagChildrenUsage
 */
@Stable
fun Modifier.layoutId(layoutId: Any) = this then LayoutIdElement(layoutId = layoutId)

private data class LayoutIdElement(
    private val layoutId: Any
) : ModifierNodeElement<LayoutIdModifier>() {
    override fun create() = LayoutIdModifier(layoutId)

    override fun update(node: LayoutIdModifier) {
        node.layoutId = layoutId
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layoutId"
        value = layoutId
    }
}

/**
 * A [ParentDataModifierNode] which tags the target with the given [id][layoutId]. The provided tag
 * will act as parent data, and can be used for example by parent layouts to associate
 * composable children to [Measurable]s when doing layout, as shown below.
 */
internal class LayoutIdModifier(
    layoutId: Any,
) : ParentDataModifierNode, LayoutIdParentData, Modifier.Node() {

    override var layoutId: Any = layoutId
        internal set

    override fun Density.modifyParentData(parentData: Any?): Any? {
        return this@LayoutIdModifier
    }
}

/**
 * Can be implemented by values used as parent data to make them usable as tags.
 * If a parent data value implements this interface, it can then be returned when querying
 * [Measurable.layoutId] for the corresponding child.
 */
interface LayoutIdParentData {
    val layoutId: Any
}

/**
 * Retrieves the tag associated to a composable with the [Modifier.layoutId] modifier.
 * For a parent data value to be returned by this property when not using the [Modifier.layoutId]
 * modifier, the parent data value should implement the [LayoutIdParentData] interface.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.LayoutTagChildrenUsage
 */
val Measurable.layoutId: Any?
    get() = (parentData as? LayoutIdParentData)?.layoutId
