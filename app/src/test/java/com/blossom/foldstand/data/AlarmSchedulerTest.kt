package com.blossom.foldstand.data

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerTest {
    @Test fun nextTriggerMovesToTomorrowWhenTimeHasPassed() {
        val now = LocalDateTime.of(2026, 9, 21, 8, 30, 12)
        val trigger = LocalDateTime.ofEpochSecond(
            AlarmScheduler.nextTriggerMillis(7, 45, now) / 1_000,
            0,
            java.time.ZoneId.systemDefault().rules.getOffset(now),
        )
        assertEquals(LocalDateTime.of(2026, 9, 22, 7, 45), trigger)
    }

    @Test fun nextTriggerStaysTodayWhenTimeIsAhead() {
        val now = LocalDateTime.of(2026, 9, 21, 8, 30)
        val trigger = AlarmScheduler.nextTriggerMillis(9, 0, now)
        assertTrue(trigger > now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
    }
}
