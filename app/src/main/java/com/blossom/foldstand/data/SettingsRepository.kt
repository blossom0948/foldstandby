package com.blossom.foldstand.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.blossom.foldstand.domain.AmbientPreset
import com.blossom.foldstand.domain.AutoDimOption
import com.blossom.foldstand.domain.ClockStyle
import com.blossom.foldstand.domain.DEFAULT_AMBIENT_COLORS
import com.blossom.foldstand.domain.StandbySettings
import com.blossom.foldstand.domain.NightModeOption
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<StandbySettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map(::decode)

    suspend fun update(transform: (StandbySettings) -> StandbySettings) {
        dataStore.edit { preferences ->
            encode(preferences, transform(decode(preferences)))
        }
    }

    suspend fun setRunning(running: Boolean) = update { it.copy(isRunning = running) }

    private fun decode(preferences: Preferences): StandbySettings = StandbySettings(
        clockStyle = enumOrDefault(preferences[Keys.clockStyle], ClockStyle.DigitalBold),
        ambientPreset = enumOrDefault(preferences[Keys.ambientPreset], AmbientPreset.Aurora),
        use24Hour = preferences[Keys.use24Hour] ?: true,
        showSeconds = preferences[Keys.showSeconds] ?: false,
        showDate = preferences[Keys.showDate] ?: true,
        showBattery = preferences[Keys.showBattery] ?: true,
        brightness = (preferences[Keys.brightness] ?: 0.25f).coerceIn(0.05f, 1f),
        keepScreenOn = preferences[Keys.keepScreenOn] ?: true,
        burnInProtection = preferences[Keys.burnInProtection] ?: true,
        powerSavingAnimation = preferences[Keys.powerSavingAnimation] ?: true,
        nightMode = decodeNightMode(preferences),
        autoDim = enumOrDefault(preferences[Keys.autoDim], AutoDimOption.FifteenMinutes),
        reverseVerticalPanes = preferences[Keys.reverseVerticalPanes] ?: false,
        suggestWhenCharging = preferences[Keys.suggestWhenCharging] ?: false,
        suggestWhenHalfOpened = preferences[Keys.suggestWhenHalfOpened] ?: false,
        customColors = listOf(
            preferences[Keys.color1] ?: DEFAULT_AMBIENT_COLORS[0],
            preferences[Keys.color2] ?: DEFAULT_AMBIENT_COLORS[1],
            preferences[Keys.color3] ?: DEFAULT_AMBIENT_COLORS[2],
        ),
        ambientColorIndex = (preferences[Keys.ambientColorIndex] ?: 0).coerceIn(0, 2),
        isRunning = preferences[Keys.isRunning] ?: false,
        hasSeenManualStartNotice = preferences[Keys.hasSeenManualStartNotice] ?: false,
    )

    private fun encode(preferences: androidx.datastore.preferences.core.MutablePreferences, value: StandbySettings) {
        preferences[Keys.clockStyle] = value.clockStyle.name
        preferences[Keys.ambientPreset] = value.ambientPreset.name
        preferences[Keys.use24Hour] = value.use24Hour
        preferences[Keys.showSeconds] = value.showSeconds
        preferences[Keys.showDate] = value.showDate
        preferences[Keys.showBattery] = value.showBattery
        preferences[Keys.brightness] = value.brightness.coerceIn(0.05f, 1f)
        preferences[Keys.keepScreenOn] = value.keepScreenOn
        preferences[Keys.burnInProtection] = value.burnInProtection
        preferences[Keys.powerSavingAnimation] = value.powerSavingAnimation
        preferences[Keys.nightModeOption] = value.nightMode.name
        preferences[Keys.autoDim] = value.autoDim.name
        preferences[Keys.reverseVerticalPanes] = value.reverseVerticalPanes
        preferences[Keys.suggestWhenCharging] = value.suggestWhenCharging
        preferences[Keys.suggestWhenHalfOpened] = value.suggestWhenHalfOpened
        preferences[Keys.color1] = value.customColors.getOrElse(0) { DEFAULT_AMBIENT_COLORS[0] }
        preferences[Keys.color2] = value.customColors.getOrElse(1) { DEFAULT_AMBIENT_COLORS[1] }
        preferences[Keys.color3] = value.customColors.getOrElse(2) { DEFAULT_AMBIENT_COLORS[2] }
        preferences[Keys.ambientColorIndex] = value.ambientColorIndex.coerceIn(0, 2)
        preferences[Keys.isRunning] = value.isRunning
        preferences[Keys.hasSeenManualStartNotice] = value.hasSeenManualStartNotice
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private fun decodeNightMode(preferences: Preferences): NightModeOption =
        preferences[Keys.nightModeOption]?.let { value ->
            enumOrDefault(value, NightModeOption.Auto)
        } ?: when (preferences[Keys.legacyNightMode]) {
            true -> NightModeOption.Auto
            false -> NightModeOption.Off
            null -> NightModeOption.Auto
        }

    private object Keys {
        val clockStyle = stringPreferencesKey("clock_style")
        val ambientPreset = stringPreferencesKey("ambient_preset")
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val showSeconds = booleanPreferencesKey("show_seconds")
        val showDate = booleanPreferencesKey("show_date")
        val showBattery = booleanPreferencesKey("show_battery")
        val brightness = floatPreferencesKey("brightness")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val burnInProtection = booleanPreferencesKey("burn_in_protection")
        val powerSavingAnimation = booleanPreferencesKey("power_saving_animation")
        val nightModeOption = stringPreferencesKey("night_mode_option")
        val legacyNightMode = booleanPreferencesKey("night_mode")
        val autoDim = stringPreferencesKey("auto_dim")
        val reverseVerticalPanes = booleanPreferencesKey("reverse_vertical_panes")
        val suggestWhenCharging = booleanPreferencesKey("suggest_when_charging")
        val suggestWhenHalfOpened = booleanPreferencesKey("suggest_when_half_opened")
        val color1 = longPreferencesKey("ambient_color_1")
        val color2 = longPreferencesKey("ambient_color_2")
        val color3 = longPreferencesKey("ambient_color_3")
        val ambientColorIndex = intPreferencesKey("ambient_color_index")
        val isRunning = booleanPreferencesKey("standby_running")
        val hasSeenManualStartNotice = booleanPreferencesKey("seen_manual_start_notice")
    }
}
