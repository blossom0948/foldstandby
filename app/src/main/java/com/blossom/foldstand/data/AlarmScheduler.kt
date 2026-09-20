package com.blossom.foldstand.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.blossom.foldstand.domain.StandbySettings
import java.time.LocalDateTime
import java.time.ZoneId

/** Schedules the user's local FoldStand alarm without requiring the Samsung Clock app. */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun sync(settings: StandbySettings) {
        if (!settings.alarmEnabled) {
            cancel()
            return
        }
        schedule(settings)
    }

    fun schedule(settings: StandbySettings) {
        val triggerAt = nextTriggerMillis(settings.alarmHour, settings.alarmMinute)
        val pendingIntent = pendingIntent(settings)
        val showIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, com.blossom.foldstand.MainActivity::class.java)
        val alarmClock = AlarmManager.AlarmClockInfo(triggerAt, PendingIntent.getActivity(
            context,
            SHOW_REQUEST_CODE,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag(),
        ))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(alarmClock, pendingIntent)
        } else {
            // Android 12+ may withhold exact-alarm access. Keep the alarm usable
            // with an idle-safe fallback instead of silently dropping it.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent(StandbySettings.Default))
    }

    private fun pendingIntent(settings: StandbySettings): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_HOUR, settings.alarmHour)
            putExtra(AlarmReceiver.EXTRA_MINUTE, settings.alarmMinute)
            putExtra(AlarmReceiver.EXTRA_LABEL, settings.alarmLabel)
            putExtra(AlarmReceiver.EXTRA_RINGTONE, settings.alarmRingtoneUri)
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag(),
        )
    }

    private fun immutableFlag(): Int = PendingIntent.FLAG_IMMUTABLE

    companion object {
        private const val ALARM_REQUEST_CODE = 7401
        private const val SHOW_REQUEST_CODE = 7402

        fun nextTriggerMillis(hour: Int, minute: Int, now: LocalDateTime = LocalDateTime.now()): Long {
            var target = now.withHour(hour.coerceIn(0, 23)).withMinute(minute.coerceIn(0, 59))
                .withSecond(0).withNano(0)
            if (!target.isAfter(now)) target = target.plusDays(1)
            return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun ringtoneUri(value: String?): Uri? = value?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }
}
