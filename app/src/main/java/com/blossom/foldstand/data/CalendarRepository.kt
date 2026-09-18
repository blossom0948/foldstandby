package com.blossom.foldstand.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.blossom.foldstand.domain.CalendarEvent
import java.time.LocalDateTime
import java.time.ZoneId

object CalendarRepository {
    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun readUpcoming(context: Context, nowMillis: Long = System.currentTimeMillis()): List<CalendarEvent> {
        if (!hasReadPermission(context)) return emptyList()
        val endMillis = LocalDateTime.now().plusDays(7)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(nowMillis.toString())
            .appendPath(endMillis.toString())
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
        )
        return runCatching {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                val titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = cursor.getColumnIndex(CalendarContract.Instances.END)
                val locationIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val allDayIndex = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                buildList {
                    while (cursor.moveToNext() && size < 8) {
                        add(
                            CalendarEvent(
                                title = cursor.getString(titleIndex).orEmpty().ifBlank { "제목 없는 일정" },
                                startMillis = cursor.getLong(beginIndex),
                                endMillis = cursor.getLong(endIndex),
                                location = cursor.getString(locationIndex)?.takeIf { it.isNotBlank() },
                                allDay = cursor.getInt(allDayIndex) == 1,
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
}
