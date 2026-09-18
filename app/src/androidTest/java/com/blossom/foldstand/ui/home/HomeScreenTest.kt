package com.blossom.foldstand.ui.home

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.blossom.foldstand.domain.BatteryState
import com.blossom.foldstand.domain.StandbyUiState
import com.blossom.foldstand.ui.theme.FoldStandTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun standbyStartIsEnabledWhileNotCharging() {
        composeRule.setContent {
            FoldStandTheme {
                HomeScreen(
                    uiState = StandbyUiState(battery = BatteryState(percent = 72, isCharging = false)),
                    onStartStandby = {},
                    onOpenSettings = {},
                    onStartDualScreen = {},
                    onDismissManualNotice = {},
                )
            }
        }

        composeRule.onNodeWithTag("home_start_button").assertIsEnabled()
    }
}
