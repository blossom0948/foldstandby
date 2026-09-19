package com.blossom.foldstand.fold

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.window.core.ExperimentalWindowApi
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import com.blossom.foldstand.domain.DualScreenStatus
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalWindowApi::class)
class DualScreenController(
    private val activity: ComponentActivity,
) : WindowAreaPresentationSessionCallback {
    private val executor: Executor = ContextCompat.getMainExecutor(activity)
    private val controller = runCatching { WindowAreaController.getOrCreate() }.getOrNull()
    private val _status = MutableStateFlow<DualScreenStatus>(DualScreenStatus.Checking)
    val status: StateFlow<DualScreenStatus> = _status.asStateFlow()

    private var areaInfo: WindowAreaInfo? = null
    private var session: WindowAreaSessionPresenter? = null
    private var presentationView: ComposeView? = null
    private var presentationContent: (@Composable () -> Unit)? = null

    fun start(scope: CoroutineScope) {
        val activeController = controller
        if (activeController == null) {
            _status.value = DualScreenStatus.Unsupported
            return
        }
        scope.launch {
            activeController.windowAreaInfos
                .map { infos -> infos.presentableRearArea() }
                .distinctUntilChanged()
                .catch { error ->
                    areaInfo = null
                    _status.value = DualScreenStatus.Error(error.message ?: "capability 확인 실패")
                }
                .collect { info ->
                    areaInfo = info
                    _status.value = info?.getCapability(PRESENT_OPERATION)?.status.toDomainStatus()
                }
        }
    }

    fun requestPresentation(content: @Composable () -> Unit) {
        val activeController = controller
        val info = areaInfo
        if (activeController == null || info == null || _status.value != DualScreenStatus.Available) {
            if (_status.value !is DualScreenStatus.Error) _status.value = DualScreenStatus.Unavailable
            return
        }
        presentationContent = content
        runCatching {
            activeController.presentContentOnWindowArea(
                token = info.token,
                activity = activity,
                executor = executor,
                windowAreaPresentationSessionCallback = this,
            )
        }.onFailure { error ->
            presentationContent = null
            _status.value = DualScreenStatus.Error(error.message ?: "보조 화면 시작 실패")
        }
    }

    fun close() {
        session?.close()
        clearSession()
        refreshCapabilityStatus()
    }

    override fun onSessionStarted(session: WindowAreaSessionPresenter) {
        this.session = session
        val view = ComposeView(session.context).apply {
            setContent { presentationContent?.invoke() }
        }
        presentationView = view
        session.setContentView(view)
        _status.value = DualScreenStatus.Active
    }

    override fun onSessionEnded(t: Throwable?) {
        if (t != null) Log.e(TAG, "Dual-screen session ended with an error", t)
        clearSession()
        _status.value = if (t == null) capabilityStatus() else {
            DualScreenStatus.Error(t.message ?: "보조 화면 세션 종료")
        }
    }

    override fun onContainerVisibilityChanged(isVisible: Boolean) {
        Log.d(TAG, "Dual-screen container visible=$isVisible")
    }

    private fun clearSession() {
        presentationView?.disposeComposition()
        presentationView = null
        presentationContent = null
        session = null
    }

    private fun refreshCapabilityStatus() {
        _status.value = capabilityStatus()
    }

    private fun capabilityStatus(): DualScreenStatus =
        areaInfo?.getCapability(PRESENT_OPERATION)?.status.toDomainStatus()

    /**
     * Some implementations expose more than one rear-facing area while a fold is moving.
     * Do not let an unsupported placeholder hide an area that is actually presentable.
     */
    private fun List<WindowAreaInfo>.presentableRearArea(): WindowAreaInfo? {
        val rearAreas = filter { it.type == WindowAreaInfo.Type.TYPE_REAR_FACING }
        return rearAreas.firstOrNull {
            it.getCapability(PRESENT_OPERATION)?.status ==
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE
        } ?: rearAreas.firstOrNull {
            it.getCapability(PRESENT_OPERATION)?.status ==
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE
        } ?: rearAreas.firstOrNull()
    }

    private fun WindowAreaCapability.Status?.toDomainStatus(): DualScreenStatus {
        val raw = when (this) {
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNSUPPORTED, null -> RawDualScreenStatus.Unsupported
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_UNAVAILABLE -> RawDualScreenStatus.Unavailable
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE -> RawDualScreenStatus.Available
            WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE -> RawDualScreenStatus.Active
            else -> RawDualScreenStatus.Unknown
        }
        return DualScreenStatusMapper.map(raw)
    }

    private companion object {
        const val TAG = "FoldStandDualScreen"
        val PRESENT_OPERATION = WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA
    }
}
