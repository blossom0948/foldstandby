package com.blossom.foldstand.fold

import com.blossom.foldstand.domain.FoldBounds
import com.blossom.foldstand.domain.FoldOrientation
import com.blossom.foldstand.domain.FoldPosture

data class PaneRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
}

enum class PaneAxis { Horizontal, Vertical }

data class PaneLayout(
    val clock: PaneRect,
    val ambient: PaneRect,
    val axis: PaneAxis,
    val usesPhysicalFold: Boolean,
)

object FoldLayoutCalculator {
    fun calculate(
        width: Int,
        height: Int,
        posture: FoldPosture,
        reverseVerticalPanes: Boolean = false,
    ): PaneLayout {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)

        if (posture is FoldPosture.HalfOpened) {
            calculateFromFold(
                width = safeWidth,
                height = safeHeight,
                orientation = posture.orientation,
                bounds = posture.bounds,
                reverseVerticalPanes = reverseVerticalPanes,
            )?.let { return it }
        }

        if (posture is FoldPosture.Flat && posture.isSeparating) {
            val orientation = posture.orientation
            val bounds = posture.bounds
            if (orientation != null && bounds != null) {
                calculateFromFold(
                    safeWidth,
                    safeHeight,
                    orientation,
                    bounds,
                    reverseVerticalPanes,
                )?.let { return it }
            }
        }

        return if (safeWidth > safeHeight) {
            verticalSplit(safeWidth, safeHeight, safeWidth / 2, safeWidth / 2, reverseVerticalPanes, false)
        } else {
            horizontalSplit(safeWidth, safeHeight, safeHeight / 2, safeHeight / 2, false)
        }
    }

    private fun calculateFromFold(
        width: Int,
        height: Int,
        orientation: FoldOrientation,
        bounds: FoldBounds,
        reverseVerticalPanes: Boolean,
    ): PaneLayout? = when (orientation) {
        FoldOrientation.Horizontal -> {
            val splitStart = bounds.top.coerceIn(0, height)
            val splitEnd = bounds.bottom.coerceIn(0, height)
            if (splitStart <= 0 || splitEnd >= height || splitEnd < splitStart) null
            else horizontalSplit(width, height, splitStart, splitEnd, true)
        }
        FoldOrientation.Vertical -> {
            val splitStart = bounds.left.coerceIn(0, width)
            val splitEnd = bounds.right.coerceIn(0, width)
            if (splitStart <= 0 || splitEnd >= width || splitEnd < splitStart) null
            else verticalSplit(width, height, splitStart, splitEnd, reverseVerticalPanes, true)
        }
    }

    private fun horizontalSplit(
        width: Int,
        height: Int,
        splitStart: Int,
        splitEnd: Int,
        usesPhysicalFold: Boolean,
    ) = PaneLayout(
        clock = PaneRect(0, 0, width, splitStart),
        ambient = PaneRect(0, splitEnd, width, height),
        axis = PaneAxis.Horizontal,
        usesPhysicalFold = usesPhysicalFold,
    )

    private fun verticalSplit(
        width: Int,
        height: Int,
        splitStart: Int,
        splitEnd: Int,
        reverse: Boolean,
        usesPhysicalFold: Boolean,
    ): PaneLayout {
        val left = PaneRect(0, 0, splitStart, height)
        val right = PaneRect(splitEnd, 0, width, height)
        return PaneLayout(
            clock = if (reverse) right else left,
            ambient = if (reverse) left else right,
            axis = PaneAxis.Vertical,
            usesPhysicalFold = usesPhysicalFold,
        )
    }
}
