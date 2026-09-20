package com.blossom.foldstand.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.blossom.foldstand.MainActivity
import kotlin.math.abs

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM) return
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { "FoldStand 알람" }
        val ringtone = AlarmScheduler.ringtoneUri(intent.getStringExtra(EXTRA_RINGTONE))
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = ensureChannel(manager, ringtone)
        val openIntent = PendingIntent.getActivity(
            context,
            7403,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText("FoldStand 알람입니다")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        manager.notify(NOTIFICATION_ID, notification)

        val hour = intent.getIntExtra(EXTRA_HOUR, 7)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val settings = com.blossom.foldstand.domain.StandbySettings.Default.copy(
            alarmEnabled = true,
            alarmHour = hour,
            alarmMinute = minute,
            alarmLabel = label,
            alarmRingtoneUri = intent.getStringExtra(EXTRA_RINGTONE),
        )
        AlarmScheduler(context).schedule(settings)
    }

    private fun ensureChannel(manager: NotificationManager, ringtone: android.net.Uri): String {
        val channelId = channelId(ringtone)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        val channel = NotificationChannel(
            channelId,
            "FoldStand 알람",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "FoldStand에서 설정한 알람"
            setSound(
                ringtone,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        return channelId
    }

    private fun channelId(ringtone: android.net.Uri): String =
        "foldstand_alarm_${abs(ringtone.toString().hashCode())}"

    companion object {
        const val ACTION_ALARM = "com.blossom.foldstand.action.ALARM"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_LABEL = "label"
        const val EXTRA_RINGTONE = "ringtone"
        private const val NOTIFICATION_ID = 7404
    }
}
