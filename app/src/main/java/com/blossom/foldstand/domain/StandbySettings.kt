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
    val coverOnlyMode: Boolean = false,
    val customColors: List<Long> = DEFAULT_AMBIENT_COLORS,
    val ambientColorIndex: Int = 0,
    val alarmEnabled: Boolean = false,
    val alarmHour: Int = 7,
    val alarmMinute: Int = 0,
    val alarmLabel: String = "FoldStand 알람",
    val alarmRingtoneUri: String? = null,
    val isRunning: Boolean = false,
    val hasSeenManualStartNotice: Boolean = false,
) {
    companion object {
        val Default = StandbySettings()
    }
}

val DEFAULT_AMBIENT_COLORS = listOf(
    0xFFFF4D6DL,
    0xFFFFC857L,
    0xFF4DFFB8L,
    0xFF45C6FFL,
    0xFF635BFFL,
    0xFFFF4DDAL,
)
