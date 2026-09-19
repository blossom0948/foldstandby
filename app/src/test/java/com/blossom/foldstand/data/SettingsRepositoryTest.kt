package com.blossom.foldstand.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.blossom.foldstand.domain.AmbientPreset
import com.blossom.foldstand.domain.AutoDimOption
import com.blossom.foldstand.domain.ClockStyle
import com.blossom.foldstand.domain.NightModeOption
import com.blossom.foldstand.domain.StandbySettings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private lateinit var scope: CoroutineScope
    private lateinit var repository: SettingsRepository
    private lateinit var dataStore: DataStore<Preferences>

    @Before fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(temporaryFolder.root, "settings.preferences_pb")
        }
        repository = SettingsRepository(dataStore)
    }

    @After fun tearDown() { scope.cancel() }

    @Test fun defaultSettingsMatchProductDefaults() = runBlocking {
        val settings = repository.settings.first()
        assertEquals(StandbySettings.Default, settings)
        assertFalse(settings.isRunning)
        assertTrue(settings.keepScreenOn)
    }

    @Test fun allImportantSettingsRoundTripThroughDataStore() = runBlocking {
        repository.update {
            it.copy(
                clockStyle = ClockStyle.Analog,
                ambientPreset = AmbientPreset.Candle,
                use24Hour = false,
                showSeconds = true,
                brightness = 0.55f,
                autoDim = AutoDimOption.ThirtyMinutes,
                nightMode = NightModeOption.On,
                customColors = listOf(0xFF112233, 0xFF445566, 0xFF778899),
                ambientColorIndex = 2,
                isRunning = true,
            )
        }

        val restored = repository.settings.first()
        assertEquals(ClockStyle.Analog, restored.clockStyle)
        assertEquals(AmbientPreset.Candle, restored.ambientPreset)
        assertFalse(restored.use24Hour)
        assertTrue(restored.showSeconds)
        assertEquals(0.55f, restored.brightness)
        assertEquals(AutoDimOption.ThirtyMinutes, restored.autoDim)
        assertEquals(NightModeOption.On, restored.nightMode)
        assertEquals(listOf(0xFF112233, 0xFF445566, 0xFF778899), restored.customColors)
        assertEquals(2, restored.ambientColorIndex)
        assertTrue(restored.isRunning)
    }

    @Test fun legacyBooleanNightModeIsMigrated() = runBlocking {
        val legacyKey = booleanPreferencesKey("night_mode")
        dataStore.edit { preferences -> preferences[legacyKey] = false }

        assertEquals(NightModeOption.Off, repository.settings.first().nightMode)

        dataStore.edit { preferences -> preferences[legacyKey] = true }
        assertEquals(NightModeOption.Auto, repository.settings.first().nightMode)
    }
}
