package com.blossom.foldstand

import android.app.Application
import com.blossom.foldstand.data.BatteryStateObserver
import com.blossom.foldstand.data.AppUpdateRepository
import com.blossom.foldstand.data.AlarmScheduler
import com.blossom.foldstand.data.SettingsRepository
import com.blossom.foldstand.data.foldStandDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FoldStandApp : Application() {
    val settingsRepository by lazy { SettingsRepository(foldStandDataStore) }
    val batteryStateObserver by lazy { BatteryStateObserver(applicationContext) }
    val appUpdateRepository by lazy { AppUpdateRepository(applicationContext) }
    val alarmScheduler by lazy { AlarmScheduler(applicationContext) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                runCatching { alarmScheduler.sync(settings) }
            }
        }
    }
}
