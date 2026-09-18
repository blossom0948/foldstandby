package com.blossom.foldstand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blossom.foldstand.fold.DualScreenController
import com.blossom.foldstand.fold.FoldStateObserver
import com.blossom.foldstand.ui.navigation.FoldStandNavGraph
import com.blossom.foldstand.ui.standby.ClockPane
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
            FoldStandTheme {
                FoldStandNavGraph(
                    viewModel = viewModel,
                    onStartDualScreen = {
                        dualScreenController.requestPresentation {
                            val uiState by viewModel.uiState.collectAsState()
                            FoldStandTheme {
                                Box(Modifier.fillMaxSize().background(Color.Black)) {
                                    ClockPane(
                                        settings = uiState.settings,
                                        battery = uiState.battery,
                                    )
                                }
                            }
                        }
                    },
                    onStopDualScreen = dualScreenController::close,
                )
            }
        }
    }

    override fun onStop() {
        if (::dualScreenController.isInitialized) dualScreenController.close()
        super.onStop()
    }
}
