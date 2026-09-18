package com.blossom.foldstand.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryStateObserverTest {
    @Test fun nullWhenBatteryExtrasAreInvalid() {
        assertNull(calculateBatteryPercent(-1, 100))
        assertNull(calculateBatteryPercent(50, 0))
    }

    @Test fun handlesZeroAndHundred() {
        assertEquals(0, calculateBatteryPercent(0, 100))
        assertEquals(100, calculateBatteryPercent(100, 100))
    }

    @Test fun clampsMalformedValues() {
        assertEquals(100, calculateBatteryPercent(150, 100))
    }
}
