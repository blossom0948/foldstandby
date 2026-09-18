package com.blossom.foldstand.domain

enum class ClockStyle(val label: String) {
    DigitalBold("Digital Bold"),
    Minimal("Minimal");

    fun next(direction: Int = 1): ClockStyle {
        val values = entries
        return values[(ordinal + direction).mod(values.size)]
    }
}
