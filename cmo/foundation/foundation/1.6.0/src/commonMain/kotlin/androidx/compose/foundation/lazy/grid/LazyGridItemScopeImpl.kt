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

package androidx.compose.foundation.lazy.grid

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimationSpecsNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntOffset

@OptIn(ExperimentalFoundationApi::class)
internal object LazyGridItemScopeImpl : LazyGridItemScope {
    @ExperimentalFoundationApi
    override fun Modifier.animateItemPlacement(animationSpec: FiniteAnimationSpec<IntOffset>) =
        this then AnimateItemElement(animationSpec)
}

private data class AnimateItemElement(
    val placementSpec: FiniteAnimationSpec<IntOffset>
) : ModifierNodeElement<LazyLayoutAnimationSpecsNode>() {

    override fun create(): LazyLayoutAnimationSpecsNode =
        LazyLayoutAnimationSpecsNode(null, placementSpec)

    override fun update(node: LazyLayoutAnimationSpecsNode) {
        node.placementSpec = placementSpec
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "animateItemPlacement"
        value = placementSpec
    }
}
