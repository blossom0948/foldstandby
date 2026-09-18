package com.blossom.foldstand.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.blossom.foldstand.FoldStandApp
import com.blossom.foldstand.data.BatteryStateObserver
import com.blossom.foldstand.data.SettingsRepository
import com.blossom.foldstand.domain.AmbientPreset
import com.blossom.foldstand.domain.ClockStyle
import com.blossom.foldstand.domain.DualScreenStatus
import com.blossom.foldstand.domain.FoldPosture
import com.blossom.foldstand.domain.StandbySettings
import com.blossom.foldstand.domain.StandbyUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoldStandViewModel(
    private val settingsRepository: SettingsRepository,
    batteryStateObserver: BatteryStateObserver,
) : ViewModel() {
    private val foldPosture = MutableStateFlow<FoldPosture>(FoldPosture.Unknown)
    private val dualScreenStatus = MutableStateFlow<DualScreenStatus>(DualScreenStatus.Checking)
    private val loaded = MutableStateFlow(false)

    private val settings = settingsRepository.settings
        .onEach { loaded.value = true }
    private val battery = batteryStateObserver.state

    val uiState = combine(
        settings,
        battery,
        foldPosture,
        dualScreenStatus,
        loaded,
    ) { currentSettings, currentBattery, posture, dualStatus, isLoaded ->
        StandbyUiState(
            settings = currentSettings,
            battery = currentBattery,
            foldPosture = posture,
            dualScreenStatus = dualStatus,
            isLoaded = isLoaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = StandbyUiState(),
    )

    fun observeDualScreenStatus(status: Flow<DualScreenStatus>) {
        viewModelScope.launch { status.collect { dualScreenStatus.value = it } }
    }

    fun updateFoldPosture(posture: FoldPosture) {
        foldPosture.value = posture
    }

    fun startStandby() = update { it.copy(isRunning = true, hasSeenManualStartNotice = true) }

    fun stopStandby() = update { it.copy(isRunning = false) }

    fun dismissManualStartNotice() = update { it.copy(hasSeenManualStartNotice = true) }

    fun setClockStyle(style: ClockStyle) = update { it.copy(clockStyle = style) }

    fun setAmbientPreset(preset: AmbientPreset) = update { it.copy(ambientPreset = preset) }

    fun cycleClockStyle(direction: Int) = update {
        it.copy(clockStyle = it.clockStyle.next(direction))
    }

    fun cycleAmbientPreset(direction: Int) = update {
        it.copy(ambientPreset = it.ambientPreset.next(direction))
    }

    fun updateSettings(transform: (StandbySettings) -> StandbySettings) = update(transform)

    private fun update(transform: (StandbySettings) -> StandbySettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    class Factory(private val app: FoldStandApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FoldStandViewModel::class.java))
            return FoldStandViewModel(app.settingsRepository, app.batteryStateObserver) as T
        }
    }
}
