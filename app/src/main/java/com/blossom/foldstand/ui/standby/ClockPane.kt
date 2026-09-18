package com.blossom.foldstand.ui.standby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.blossom.foldstand.domain.BatteryState
import com.blossom.foldstand.domain.ClockStyle
import com.blossom.foldstand.domain.StandbySettings
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun ClockPane(
    settings: StandbySettings,
    battery: BatteryState,
    modifier: Modifier = Modifier,
    burnInOffset: Offset = Offset.Zero,
    nightTint: Boolean = false,
) {
    val now by currentClockTime(showSeconds = settings.showSeconds)
    val timePattern = when {
        settings.use24Hour && settings.showSeconds -> "HH:mm:ss"
        settings.use24Hour -> "HH:mm"
        settings.showSeconds -> "a h:mm:ss"
        else -> "a h:mm"
    }
    val timeText = remember(now, timePattern) {
        now.format(DateTimeFormatter.ofPattern(timePattern, Locale.KOREAN))
    }
    val dateText = remember(now.toLocalDate()) {
        now.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN))
    }
    val batteryText = battery.percent?.let { "$it%" } ?: "배터리 정보 없음"
    val description = buildString {
        append(timeText)
        if (settings.showDate) append(", $dateText")
        if (settings.showBattery) append(", 배터리 $batteryText")
        if (battery.isCharging) append(", 충전 중")
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val maxClockSp = (maxWidth.value * 0.26f)
            .coerceAtMost(maxHeight.value * 0.48f)
            .coerceIn(46f, 190f)
        val minimal = settings.clockStyle == ClockStyle.Minimal
        Column(
            modifier = Modifier.padding(
                start = (20 + burnInOffset.x).dp,
                top = (20 + burnInOffset.y).dp,
                end = (20 - burnInOffset.x).dp,
                bottom = (20 - burnInOffset.y).dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = timeText,
                color = if (nightTint) Color(0xFFFF6B6B) else Color(0xFFF5F5F5),
                fontSize = (if (minimal) maxClockSp * 0.86f else maxClockSp).sp,
                fontWeight = if (minimal) FontWeight.Light else FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = if (minimal) 0.sp else (-2).sp,
                maxLines = 1,
            )
            if (settings.showDate) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (nightTint) Color(0xFFE06B6B) else Color(0xFFA6A6A6),
                    modifier = Modifier.padding(top = if (minimal) 6.dp else 2.dp),
                    maxLines = 1,
                )
            }
            if (settings.showBattery) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (battery.isCharging) {
                            Icons.Default.BatteryChargingFull
                        } else {
                            Icons.Default.BatteryStd
                        },
                        contentDescription = if (battery.isCharging) "충전 중" else "배터리 사용 중",
                        tint = if (nightTint) Color(0xFFE06B6B) else Color(0xFFA6A6A6),
                    )
                    Text(batteryText, color = if (nightTint) Color(0xFFE06B6B) else Color(0xFFA6A6A6))
                }
            }
        }
    }
}

@Composable
private fun currentClockTime(showSeconds: Boolean): androidx.compose.runtime.State<ZonedDateTime> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(
        initialValue = ZonedDateTime.now(),
        key1 = showSeconds,
        key2 = lifecycle,
    ) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val period = if (showSeconds) 1_000L else 60_000L
            while (true) {
                value = ZonedDateTime.now()
                val remainder = System.currentTimeMillis().mod(period)
                delay((period - remainder).coerceAtLeast(25L))
            }
        }
    }
}
