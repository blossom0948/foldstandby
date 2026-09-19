package com.blossom.foldstand.domain

enum class ClockStyle(val label: String) {
    DigitalBold("Digital Bold"),
    Minimal("Minimal"),
    Flip("Flip"),
    Analog("Analog");

    fun next(direction: Int = 1): ClockStyle {
        val values = entries
        return values[(ordinal + direction).mod(values.size)]
    }
}
