package com.blossom.foldstand.domain

data class FoldBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val isMeaningful: Boolean get() = width > 0 || height > 0
}

enum class FoldOrientation { Horizontal, Vertical }

sealed interface FoldPosture {
    data object Unknown : FoldPosture

    data class Flat(
        val orientation: FoldOrientation? = null,
        val bounds: FoldBounds? = null,
        val isSeparating: Boolean = false,
    ) : FoldPosture

    data class HalfOpened(
        val orientation: FoldOrientation,
        val bounds: FoldBounds,
        val isSeparating: Boolean,
        val isOccluding: Boolean,
    ) : FoldPosture
}

fun FoldPosture.readableName(): String = when (this) {
    FoldPosture.Unknown -> "일반 화면 모드"
    is FoldPosture.Flat -> "펼침"
    is FoldPosture.HalfOpened -> "반접힘"
}
