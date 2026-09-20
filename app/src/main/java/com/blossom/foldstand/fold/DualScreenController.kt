package com.blossom.foldstand.fold

import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.core.ExperimentalWindowApi
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import com.blossom.foldstand.domain.DualScreenStatus
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
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
    private var requestInFlight = false
    private var closeRequested = false

    fun start(scope: CoroutineScope) {
        val activeController = controller
        if (activeController == null) {
            _status.value = DualScreenStatus.Unsupported
            return
        }
        scope.launch(Dispatchers.Main.immediate) {
            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                activeController.windowAreaInfos
                    .map { infos -> infos.presentableRearArea() }
                    .distinctUntilChanged()
                    .catch { error ->
                        areaInfo = null
                        _status.value = DualScreenStatus.Error(error.message ?: "capability 확인 실패")
                    }
                    .collectLatest { info ->
                        areaInfo = info
                        // Foldable OEMs often publish transient unavailable/active
                        // snapshots while the hinge settles. Debouncing here keeps
                        // the home action stable instead of flickering on every frame.
                        delay(CAPABILITY_SETTLE_MILLIS)
                        if (session == null && !requestInFlight) {
                            _status.value = info?.getCapability(PRESENT_OPERATION)?.status.toDomainStatus()
                        }
                    }
                }
            }
    }

    fun requestPresentation(content: @Composable () -> Unit) {
        val activeController = controller
        val info = areaInfo
        if (session != null || requestInFlight) return
        if (activeController == null || info == null || _status.value != DualScreenStatus.Available) {
            if (_status.value !is DualScreenStatus.Error) _status.value = DualScreenStatus.Unavailable
            return
        }
        closeRequested = false
        requestInFlight = true
        presentationContent = content
        runCatching {
            activeController.presentContentOnWindowArea(
                token = info.token,
                activity = activity,
                executor = executor,
                windowAreaPresentationSessionCallback = this,
            )
        }.onFailure { error ->
            requestInFlight = false
            presentationContent = null
            _status.value = DualScreenStatus.Error(error.message ?: "보조 화면 시작 실패")
        }
    }

    fun close() {
        closeRequested = session != null || requestInFlight
        requestInFlight = false
        runCatching { session?.close() }
        clearSession()
        refreshCapabilityStatus()
        if (!closeRequested) closeRequested = false
    }

    override fun onSessionStarted(session: WindowAreaSessionPresenter) {
        requestInFlight = false
        val content = presentationContent
        if (content == null || closeRequested) {
            runCatching { session.close() }
            closeRequested = false
            return
        }
        this.session = session
        val view = ComposeView(session.context)
        runCatching {
            installViewTreeOwners(view)
            view.setContent { content() }
            session.setContentView(view)
            view
        }.onFailure { error ->
            Log.e(TAG, "Unable to attach dual-screen content", error)
            runCatching { session.close() }
            clearSession()
            _status.value = DualScreenStatus.Error(error.message ?: "보조 화면 콘텐츠 연결 실패")
        }.onSuccess {
            presentationView = view
            _status.value = DualScreenStatus.Active
        }
    }

    override fun onSessionEnded(t: Throwable?) {
        if (t != null) Log.e(TAG, "Dual-screen session ended with an error", t)
        requestInFlight = false
        val wasCloseRequested = closeRequested
        closeRequested = false
        clearSession()
        _status.value = if (t == null || wasCloseRequested) capabilityStatus() else {
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

    /**
     * The WindowArea presenter owns a separate view tree. Some OEM builds do not
     * install the lifecycle/saved-state owners that Compose's window recomposer
     * requires, so copy the activity owners onto the presenter root when the
     * public setter classes are available. Reflection keeps this compatible with
     * the different AndroidX packaging used by older devices.
     */
    private fun installViewTreeOwners(view: View) {
        runCatching {
            val lifecycleOwnerType = Class.forName("androidx.lifecycle.LifecycleOwner")
            Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner")
                .getMethod("set", View::class.java, lifecycleOwnerType)
                .invoke(null, view, activity)
        }.onFailure { error ->
            Log.w(TAG, "Presenter lifecycle owner is unavailable", error)
        }
        runCatching {
            val savedStateOwnerType = Class.forName("androidx.savedstate.SavedStateRegistryOwner")
            Class.forName("androidx.savedstate.ViewTreeSavedStateRegistryOwner")
                .getMethod("set", View::class.java, savedStateOwnerType)
                .invoke(null, view, activity)
        }.onFailure { error ->
            Log.w(TAG, "Presenter saved-state owner is unavailable", error)
        }
    }

    private fun capabilityStatus(): DualScreenStatus {
        val status = areaInfo?.getCapability(PRESENT_OPERATION)?.status.toDomainStatus()
        // WindowArea can keep reporting ACTIVE for a short period after the
        // presenter callback ended. With no local session, it is requestable again.
        return if (session == null && status == DualScreenStatus.Active) {
            DualScreenStatus.Available
        } else {
            status
        }
    }

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
        const val CAPABILITY_SETTLE_MILLIS = 240L
        val PRESENT_OPERATION = WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA
    }
}
