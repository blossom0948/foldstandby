package com.blossom.foldstand.ui.standby

import android.animation.ValueAnimator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.blossom.foldstand.domain.AmbientPreset
import com.blossom.foldstand.domain.DEFAULT_AMBIENT_COLORS
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AmbientPane(
    modifier: Modifier = Modifier,
    preset: AmbientPreset,
    colorValues: List<Long>,
    primaryColorIndex: Int = 0,
    powerSaving: Boolean,
) {
    val colors = remember(colorValues, primaryColorIndex) {
        val safe = if (colorValues.size >= 3) colorValues else DEFAULT_AMBIENT_COLORS
        val primary = primaryColorIndex.coerceIn(0, safe.lastIndex)
        val ordered = listOf(safe[primary]) + safe.indices.filter { it != primary }.map { safe[it] }.take(2)
        ordered.map(::Color)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var phase by remember { mutableFloatStateOf(0.18f) }

    LaunchedEffect(preset, powerSaving, lifecycle) {
        if (!ValueAnimator.areAnimatorsEnabled() || preset == AmbientPreset.Solid) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val durationNanos = if (powerSaving) 14_000_000_000L else 10_000_000_000L
            val frameInterval = if (powerSaving) 66_666_667L else 33_333_334L
            var start = 0L
            var lastDraw = 0L
            while (true) {
                withFrameNanos { frame ->
                    if (start == 0L) start = frame
                    if (frame - lastDraw >= frameInterval) {
                        phase = ((frame - start) % durationNanos).toFloat() / durationNanos
                        lastDraw = frame
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Color.Black)
        when (preset) {
            AmbientPreset.Solid -> {
                drawRect(colors[0].copy(alpha = 0.78f))
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.42f),
                    ),
                )
            }
            AmbientPreset.Gradient -> {
                val shift = sin(phase * 2f * PI).toFloat() * size.width * 0.16f
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(
                            colors[0].copy(alpha = 0.85f),
                            lerp(colors[1], colors[2], 0.45f).copy(alpha = 0.72f),
                            colors[2].copy(alpha = 0.8f),
                        ),
                        start = Offset(shift, 0f),
                        end = Offset(size.width - shift, size.height),
                    ),
                )
                drawRect(Color.Black.copy(alpha = 0.16f))
            }
            AmbientPreset.Aurora -> {
                val angle = phase * 2f * PI
                val centers = listOf(
                    Offset(
                        size.width * (0.28f + 0.18f * sin(angle).toFloat()),
                        size.height * (0.34f + 0.12f * cos(angle).toFloat()),
                    ),
                    Offset(
                        size.width * (0.68f + 0.15f * cos(angle * 0.83).toFloat()),
                        size.height * (0.62f + 0.18f * sin(angle * 0.77).toFloat()),
                    ),
                    Offset(
                        size.width * (0.46f + 0.16f * sin(angle * 0.61).toFloat()),
                        size.height * (0.82f + 0.08f * cos(angle * 0.7).toFloat()),
                    ),
                )
                colors.zip(centers).forEach { (color, center) ->
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.66f), Color.Transparent),
                            center = center,
                            radius = size.maxDimension * 0.72f,
                        ),
                    )
                }
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.38f)),
                    ),
                )
            }
            AmbientPreset.Sunset -> {
                val shift = sin(phase * 2f * PI).toFloat() * size.width * 0.12f
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF28133F),
                            colors[0].copy(alpha = 0.88f),
                            Color(0xFFE27D5F).copy(alpha = 0.82f),
                            Color(0xFF3B1D3F),
                        ),
                        start = Offset(shift, size.height),
                        end = Offset(size.width - shift, 0f),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFC857).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.72f, size.height * 0.28f),
                        radius = size.minDimension * 0.46f,
                    ),
                    radius = size.minDimension * 0.46f,
                    center = Offset(size.width * 0.72f, size.height * 0.28f),
                )
                drawRect(Color.Black.copy(alpha = 0.2f))
            }
            AmbientPreset.Candle -> {
                val angle = phase * 2f * PI
                val flicker = (0.88f + 0.12f * sin(angle * 3.1f)).toFloat()
                val center = Offset(
                    size.width * (0.5f + 0.06f * sin(angle * 1.7f).toFloat()),
                    size.height * (0.48f + 0.05f * cos(angle * 1.3f).toFloat()),
                )
                drawRect(Color(0xFF100804))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD27D).copy(alpha = 0.72f * flicker),
                            Color(0xFFE37B3A).copy(alpha = 0.25f * flicker),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.minDimension * 0.68f,
                    ),
                    radius = size.minDimension * 0.68f,
                    center = center,
                )
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f)),
                    ),
                )
            }
        }
    }
}
