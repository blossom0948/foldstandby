package com.blossom.foldstand

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blossom.foldstand.fold.DualScreenController
import com.blossom.foldstand.fold.FoldStateObserver
import com.blossom.foldstand.ui.navigation.FoldStandNavGraph
import com.blossom.foldstand.ui.standby.CoverStandbyPane
import com.blossom.foldstand.ui.theme.FoldStandTheme
import com.blossom.foldstand.viewmodel.FoldStandViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: FoldStandViewModel by viewModels {
        FoldStandViewModel.Factory(application as FoldStandApp)
    }
    private lateinit var dualScreenController: DualScreenController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dualScreenController = DualScreenController(this).also { controller ->
            controller.start(lifecycleScope)
            viewModel.observeDualScreenStatus(controller.status)
        }

        val foldObserver = FoldStateObserver(applicationContext, this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                foldObserver.posture.collect(viewModel::updateFoldPosture)
            }
        }

        setContent {
            var calendarPermissionGranted by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
                        PackageManager.PERMISSION_GRANTED,
                )
            }
            val calendarPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted -> calendarPermissionGranted = granted }
            val alarmRingtoneLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    viewModel.updateSettings { it.copy(alarmRingtoneUri = uri?.toString()) }
                }
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }
            FoldStandTheme {
                FoldStandNavGraph(
                    viewModel = viewModel,
                    onStartDualScreen = {
                        dualScreenController.requestPresentation {
                            val coverState by viewModel.dualCoverUiState.collectAsState()
                            FoldStandTheme {
                                Box(Modifier.fillMaxSize().background(Color.Black)) {
                                    CoverStandbyPane(
                                        settings = coverState.settings,
                                        battery = coverState.battery,
                                        calendarPermissionGranted = calendarPermissionGranted,
                                    )
                                }
                            }
                        }
                    },
                    onStopDualScreen = dualScreenController::close,
                    calendarPermissionGranted = calendarPermissionGranted,
                    onRequestCalendarPermission = { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    onOpenNotificationSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onPickAlarmRingtone = {
                        alarmRingtoneLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "FoldStand 알람 소리")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    viewModel.uiState.value.settings.alarmRingtoneUri?.let(android.net.Uri::parse),
                                )
                            },
                        )
                    },
                    onRequestAlarmNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onInstallUpdate = { file -> (application as FoldStandApp).appUpdateRepository.install(this, file) },
                    onCheckForUpdates = viewModel::checkForUpdates,
                    onDownloadUpdate = viewModel::downloadUpdate,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as FoldStandApp).appUpdateRepository.resumePendingInstall(this)
    }
}
