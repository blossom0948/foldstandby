package com.blossom.foldstand.domain

enum class AmbientPreset(val label: String) {
    White("화이트"),
    Solid("단색"),
    Gradient("그라데이션"),
    Aurora("오로라"),
    Spectrum("스펙트럼"),
    Sunset("선셋"),
    Candle("촛불");

    fun next(direction: Int = 1): AmbientPreset {
        val values = entries
        return values[(ordinal + direction).mod(values.size)]
    }
}
