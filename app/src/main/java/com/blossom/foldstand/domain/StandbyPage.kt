package com.blossom.foldstand.domain

enum class StandbyPage(val label: String) {
    Clock("시계"),
    Widgets("위젯"),
    Calendar("달력"),
    Notifications("알림");

    fun next(direction: Int): StandbyPage {
        val pages = entries
        return pages[(ordinal + direction).mod(pages.size)]
    }
}

data class CalendarEvent(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val location: String? = null,
    val allDay: Boolean = false,
)

data class NotificationItem(
    val appName: String,
    val title: String,
    val text: String,
    val postedAtMillis: Long,
)
