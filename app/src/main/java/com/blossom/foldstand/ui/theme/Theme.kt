package com.blossom.foldstand.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FoldStandColors = darkColorScheme(
    primary = Color(0xFF9BC7FF),
    onPrimary = Color(0xFF062A4A),
    secondary = Color(0xFFD6B7FF),
    background = Color.Black,
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFFA6A6A6),
    error = Color(0xFFFFB4AB),
)

@Composable
fun FoldStandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FoldStandColors,
        content = content,
    )
}
