package com.blossom.foldstand.fold

import com.blossom.foldstand.domain.FoldBounds
import com.blossom.foldstand.domain.FoldOrientation
import com.blossom.foldstand.domain.FoldPosture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldLayoutCalculatorTest {
    @Test
    fun horizontalHalfOpen_usesHingeBounds() {
        val layout = FoldLayoutCalculator.calculate(
            width = 1800,
            height = 2200,
            posture = FoldPosture.HalfOpened(
                FoldOrientation.Horizontal,
                FoldBounds(0, 1080, 1800, 1120),
                isSeparating = true,
                isOccluding = true,
            ),
        )

        assertEquals(PaneRect(0, 0, 1800, 1080), layout.clock)
        assertEquals(PaneRect(0, 1120, 1800, 2200), layout.ambient)
        assertEquals(PaneAxis.Horizontal, layout.axis)
        assertTrue(layout.usesPhysicalFold)
    }

    @Test
    fun verticalHalfOpen_canReverseClockAndAmbient() {
        val layout = FoldLayoutCalculator.calculate(
            width = 2200,
            height = 1800,
            posture = FoldPosture.HalfOpened(
                FoldOrientation.Vertical,
                FoldBounds(1080, 0, 1120, 1800),
                isSeparating = true,
                isOccluding = true,
            ),
            reverseVerticalPanes = true,
        )

        assertEquals(PaneRect(1120, 0, 2200, 1800), layout.clock)
        assertEquals(PaneRect(0, 0, 1080, 1800), layout.ambient)
        assertEquals(PaneAxis.Vertical, layout.axis)
    }

    @Test
    fun flatPortrait_usesEqualTopBottomFallback() {
        val layout = FoldLayoutCalculator.calculate(1080, 2200, FoldPosture.Flat())

        assertEquals(1100, layout.clock.height)
        assertEquals(1100, layout.ambient.height)
        assertFalse(layout.usesPhysicalFold)
    }

    @Test
    fun unknownLandscape_usesEqualLeftRightFallback() {
        val layout = FoldLayoutCalculator.calculate(2200, 1080, FoldPosture.Unknown)

        assertEquals(1100, layout.clock.width)
        assertEquals(1100, layout.ambient.width)
        assertEquals(PaneAxis.Vertical, layout.axis)
    }

    @Test
    fun zeroBounds_fallsBackWithoutCreatingZeroSizedPane() {
        val layout = FoldLayoutCalculator.calculate(
            1080,
            2200,
            FoldPosture.HalfOpened(
                FoldOrientation.Horizontal,
                FoldBounds(0, 0, 0, 0),
                isSeparating = false,
                isOccluding = false,
            ),
        )

        assertEquals(1100, layout.clock.height)
        assertEquals(1100, layout.ambient.height)
        assertFalse(layout.usesPhysicalFold)
    }
}
