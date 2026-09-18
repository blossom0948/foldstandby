package com.blossom.foldstand

import android.app.Application
import com.blossom.foldstand.data.BatteryStateObserver
import com.blossom.foldstand.data.AppUpdateRepository
import com.blossom.foldstand.data.SettingsRepository
import com.blossom.foldstand.data.foldStandDataStore

class FoldStandApp : Application() {
    val settingsRepository by lazy { SettingsRepository(foldStandDataStore) }
    val batteryStateObserver by lazy { BatteryStateObserver(applicationContext) }
    val appUpdateRepository by lazy { AppUpdateRepository(applicationContext) }
}
