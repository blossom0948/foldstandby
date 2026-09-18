package com.blossom.foldstand.domain

enum class AmbientPreset(val label: String) {
    Solid("단색"),
    Gradient("그라데이션"),
    Aurora("오로라");

    fun next(direction: Int = 1): AmbientPreset {
        val values = entries
        return values[(ordinal + direction).mod(values.size)]
    }
}
