package com.blossom.foldstand.ui.standby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
import kotlin.math.sin
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
        val analogSize = (maxWidth.value * 0.56f)
            .coerceAtMost(maxHeight.value * 0.54f)
            .coerceIn(90f, 340f)
        val minimal = settings.clockStyle == ClockStyle.Minimal
        val timeColor = if (nightTint) Color(0xFFFF6B6B) else Color(0xFFF5F5F5)
        val secondaryColor = if (nightTint) Color(0xFFE06B6B) else Color(0xFFA6A6A6)
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
            when (settings.clockStyle) {
                ClockStyle.DigitalBold,
                ClockStyle.Minimal,
                -> Text(
                    text = timeText,
                    color = timeColor,
                    fontSize = (if (minimal) maxClockSp * 0.86f else maxClockSp).sp,
                    fontWeight = if (minimal) FontWeight.Light else FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = if (minimal) 0.sp else (-2).sp,
                    maxLines = 1,
                )

                ClockStyle.Flip -> {
                    val period = if (settings.use24Hour) {
                        null
                    } else {
                        now.format(DateTimeFormatter.ofPattern("a", Locale.KOREAN))
                    }
                    FlipClockDisplay(
                        text = timeText.removePrefix("오전 ").removePrefix("오후 "),
                        period = period,
                        fontSize = (if (settings.showSeconds) maxClockSp * 0.62f else maxClockSp * 0.78f)
                            .coerceIn(34f, 126f),
                        color = timeColor,
                    )
                }

                ClockStyle.Analog -> AnalogClockDisplay(
                    time = now,
                    size = analogSize.dp,
                    tint = timeColor,
                    mutedTint = secondaryColor,
                )
            }
            if (settings.showDate) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    color = secondaryColor,
                    modifier = Modifier.padding(top = if (minimal) 6.dp else 10.dp),
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
                        tint = secondaryColor,
                    )
                    Text(batteryText, color = secondaryColor)
                }
            }
        }
    }
}

/**
 * A compact, low-power flip clock. Each digit is rendered as its own tile so the
 * centre hinge and the two halves of a foldable display remain visually distinct.
 */
@Composable
private fun FlipClockDisplay(
    text: String,
    period: String?,
    fontSize: Float,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            text.forEach { character ->
                if (character.isDigit()) {
                    FlipDigit(character.toString(), fontSize, color)
                } else {
                    Text(
                        text = character.toString(),
                        color = color.copy(alpha = 0.84f),
                        fontSize = (fontSize * 0.72f).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        if (period != null) {
            Text(
                text = period,
                color = color.copy(alpha = 0.78f),
                fontSize = (fontSize * 0.26f).sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun FlipDigit(value: String, fontSize: Float, color: Color) {
    val tileWidth = (fontSize * 0.62f).coerceIn(25f, 78f).dp
    val tileHeight = (fontSize * 1.12f).coerceIn(42f, 132f).dp
    Surface(
        modifier = Modifier
            .width(tileWidth)
            .height(tileHeight),
        shape = RoundedCornerShape((fontSize * 0.12f).coerceIn(7f, 16f).dp),
        color = Color(0xFF1B1B1D),
        shadowElevation = 2.dp,
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Text(
                text = value,
                color = color,
                fontSize = (fontSize * 0.74f).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Black.copy(alpha = 0.42f))
                    .align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun AnalogClockDisplay(
    time: ZonedDateTime,
    size: androidx.compose.ui.unit.Dp,
    tint: Color,
    mutedTint: Color,
) {
    Canvas(
        modifier = Modifier
            .size(size)
            .semantics { contentDescription = "아날로그 시계" },
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension * 0.45f
        drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = radius,
            center = center,
            style = Stroke(width = this.size.minDimension * 0.012f),
        )
        for (index in 0 until 60) {
            val angle = (index / 60f) * (2f * Math.PI.toFloat()) - Math.PI.toFloat() / 2f
            val isHour = index % 5 == 0
            val outer = radius * 0.94f
            val inner = radius * if (isHour) 0.78f else 0.87f
            drawLine(
                color = if (isHour) tint.copy(alpha = 0.86f) else mutedTint.copy(alpha = 0.48f),
                start = Offset(
                    center.x + cos(angle) * inner,
                    center.y + sin(angle) * inner,
                ),
                end = Offset(
                    center.x + cos(angle) * outer,
                    center.y + sin(angle) * outer,
                ),
                strokeWidth = if (isHour) this.size.minDimension * 0.018f else this.size.minDimension * 0.007f,
                cap = StrokeCap.Round,
            )
        }
        val hourAngle = (((time.hour % 12) + time.minute / 60f) / 12f) * (2f * Math.PI.toFloat()) - Math.PI.toFloat() / 2f
        val minuteAngle = ((time.minute + time.second / 60f) / 60f) * (2f * Math.PI.toFloat()) - Math.PI.toFloat() / 2f
        val secondAngle = (time.second / 60f) * (2f * Math.PI.toFloat()) - Math.PI.toFloat() / 2f
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + cos(hourAngle) * radius * 0.52f, center.y + sin(hourAngle) * radius * 0.52f),
            strokeWidth = this.size.minDimension * 0.035f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + cos(minuteAngle) * radius * 0.74f, center.y + sin(minuteAngle) * radius * 0.74f),
            strokeWidth = this.size.minDimension * 0.022f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = if (tint.red > 0.8f && tint.green < 0.6f) tint else Color(0xFFE06B6B),
            start = Offset(center.x - cos(secondAngle) * radius * 0.2f, center.y - sin(secondAngle) * radius * 0.2f),
            end = Offset(center.x + cos(secondAngle) * radius * 0.82f, center.y + sin(secondAngle) * radius * 0.82f),
            strokeWidth = this.size.minDimension * 0.009f,
            cap = StrokeCap.Round,
        )
        drawCircle(color = tint, radius = this.size.minDimension * 0.035f, center = center)
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
