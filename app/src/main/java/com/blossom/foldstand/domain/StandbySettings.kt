package com.blossom.foldstand.domain

enum class AutoDimOption(val label: String, val delayMillis: Long?) {
    FiveMinutes("5분", 5 * 60_000L),
    FifteenMinutes("15분", 15 * 60_000L),
    ThirtyMinutes("30분", 30 * 60_000L),
    Disabled("사용 안 함", null),
}

data class StandbySettings(
    val clockStyle: ClockStyle = ClockStyle.DigitalBold,
    val ambientPreset: AmbientPreset = AmbientPreset.Aurora,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val brightness: Float = 0.25f,
    val keepScreenOn: Boolean = true,
    val burnInProtection: Boolean = true,
    val powerSavingAnimation: Boolean = true,
    val nightMode: NightModeOption = NightModeOption.Auto,
    val autoDim: AutoDimOption = AutoDimOption.FifteenMinutes,
    val reverseVerticalPanes: Boolean = false,
    val suggestWhenCharging: Boolean = false,
    val suggestWhenHalfOpened: Boolean = false,
    val customColors: List<Long> = DEFAULT_AMBIENT_COLORS,
    val ambientColorIndex: Int = 0,
    val isRunning: Boolean = false,
    val hasSeenManualStartNotice: Boolean = false,
) {
    companion object {
        val Default = StandbySettings()
    }
}

val DEFAULT_AMBIENT_COLORS = listOf(
    0xFF355C7DL,
    0xFF6C5B7BL,
    0xFFC06C84L,
)
