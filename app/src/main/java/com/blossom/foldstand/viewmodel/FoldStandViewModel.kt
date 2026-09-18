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
import com.blossom.foldstand.domain.StandbyPage
import com.blossom.foldstand.domain.StandbyUiState
import com.blossom.foldstand.data.AppUpdateRepository
import com.blossom.foldstand.data.UpdateState
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
    private val appUpdateRepository: AppUpdateRepository,
) : ViewModel() {
    private val foldPosture = MutableStateFlow<FoldPosture>(FoldPosture.Unknown)
    private val dualScreenStatus = MutableStateFlow<DualScreenStatus>(DualScreenStatus.Checking)
    private val loaded = MutableStateFlow(false)
    private val page = MutableStateFlow(StandbyPage.Clock)

    private val settings = settingsRepository.settings
        .onEach { loaded.value = true }
    private val battery = batteryStateObserver.state

    private val baseUiState = combine(
        settings,
        battery,
        foldPosture,
        dualScreenStatus,
    ) { currentSettings, currentBattery, posture, dualStatus ->
        StandbyUiState(
            settings = currentSettings,
            battery = currentBattery,
            foldPosture = posture,
            dualScreenStatus = dualStatus,
        )
    }

    val uiState = combine(baseUiState, page, loaded) { base, currentPage, isLoaded ->
        base.copy(page = currentPage, isLoaded = isLoaded)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = StandbyUiState(),
    )

    val updateState = appUpdateRepository.state

    fun observeDualScreenStatus(status: Flow<DualScreenStatus>) {
        viewModelScope.launch { status.collect { dualScreenStatus.value = it } }
    }

    fun updateFoldPosture(posture: FoldPosture) {
        foldPosture.value = posture
    }

    fun startStandby() {
        page.value = StandbyPage.Clock
        update { it.copy(isRunning = true, hasSeenManualStartNotice = true) }
    }

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

    fun movePage(direction: Int) {
        page.value = page.value.next(direction)
    }

    fun checkForUpdates() = appUpdateRepository.checkForLatest()

    fun downloadUpdate() = appUpdateRepository.downloadAvailable()

    fun updateSettings(transform: (StandbySettings) -> StandbySettings) = update(transform)

    private fun update(transform: (StandbySettings) -> StandbySettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    class Factory(private val app: FoldStandApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FoldStandViewModel::class.java))
            return FoldStandViewModel(app.settingsRepository, app.batteryStateObserver, app.appUpdateRepository) as T
        }
    }
}
