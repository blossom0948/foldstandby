package com.blossom.foldstand.fold

import com.blossom.foldstand.domain.DualScreenStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DualScreenStatusMapperTest {
    @Test fun mapsEveryCapabilityStatusSafely() {
        assertEquals(DualScreenStatus.Unsupported, DualScreenStatusMapper.map(RawDualScreenStatus.Unsupported))
        assertEquals(DualScreenStatus.Unavailable, DualScreenStatusMapper.map(RawDualScreenStatus.Unavailable))
        assertEquals(DualScreenStatus.Available, DualScreenStatusMapper.map(RawDualScreenStatus.Available))
        assertEquals(DualScreenStatus.Active, DualScreenStatusMapper.map(RawDualScreenStatus.Active))
        assertEquals(DualScreenStatus.Unsupported, DualScreenStatusMapper.map(RawDualScreenStatus.Unknown))
    }
}
