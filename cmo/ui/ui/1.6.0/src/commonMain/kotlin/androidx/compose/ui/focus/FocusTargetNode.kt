/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.dispatchForKind
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.visitAncestors
import androidx.compose.ui.node.visitSelfAndAncestors
import androidx.compose.ui.platform.InspectorInfo

internal class FocusTargetNode :
    CompositionLocalConsumerModifierNode,
    FocusTargetModifierNode,
    ObserverModifierNode,
    ModifierLocalModifierNode,
    Modifier.Node() {

    private var isProcessingCustomExit = false
    private var isProcessingCustomEnter = false

    // During a transaction, changes to the state are stored as uncommitted focus state. At the
    // end of the transaction, this state is stored as committed focus state.
    private var committedFocusState: FocusStateImpl = Inactive

    @OptIn(ExperimentalComposeUiApi::class)
    override var focusState: FocusStateImpl
        get() = focusTransactionManager?.run { uncommittedFocusState } ?: committedFocusState
        set(value) {
            with(requireTransactionManager()) {
                uncommittedFocusState = value
            }
        }

    var previouslyFocusedChildHash: Int = 0

    val beyondBoundsLayoutParent: BeyondBoundsLayout?
        get() = ModifierLocalBeyondBoundsLayout.current

    override fun onObservedReadsChanged() {
        val previousFocusState = focusState
        invalidateFocus()
        if (previousFocusState != focusState) refreshFocusEventNodes()
    }

    /**
     * Clears focus if this focus target has it.
     */
    override fun onReset() {
        when (focusState) {
            // Clear focus from the current FocusTarget.
            // This currently clears focus from the entire hierarchy, but we can change the
            // implementation so that focus is sent to the immediate focus parent.
            Active, Captured -> requireOwner().focusOwner.clearFocus(force = true)
            ActiveParent -> {
                scheduleInvalidationForFocusEvents()
                // This node might be reused, so reset the state to Inactive.
                requireTransactionManager().withNewTransaction { focusState = Inactive }
            }
            Inactive -> scheduleInvalidationForFocusEvents()
        }
    }

    /**
     * Visits parent [FocusPropertiesModifierNode]s and runs
     * [FocusPropertiesModifierNode.applyFocusProperties] on each parent.
     * This effectively collects an aggregated focus state.
     */
    internal fun fetchFocusProperties(): FocusProperties {
        val properties = FocusPropertiesImpl()
        visitSelfAndAncestors(Nodes.FocusProperties, untilType = Nodes.FocusTarget) {
            it.applyFocusProperties(properties)
        }
        return properties
    }

    /**
     * Fetch custom enter destination associated with this [focusTarget].
     *
     * Custom focus enter properties are specified as a lambda. If the user runs code in this
     * lambda that triggers a focus search, or some other focus change that causes focus to leave
     * the sub-hierarchy associated with this node, we could end up in a loop as that operation
     * will trigger another invocation of the lambda associated with the focus exit property.
     * This function prevents that re-entrant scenario by ensuring there is only one concurrent
     * invocation of this lambda.
     */
    internal inline fun fetchCustomEnter(
        focusDirection: FocusDirection,
        block: (FocusRequester) -> Unit
    ) {
        if (!isProcessingCustomEnter) {
            isProcessingCustomEnter = true
            try {
                @OptIn(ExperimentalComposeUiApi::class)
                fetchFocusProperties().enter(focusDirection).also {
                    if (it !== Default) block(it)
                }
            } finally {
                isProcessingCustomEnter = false
            }
        }
    }

    /**
     * Fetch custom exit destination associated with this [focusTarget].
     *
     * Custom focus exit properties are specified as a lambda. If the user runs code in this
     * lambda that triggers a focus search, or some other focus change that causes focus to leave
     * the sub-hierarchy associated with this node, we could end up in a loop as that operation
     * will trigger another invocation of the lambda associated with the focus exit property.
     * This function prevents that re-entrant scenario by ensuring there is only one concurrent
     * invocation of this lambda.
     */
    internal inline fun fetchCustomExit(
        focusDirection: FocusDirection,
        block: (FocusRequester) -> Unit
    ) {
        if (!isProcessingCustomExit) {
            isProcessingCustomExit = true
            try {
                @OptIn(ExperimentalComposeUiApi::class)
                fetchFocusProperties().exit(focusDirection).also {
                    if (it !== Default) block(it)
                }
            } finally {
                isProcessingCustomExit = false
            }
        }
    }

    internal fun commitFocusState() {
        with(requireTransactionManager()) {
            committedFocusState = checkNotNull(uncommittedFocusState) {
                "committing a node that was not updated in the current transaction"
            }
        }
    }

    internal fun invalidateFocus() {
        when (focusState) {
            // Clear focus from the current FocusTarget.
            // This currently clears focus from the entire hierarchy, but we can change the
            // implementation so that focus is sent to the immediate focus parent.
            Active, Captured -> {
                lateinit var focusProperties: FocusProperties
                observeReads {
                    focusProperties = fetchFocusProperties()
                }
                if (!focusProperties.canFocus) {
                    requireOwner().focusOwner.clearFocus(force = true)
                }
            }

            ActiveParent, Inactive -> {}
        }
    }

    internal fun scheduleInvalidationForFocusEvents() {
        // include possibility for ourselves to also be a focus event modifier node in case
        // we are being delegated to
        node.dispatchForKind(Nodes.FocusEvent) { eventNode ->
            eventNode.invalidateFocusEvent()
        }
        // Since this is potentially called while _this_ node is getting detached, it is possible
        // that the nodes above us are already detached, thus, we check for isAttached here.
        // We should investigate changing the order that children.detach() is called relative to
        // actually nulling out / detaching ones self.
        visitAncestors(Nodes.FocusEvent or Nodes.FocusTarget) {
            if (it.isKind(Nodes.FocusTarget)) return@visitAncestors

            if (it.isAttached) {
                it.dispatchForKind(Nodes.FocusEvent) { eventNode ->
                    eventNode.invalidateFocusEvent()
                }
            }
        }
    }

    internal object FocusTargetElement : ModifierNodeElement<FocusTargetNode>() {
        override fun create() = FocusTargetNode()

        override fun update(node: FocusTargetNode) {}

        override fun InspectorInfo.inspectableProperties() {
            name = "focusTarget"
        }

        override fun hashCode() = "focusTarget".hashCode()
        override fun equals(other: Any?) = other === this
    }
}

internal fun FocusTargetNode.requireTransactionManager(): FocusTransactionManager {
    return requireOwner().focusOwner.focusTransactionManager
}

private val FocusTargetNode.focusTransactionManager: FocusTransactionManager?
    get() = node.coordinator?.layoutNode?.owner?.focusOwner?.focusTransactionManager

internal fun FocusTargetNode.invalidateFocusTarget() {
    requireOwner().focusOwner.scheduleInvalidation(this)
}
