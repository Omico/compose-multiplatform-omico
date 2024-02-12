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

import androidx.collection.MutableLongSet
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.CustomDestinationResult.Cancelled
import androidx.compose.ui.focus.CustomDestinationResult.None
import androidx.compose.ui.focus.CustomDestinationResult.RedirectCancelled
import androidx.compose.ui.focus.CustomDestinationResult.Redirected
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusDirection.Companion.Next
import androidx.compose.ui.focus.FocusDirection.Companion.Previous
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeKind
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ancestors
import androidx.compose.ui.node.dispatchForKind
import androidx.compose.ui.node.nearestAncestor
import androidx.compose.ui.node.visitLocalDescendants
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed

/**
 * The focus manager is used by different [Owner][androidx.compose.ui.node.Owner] implementations
 * to control focus.
 */
internal class FocusOwnerImpl(onRequestApplyChangesListener: (() -> Unit) -> Unit) : FocusOwner {

    internal var rootFocusNode = FocusTargetNode()

    private val focusInvalidationManager = FocusInvalidationManager(onRequestApplyChangesListener)

    override val focusTransactionManager: FocusTransactionManager = FocusTransactionManager()

    /**
     * A [Modifier] that can be added to the [Owners][androidx.compose.ui.node.Owner] modifier
     * list that contains the modifiers required by the focus system. (Eg, a root focus modifier).
     */
    // TODO(b/168831247): return an empty Modifier when there are no focusable children.
    override val modifier: Modifier = object : ModifierNodeElement<FocusTargetNode>() {
        override fun create() = rootFocusNode

        override fun update(node: FocusTargetNode) {}

        override fun InspectorInfo.inspectableProperties() {
            name = "RootFocusTarget"
        }

        override fun hashCode(): Int = rootFocusNode.hashCode()

        override fun equals(other: Any?) = other === this
    }

    override lateinit var layoutDirection: LayoutDirection

    /**
     * Keeps track of which keys have received DOWN events without UP events – i.e. which keys are
     * currently down. This is used to detect UP events for keys that aren't down and ignore them.
     *
     * This set is lazily initialized the first time a DOWN event is received for a key.
     */
    // TODO(b/307580000) Factor this state out into a class to manage key inputs.
    private var keysCurrentlyDown: MutableLongSet? = null

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it gains focus. This
     * informs the [focus manager][FocusOwnerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] gained focus, and that it should propagate this
     * focus to one of the focus modifiers in the component hierarchy.
     */
    override fun takeFocus() {
        // If the focus state is not Inactive, it indicates that the focus state is already
        // set (possibly by dispatchWindowFocusChanged). So we don't update the state.
        if (rootFocusNode.focusState == Inactive) {
            rootFocusNode.focusState = Active
            // TODO(b/152535715): propagate focus to children based on child focusability.
            //  moveFocus(FocusDirection.Enter)
        }
    }

    /**
     * The [Owner][androidx.compose.ui.node.Owner] calls this function when it loses focus. This
     * informs the [focus manager][FocusOwnerImpl] that the
     * [Owner][androidx.compose.ui.node.Owner] lost focus, and that it should clear focus from
     * all the focus modifiers in the component hierarchy.
     */
    override fun releaseFocus() {
        rootFocusNode.clearFocus(forced = true, refreshFocusEvents = true)
    }

    /**
     * Call this function to set the focus to the root focus modifier.
     *
     * @param force: Whether we should forcefully clear focus regardless of whether we have
     * any components that have captured focus.
     *
     * This could be used to clear focus when a user clicks on empty space outside a focusable
     * component.
     */
    override fun clearFocus(force: Boolean) {
        clearFocus(force, refreshFocusEvents = true)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun clearFocus(force: Boolean, refreshFocusEvents: Boolean) {
        focusTransactionManager.withNewTransaction {
            // Don't clear focus if an item on the focused path has a custom exit specified.
            if (!force) {
                when (rootFocusNode.performCustomClearFocus(Exit)) {
                    Redirected, Cancelled, RedirectCancelled -> return
                    None -> { /* Do nothing. */ }
                }
            }

            // If this hierarchy had focus before clearing it, it indicates that the host view has
            // focus. So after clearing focus within the compose hierarchy, we should restore focus
            // to the root focus modifier to maintain consistency with the host view.
            val rootInitialState = rootFocusNode.focusState
            if (rootFocusNode.clearFocus(force, refreshFocusEvents)) {
                rootFocusNode.focusState = when (rootInitialState) {
                    Active, ActiveParent, Captured -> Active
                    Inactive -> Inactive
                }
            }
        }
    }

    /**
     * Moves focus in the specified direction.
     *
     * @return true if focus was moved successfully. false if the focused item is unchanged.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    override fun moveFocus(focusDirection: FocusDirection): Boolean {

        // If there is no active node in this sub-hierarchy, we can't move focus.
        val source = rootFocusNode.findActiveFocusNode() ?: return false

        // Check if a custom focus traversal order is specified.
        source.customFocusSearch(focusDirection, layoutDirection).also {
            if (it !== Default) {
                return it !== Cancel && it.focus()
            }
        }

        var isCancelled = false
        val foundNextItem =
            rootFocusNode.focusSearch(focusDirection, layoutDirection) { destination ->
                if (destination == source) return@focusSearch false
                checkNotNull(destination.nearestAncestor(Nodes.FocusTarget)) {
                    "Focus search landed at the root."
                }
                // If we found a potential next item, move focus to it.
                // Returning true ends focus search.
                focusTransactionManager.withNewTransaction {
                    when (destination.performCustomRequestFocus(focusDirection)) {
                        Redirected -> true
                        Cancelled, RedirectCancelled -> { isCancelled = true; true }
                        None -> destination.performRequestFocus()
                    }
                }
            }
        // If we didn't find a potential next item, try to wrap around.
        return !isCancelled && (foundNextItem || wrapAroundFocus(focusDirection))
    }

    /**
     * Dispatches a key event through the compose hierarchy.
     */
    override fun dispatchKeyEvent(keyEvent: KeyEvent): Boolean {
        if (!validateKeyEvent(keyEvent)) return false

        val activeFocusTarget = rootFocusNode.findActiveFocusNode()
        checkNotNull(activeFocusTarget) {
            "Event can't be processed because we do not have an active focus target."
        }
        val focusedKeyInputNode = activeFocusTarget.lastLocalKeyInputNode()
            ?: activeFocusTarget.nearestAncestor(Nodes.KeyInput)?.node

        focusedKeyInputNode?.traverseAncestors(
            type = Nodes.KeyInput,
            onPreVisit = { if (it.onPreKeyEvent(keyEvent)) return true },
            onVisit = { if (it.onKeyEvent(keyEvent)) return true }
        )
        return false
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun dispatchInterceptedSoftKeyboardEvent(keyEvent: KeyEvent): Boolean {
        val focusedSoftKeyboardInterceptionNode = rootFocusNode.findActiveFocusNode()
            ?.nearestAncestor(Nodes.SoftKeyboardKeyInput)

        focusedSoftKeyboardInterceptionNode?.traverseAncestors(
            type = Nodes.SoftKeyboardKeyInput,
            onPreVisit = { if (it.onPreInterceptKeyBeforeSoftKeyboard(keyEvent)) return true },
            onVisit = { if (it.onInterceptKeyBeforeSoftKeyboard(keyEvent)) return true }
        )
        return false
    }

    /**
     * Dispatches a rotary scroll event through the compose hierarchy.
     */
    override fun dispatchRotaryEvent(event: RotaryScrollEvent): Boolean {
        val focusedRotaryInputNode = rootFocusNode.findActiveFocusNode()
            ?.nearestAncestor(Nodes.RotaryInput)

        focusedRotaryInputNode?.traverseAncestors(
            type = Nodes.RotaryInput,
            onPreVisit = { if (it.onPreRotaryScrollEvent(event)) return true },
            onVisit = { if (it.onRotaryScrollEvent(event)) return true }
        )

        return false
    }

    override fun scheduleInvalidation(node: FocusTargetNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    override fun scheduleInvalidation(node: FocusEventModifierNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    override fun scheduleInvalidation(node: FocusPropertiesModifierNode) {
        focusInvalidationManager.scheduleInvalidation(node)
    }

    private inline fun <reified T : DelegatableNode> DelegatableNode.traverseAncestors(
        type: NodeKind<T>,
        onPreVisit: (T) -> Unit,
        onVisit: (T) -> Unit
    ) {
        val ancestors = ancestors(type)
        ancestors?.fastForEachReversed(onPreVisit)
        node.dispatchForKind(type, onPreVisit)
        node.dispatchForKind(type, onVisit)
        ancestors?.fastForEach(onVisit)
    }

    /**
     * Searches for the currently focused item, and returns its coordinates as a rect.
     */
    override fun getFocusRect(): Rect? {
        return rootFocusNode.findActiveFocusNode()?.focusRect()
    }

    private fun DelegatableNode.lastLocalKeyInputNode(): Modifier.Node? {
        var focusedKeyInputNode: Modifier.Node? = null
        visitLocalDescendants(Nodes.FocusTarget or Nodes.KeyInput) { modifierNode ->
            if (modifierNode.isKind(Nodes.FocusTarget)) return focusedKeyInputNode

            focusedKeyInputNode = modifierNode
        }
        return focusedKeyInputNode
    }

    // TODO(b/144116848): This is a hack to make Next/Previous wrap around. This must be
    //  replaced by code that sends the move request back to the view system. The view system
    //  will then pass focus to other views, and ultimately return back to this compose view.
    private fun wrapAroundFocus(focusDirection: FocusDirection): Boolean {
        // Wrap is not supported when this sub-hierarchy doesn't have focus.
        if (!rootFocusNode.focusState.hasFocus || rootFocusNode.focusState.isFocused) return false

        // Next and Previous wraps around.
        when (focusDirection) {
            Next, Previous -> {
                // Clear Focus to send focus the root node.
                clearFocus(force = false)
                if (!rootFocusNode.focusState.isFocused) return false

                // Wrap around by calling moveFocus after the root gains focus.
                return moveFocus(focusDirection)
            }
            // We only wrap-around for 1D Focus search.
            else -> return false
        }
    }

    // TODO(b/307580000) Factor this out into a class to manage key inputs.
    private fun validateKeyEvent(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.key.keyCode
        when (keyEvent.type) {
            KeyDown -> {
                // It's probably rare for more than 3 hardware keys to be pressed simultaneously.
                val keysCurrentlyDown = keysCurrentlyDown ?: MutableLongSet(initialCapacity = 3)
                    .also { keysCurrentlyDown = it }
                keysCurrentlyDown += keyCode
            }

            KeyUp -> {
                if (keysCurrentlyDown?.contains(keyCode) != true) {
                    // An UP event for a key that was never DOWN is invalid, ignore it.
                    return false
                }
                keysCurrentlyDown?.remove(keyCode)
            }
            // Always process Unknown event types.
        }
        return true
    }
}
