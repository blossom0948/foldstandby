package com.blossom.foldstand.domain

data class BatteryState(
    val percent: Int? = null,
    val isCharging: Boolean = false,
)

sealed interface DualScreenStatus {
    data object Checking : DualScreenStatus
    data object Unsupported : DualScreenStatus
    data object Unavailable : DualScreenStatus
    data object Available : DualScreenStatus
    data object Active : DualScreenStatus
    data class Error(val reason: String) : DualScreenStatus
}

fun DualScreenStatus.readableName(): String = when (this) {
    DualScreenStatus.Checking -> "기기 기능 확인 중"
    DualScreenStatus.Unsupported -> "지원하지 않음"
    DualScreenStatus.Unavailable -> "현재 사용할 수 없음"
    DualScreenStatus.Available -> "사용 가능 · 실험 기능"
    DualScreenStatus.Active -> "보조 화면에 시계 표시 중"
    is DualScreenStatus.Error -> "오류 · ${reason}"
}

data class StandbyUiState(
    val settings: StandbySettings = StandbySettings.Default,
    val battery: BatteryState = BatteryState(),
    val foldPosture: FoldPosture = FoldPosture.Unknown,
    val dualScreenStatus: DualScreenStatus = DualScreenStatus.Checking,
    val page: StandbyPage = StandbyPage.Clock,
    val isLoaded: Boolean = false,
) {
    val isRunning: Boolean get() = settings.isRunning
}

/**
 * State intentionally kept separate from the primary StandBy page state.
 * The rear/cover presentation must not be recreated when the inner display
 * changes its widget page or animation target.
 */
data class DualCoverUiState(
    val settings: StandbySettings = StandbySettings.Default,
    val battery: BatteryState = BatteryState(),
)
