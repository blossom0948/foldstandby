package com.blossom.foldstand.fold

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import com.blossom.foldstand.domain.ClockStyle
import com.blossom.foldstand.domain.DualCoverUiState
import com.blossom.foldstand.domain.NightModeOption
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Always-visible native clock layer for a presented cover window.
 *
 * WindowArea implementations can create a separate view tree before Compose
 * has a lifecycle owner. Keeping the clock in a plain View means a transient
 * Compose failure can never turn the cover display into a blank black screen.
 */
@SuppressLint("ViewConstructor")
class DualCoverClockView(
    context: Context,
    private val stateFlow: StateFlow<DualCoverUiState>,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null
    private var state = stateFlow.value
    private val ticker = object : Runnable {
        override fun run() {
            postInvalidateOnAnimation()
            postDelayed(this, 1_000L)
        }
    }

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setBackgroundColor(Color.TRANSPARENT)
        contentDescription = "FoldStand 커버 시계"
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        stateJob?.cancel()
        stateJob = scope.launch {
            stateFlow.collect {
                state = it
                invalidate()
            }
        }
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        stateJob?.cancel()
        stateJob = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val settings = state.settings
        val now = ZonedDateTime.now()
        val narrow = width < height * 0.82f
        val clockWidth: Float
        val clockHeight: Float
        val centerX: Float
        val centerY: Float
        if (narrow) {
            clockWidth = width.toFloat()
            clockHeight = height * 0.62f
            centerX = width / 2f
            centerY = clockHeight / 2f
        } else {
            clockWidth = width * 0.62f
            clockHeight = height.toFloat()
            centerX = clockWidth / 2f
            centerY = height / 2f
        }
        val night = when (settings.nightMode) {
            NightModeOption.On -> true
            NightModeOption.Off -> false
            NightModeOption.Auto -> now.hour >= 22 || now.hour < 7
        }
        val primary = if (night) Color.rgb(255, 92, 92) else Color.rgb(245, 245, 245)
        val secondary = if (night) Color.rgb(224, 107, 107) else Color.rgb(198, 198, 198)
        if (settings.clockStyle == ClockStyle.Analog) {
            drawAnalog(canvas, centerX, centerY, min(clockWidth, clockHeight) * 0.29f, now, primary, secondary)
        } else {
            val pattern = when {
                settings.use24Hour && settings.showSeconds -> "HH:mm:ss"
                settings.use24Hour -> "HH:mm"
                settings.showSeconds -> "a h:mm:ss"
                else -> "a h:mm"
            }
            val time = now.format(DateTimeFormatter.ofPattern(pattern, Locale.KOREAN))
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(
                if (settings.clockStyle == ClockStyle.Flip) Typeface.MONOSPACE else Typeface.SANS_SERIF,
                if (settings.clockStyle == ClockStyle.Minimal) Typeface.NORMAL else Typeface.BOLD,
            )
            paint.color = primary
            paint.textSize = min(clockWidth * 0.22f, clockHeight * 0.22f).coerceIn(48f, 190f)
            val metrics = paint.fontMetrics
            val baseline = centerY - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(time, centerX, baseline, paint)
        }
        var infoOffset = centerY + min(clockWidth, clockHeight) * 0.23f
        if (settings.showDate) {
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.textSize = min(clockWidth, clockHeight) * 0.034f
                .coerceIn(18f, 34f)
            paint.color = secondary
            canvas.drawText(
                now.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                centerX,
                infoOffset,
                paint,
            )
            infoOffset += paint.textSize * 1.55f
        }
        if (settings.showBattery) {
            val batteryText = state.battery.percent?.let { "$it%" } ?: "배터리 정보 없음"
            paint.textSize = min(clockWidth, clockHeight) * 0.029f
                .coerceIn(16f, 28f)
            paint.color = secondary
            canvas.drawText(
                if (state.battery.isCharging) "충전 중 · $batteryText" else batteryText,
                centerX,
                infoOffset,
                paint,
            )
        }
    }

    private fun drawAnalog(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        now: ZonedDateTime,
        primary: Int,
        secondary: Int,
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = radius * 0.025f
        paint.color = Color.argb(30, 255, 255, 255)
        canvas.drawCircle(centerX, centerY, radius, paint)
        for (index in 0 until 60) {
            val angle = index / 60f * (Math.PI.toFloat() * 2f) - Math.PI.toFloat() / 2f
            val outer = radius * 0.94f
            val inner = radius * if (index % 5 == 0) 0.78f else 0.87f
            paint.color = if (index % 5 == 0) primary else secondary
            paint.alpha = if (index % 5 == 0) 220 else 120
            paint.strokeWidth = if (index % 5 == 0) radius * 0.045f else radius * 0.017f
            canvas.drawLine(
                centerX + cos(angle) * inner,
                centerY + sin(angle) * inner,
                centerX + cos(angle) * outer,
                centerY + sin(angle) * outer,
                paint,
            )
        }
        paint.alpha = 255
        val hour = ((now.hour % 12) + now.minute / 60f) / 12f * Math.PI.toFloat() * 2f - Math.PI.toFloat() / 2f
        val minute = (now.minute + now.second / 60f) / 60f * Math.PI.toFloat() * 2f - Math.PI.toFloat() / 2f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = primary
        paint.strokeWidth = radius * 0.075f
        canvas.drawLine(centerX, centerY, centerX + cos(hour) * radius * 0.52f, centerY + sin(hour) * radius * 0.52f, paint)
        paint.strokeWidth = radius * 0.05f
        canvas.drawLine(centerX, centerY, centerX + cos(minute) * radius * 0.74f, centerY + sin(minute) * radius * 0.74f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, radius * 0.065f, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }
}
