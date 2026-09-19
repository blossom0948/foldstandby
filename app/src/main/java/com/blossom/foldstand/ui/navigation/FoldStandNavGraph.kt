package com.blossom.foldstand.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.blossom.foldstand.ui.home.HomeScreen
import com.blossom.foldstand.ui.settings.SettingsScreen
import com.blossom.foldstand.ui.standby.StandbyScreen
import com.blossom.foldstand.viewmodel.FoldStandViewModel

private object Routes {
    const val Home = "home"
    const val Standby = "standby"
    const val Settings = "settings"
}

@Composable
fun FoldStandNavGraph(
    viewModel: FoldStandViewModel,
    onStartDualScreen: () -> Unit,
    onStopDualScreen: () -> Unit,
    calendarPermissionGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onInstallUpdate: (java.io.File) -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    if (!uiState.isLoaded) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        return
    }

    val navController = rememberNavController()
    val initialRoute = if (uiState.isRunning) Routes.Standby else Routes.Home
    NavHost(navController = navController, startDestination = initialRoute) {
        composable(Routes.Home) {
            HomeScreen(
                uiState = uiState,
                updateState = updateState,
                onStartStandby = {
                    viewModel.startStandby()
                    // When the device exposes the public dual-screen capability, prefer it
                    // for the primary action. The controller falls back to the inner split
                    // layout if the system rejects the session.
                    if (uiState.dualScreenStatus == com.blossom.foldstand.domain.DualScreenStatus.Available) {
                        onStartDualScreen()
                    }
                    navController.navigate(Routes.Standby) { launchSingleTop = true }
                },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onStartDualScreen = {
                    viewModel.startStandby()
                    onStartDualScreen()
                    navController.navigate(Routes.Standby) { launchSingleTop = true }
                },
                onDismissManualNotice = viewModel::dismissManualStartNotice,
                onDownloadUpdate = viewModel::downloadUpdate,
                onInstallUpdate = onInstallUpdate,
            )
        }
        composable(Routes.Standby) {
            StandbyScreen(
                uiState = uiState,
                onPageChange = viewModel::movePage,
                onCycleAmbientPreset = viewModel::cycleAmbientPreset,
                onSettingsChange = viewModel::updateSettings,
                onOpenSettings = { navController.navigate(Routes.Settings) },
                calendarPermissionGranted = calendarPermissionGranted,
                onRequestCalendarPermission = onRequestCalendarPermission,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onExit = {
                    onStopDualScreen()
                    viewModel.stopStandby()
                    navController.navigate(Routes.Home) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                settings = uiState.settings,
                dualScreenStatus = uiState.dualScreenStatus,
                onSettingsChange = viewModel::updateSettings,
                onBack = { navController.popBackStack() },
                updateState = updateState,
                onCheckForUpdates = onCheckForUpdates,
                onDownloadUpdate = onDownloadUpdate,
                onInstallUpdate = onInstallUpdate,
            )
        }
    }
}
