package com.blossom.foldstand.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.blossom.foldstand.domain.BatteryState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class BatteryStateObserver(private val context: Context) {
    val state: Flow<BatteryState> = callbackFlow {
        fun emitIntent(intent: Intent?) {
            trySend(intent.toBatteryState())
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = emitIntent(intent)
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        emitIntent(
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            ),
        )
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }.distinctUntilChanged()
}

internal fun Intent?.toBatteryState(): BatteryState {
    if (this == null) return BatteryState()
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val percent = calculateBatteryPercent(level, scale)
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
    return BatteryState(
        percent = percent,
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL,
    )
}

fun calculateBatteryPercent(level: Int, scale: Int): Int? {
    if (level < 0 || scale <= 0) return null
    return ((level * 100f) / scale).toInt().coerceIn(0, 100)
}
