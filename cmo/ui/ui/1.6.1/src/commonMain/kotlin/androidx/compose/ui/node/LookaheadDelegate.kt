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

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadLayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.PlacementScope
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * This is the base class for NodeCoordinator and LookaheadDelegate. The common
 * functionalities between the two are extracted here.
 */
internal abstract class LookaheadCapablePlaceable : Placeable(), MeasureScopeWithLayoutNode {
    abstract val position: IntOffset
    abstract val child: LookaheadCapablePlaceable?
    abstract val parent: LookaheadCapablePlaceable?
    abstract val hasMeasureResult: Boolean
    abstract override val layoutNode: LayoutNode
    abstract val coordinates: LayoutCoordinates
    final override fun get(alignmentLine: AlignmentLine): Int {
        if (!hasMeasureResult) return AlignmentLine.Unspecified
        val measuredPosition = calculateAlignmentLine(alignmentLine)
        if (measuredPosition == AlignmentLine.Unspecified) return AlignmentLine.Unspecified
        return measuredPosition + if (alignmentLine is VerticalAlignmentLine) {
            apparentToRealOffset.x
        } else {
            apparentToRealOffset.y
        }
    }

    abstract fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int

    // True when the coordinator is running its own placing block to obtain the position
    // in parent, but is not interested in the position of children.
    internal var isShallowPlacing: Boolean = false
    internal abstract val measureResult: MeasureResult
    internal abstract fun replace()
    abstract val alignmentLinesOwner: AlignmentLinesOwner

    /**
     * Used to indicate that this placement pass is for the purposes of calculating an
     * alignment line. If it is, then
     * [LayoutNodeLayoutDelegate.coordinatesAccessedDuringPlacement] will be changed
     * when [Placeable.PlacementScope.coordinates] is accessed to indicate that the placement
     * is not finalized and must be run again.
     */
    internal var isPlacingForAlignment = false

    /**
     * [PlacementScope] used to place children.
     */
    val placementScope = PlacementScope(this)

    protected fun NodeCoordinator.invalidateAlignmentLinesFromPositionChange() {
        if (wrapped?.layoutNode != layoutNode) {
            alignmentLinesOwner.alignmentLines.onAlignmentsChanged()
        } else {
            alignmentLinesOwner.parentAlignmentLinesOwner?.alignmentLines?.onAlignmentsChanged()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override val isLookingAhead: Boolean
        get() = false

    override fun layout(
        width: Int,
        height: Int,
        alignmentLines: Map<AlignmentLine, Int>,
        placementBlock: PlacementScope.() -> Unit
    ): MeasureResult {
        checkMeasuredSize(width, height)
        return object : MeasureResult {
            override val width: Int
                get() = width
            override val height: Int
                get() = height
            override val alignmentLines: Map<AlignmentLine, Int>
                get() = alignmentLines

            override fun placeChildren() {
                placementScope.placementBlock()
            }
        }
    }
}

// This is about 16 million pixels. That should be big enough. We'll treat anything bigger as an
// error.
private const val MaxLayoutDimension = (1 shl 24) - 1
private const val MaxLayoutMask: Int = 0xFF00_0000.toInt()

@Suppress("NOTHING_TO_INLINE")
internal inline fun checkMeasuredSize(width: Int, height: Int) {
    check(width and MaxLayoutMask == 0 && height and MaxLayoutMask == 0) {
        "Size($width x $height) is out of range. Each dimension must be between 0 and " +
            "$MaxLayoutDimension."
    }
}

internal abstract class LookaheadDelegate(
    val coordinator: NodeCoordinator,
) : Measurable, LookaheadCapablePlaceable() {
    override val child: LookaheadCapablePlaceable?
        get() = coordinator.wrapped?.lookaheadDelegate
    override val hasMeasureResult: Boolean
        get() = _measureResult != null
    override var position = IntOffset.Zero
    private var oldAlignmentLines: MutableMap<AlignmentLine, Int>? = null
    override val measureResult: MeasureResult
        get() = _measureResult ?: error(
            "LookaheadDelegate has not been measured yet when measureResult is requested."
        )
    override val isLookingAhead: Boolean
        get() = true
    override val layoutDirection: LayoutDirection
        get() = coordinator.layoutDirection
    override val density: Float
        get() = coordinator.density
    override val fontScale: Float
        get() = coordinator.fontScale
    override val parent: LookaheadCapablePlaceable?
        get() = coordinator.wrappedBy?.lookaheadDelegate
    override val layoutNode: LayoutNode
        get() = coordinator.layoutNode
    override val coordinates: LayoutCoordinates
        get() = lookaheadLayoutCoordinates

    val lookaheadLayoutCoordinates = LookaheadLayoutCoordinates(this)
    override val alignmentLinesOwner: AlignmentLinesOwner
        get() = coordinator.layoutNode.layoutDelegate.lookaheadAlignmentLinesOwner!!

    private var _measureResult: MeasureResult? = null
        set(result) {
            result?.let {
                measuredSize = IntSize(it.width, it.height)
            } ?: run { measuredSize = IntSize.Zero }
            if (field != result && result != null) {
                // We do not simply compare against old.alignmentLines in case this is a
                // MutableStateMap and the same instance might be passed.
                if ((!oldAlignmentLines.isNullOrEmpty() || result.alignmentLines.isNotEmpty()) &&
                    result.alignmentLines != oldAlignmentLines
                ) {
                    alignmentLinesOwner.alignmentLines.onAlignmentsChanged()

                    @Suppress("PrimitiveInCollection")
                    val oldLines = oldAlignmentLines
                        ?: (mutableMapOf<AlignmentLine, Int>().also { oldAlignmentLines = it })
                    oldLines.clear()
                    oldLines.putAll(result.alignmentLines)
                }
            }
            field = result
        }

    protected val cachedAlignmentLinesMap = mutableMapOf<AlignmentLine, Int>()

    internal fun getCachedAlignmentLine(alignmentLine: AlignmentLine): Int =
        cachedAlignmentLinesMap[alignmentLine] ?: AlignmentLine.Unspecified

    override fun replace() {
        placeAt(position, 0f, null)
    }

    final override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        placeSelf(position)
        if (isShallowPlacing) return
        placeChildren()
    }

    private fun placeSelf(position: IntOffset) {
        if (this.position != position) {
            this.position = position
            layoutNode.layoutDelegate.lookaheadPassDelegate
                ?.notifyChildrenUsingCoordinatesWhilePlacing()
            coordinator.invalidateAlignmentLinesFromPositionChange()
        }
    }

    internal fun placeSelfApparentToRealOffset(position: IntOffset) {
        placeSelf(position + apparentToRealOffset)
    }

    protected open fun placeChildren() {
        measureResult.placeChildren()
    }

    inline fun performingMeasure(
        constraints: Constraints,
        block: () -> MeasureResult
    ): Placeable {
        measurementConstraints = constraints
        _measureResult = block()
        return this
    }

    override val parentData: Any?
        get() = coordinator.parentData

    override fun minIntrinsicWidth(height: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.minIntrinsicWidth(height)
    }

    override fun maxIntrinsicWidth(height: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.maxIntrinsicWidth(height)
    }

    override fun minIntrinsicHeight(width: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.minIntrinsicHeight(width)
    }

    override fun maxIntrinsicHeight(width: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.maxIntrinsicHeight(width)
    }

    internal fun positionIn(ancestor: LookaheadDelegate): IntOffset {
        var aggregatedOffset = IntOffset.Zero
        var lookaheadDelegate = this
        while (lookaheadDelegate != ancestor) {
            aggregatedOffset += lookaheadDelegate.position
            lookaheadDelegate = lookaheadDelegate.coordinator.wrappedBy!!.lookaheadDelegate!!
        }
        return aggregatedOffset
    }
}
