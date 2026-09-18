package com.blossom.foldstand.ui.standby

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.blossom.foldstand.domain.DualScreenStatus
import com.blossom.foldstand.domain.FoldPosture
import com.blossom.foldstand.domain.StandbySettings
import com.blossom.foldstand.domain.StandbyPage
import com.blossom.foldstand.domain.StandbyUiState
import com.blossom.foldstand.domain.readableName
import com.blossom.foldstand.fold.FoldLayoutCalculator
import java.time.LocalTime
import kotlin.math.abs
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandbyScreen(
    uiState: StandbyUiState,
    onPageChange: (Int) -> Unit,
    onCycleAmbientPreset: (Int) -> Unit,
    onSettingsChange: ((StandbySettings) -> StandbySettings) -> Unit,
    onOpenSettings: () -> Unit,
    calendarPermissionGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var controlsVisible by remember { mutableStateOf(false) }
    var settingsSheetVisible by remember { mutableStateOf(false) }
    var exitConfirmationVisible by remember { mutableStateOf(false) }
    var interactionNonce by remember { mutableLongStateOf(0L) }
    var isDimmed by remember { mutableStateOf(false) }
    var dragTotal by remember { mutableStateOf(Offset.Zero) }
    val isNight = uiState.settings.nightMode && LocalTime.now().hour >= 22 ||
        uiState.settings.nightMode && LocalTime.now().hour < 7

    fun recordInteraction() {
        interactionNonce++
        isDimmed = false
    }

    LaunchedEffect(interactionNonce, uiState.settings.autoDim) {
        val delayMillis = uiState.settings.autoDim.delayMillis ?: return@LaunchedEffect
        delay(delayMillis)
        isDimmed = true
    }

    val brightnessFactor by animateFloatAsState(
        targetValue = if (isDimmed) 0.35f else 1f,
        animationSpec = tween(1_400),
        label = "automatic brightness dimming",
    )
    StandbyWindowEffects(
        brightness = uiState.settings.brightness * brightnessFactor,
        keepScreenOn = uiState.settings.keepScreenOn,
    )

    val burnInOffset = rememberBurnInOffset(uiState.settings.burnInProtection)
    val gestureModifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    recordInteraction()
                    controlsVisible = !controlsVisible
                },
                onLongPress = {
                    recordInteraction()
                    settingsSheetVisible = true
                },
            )
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    dragTotal = Offset.Zero
                    recordInteraction()
                },
                onDrag = { change, amount ->
                    change.consume()
                    dragTotal += amount
                },
                onDragEnd = {
                    val threshold = 64.dp.toPx()
                    when {
                        abs(dragTotal.x) > abs(dragTotal.y) && abs(dragTotal.x) > threshold ->
                            onPageChange(if (dragTotal.x < 0) 1 else -1)
                        abs(dragTotal.y) > threshold ->
                            onCycleAmbientPreset(if (dragTotal.y < 0) 1 else -1)
                    }
                    dragTotal = Offset.Zero
                },
                onDragCancel = { dragTotal = Offset.Zero },
            )
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(gestureModifier),
    ) {
        Crossfade(
            targetState = uiState.foldPosture to uiState.settings.reverseVerticalPanes,
            animationSpec = tween(320),
            label = "fold posture transition",
            modifier = Modifier.fillMaxSize(),
        ) { (posture, reverse) ->
            if (uiState.page != StandbyPage.Clock) {
                when (uiState.page) {
                    StandbyPage.Widgets -> StandbyWidgetsPane(
                        calendarPermissionGranted = calendarPermissionGranted,
                        onRequestCalendarPermission = onRequestCalendarPermission,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                    )
                    StandbyPage.Calendar -> CalendarPage(
                        permissionGranted = calendarPermissionGranted,
                        onRequestPermission = onRequestCalendarPermission,
                    )
                    StandbyPage.Notifications -> NotificationsPage(
                        onOpenNotificationSettings = onOpenNotificationSettings,
                    )
                    StandbyPage.Clock -> Unit
                }
            } else if (uiState.dualScreenStatus == DualScreenStatus.Active) {
                AmbientPane(
                    preset = uiState.settings.ambientPreset,
                    colorValues = uiState.settings.customColors,
                    powerSaving = uiState.settings.powerSavingAnimation,
                )
            } else {
                FoldAwareStandbyLayout(
                    posture = posture,
                    reverseVerticalPanes = reverse,
                    clock = {
                        ClockPane(
                            settings = uiState.settings,
                            battery = uiState.battery,
                            burnInOffset = burnInOffset,
                        )
                    },
                    ambient = {
                        AmbientPane(
                            preset = uiState.settings.ambientPreset,
                            colorValues = uiState.settings.customColors,
                            powerSaving = uiState.settings.powerSavingAnimation,
                        )
                    },
                )
            }
        }

        if (isNight) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0x2BFF3030)),
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
                color = Color(0xCC141414),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = if (uiState.dualScreenStatus == DualScreenStatus.Active) {
                        "듀얼 화면 · ${uiState.settings.ambientPreset.label}"
                    } else {
                        "${uiState.foldPosture.readableName()} · ${uiState.page.label} · ${uiState.settings.ambientPreset.label}"
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        PageIndicator(page = uiState.page, modifier = Modifier.align(Alignment.BottomCenter))

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            StandbyControls(
                uiState = uiState,
                onBrightness = { value -> onSettingsChange { it.copy(brightness = value) } },
                onOpenSettings = onOpenSettings,
                onExit = { exitConfirmationVisible = true },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }

    if (settingsSheetVisible) {
        ModalBottomSheet(onDismissRequest = { settingsSheetVisible = false }) {
            QuickSettingsSheet(
                uiState = uiState,
                onSettingsChange = onSettingsChange,
                onExit = {
                    settingsSheetVisible = false
                    exitConfirmationVisible = true
                },
            )
        }
    }

    if (exitConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { exitConfirmationVisible = false },
            icon = { Icon(Icons.Default.FullscreenExit, contentDescription = null) },
            title = { Text("스탠바이를 종료할까요?") },
            text = { Text("설정한 시계, 색상과 밝기는 다음 실행에도 유지됩니다.") },
            confirmButton = {
                Button(onClick = onExit) { Text("종료") }
            },
            dismissButton = {
                TextButton(onClick = { exitConfirmationVisible = false }) { Text("계속 사용") }
            },
        )
    }

    BackHandler {
        recordInteraction()
        if (!controlsVisible) controlsVisible = true else exitConfirmationVisible = true
    }
}

@Composable
private fun StandbyControls(
    uiState: StandbyUiState,
    onBrightness: (Float) -> Unit,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(16.dp).fillMaxWidth().widthIn(max = 620.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE6141414)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            if (uiState.battery.percent != null && uiState.battery.percent <= 15 && !uiState.battery.isCharging) {
                Text(
                    "배터리 ${uiState.battery.percent}% · 밝기를 낮추면 더 오래 사용할 수 있어요.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Brightness6, contentDescription = "밝기")
                Slider(
                    value = uiState.settings.brightness,
                    onValueChange = onBrightness,
                    valueRange = 0.05f..1f,
                    steps = 18,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                )
                Text("${(uiState.settings.brightness * 100).toInt()}%")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                FilledTonalButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Text("설정", modifier = Modifier.padding(start = 6.dp))
                }
                FilledTonalButton(onClick = onExit) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Text("종료", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsSheet(
    uiState: StandbyUiState,
    onSettingsChange: ((StandbySettings) -> StandbySettings) -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 12.dp)) {
                Text("빠른 설정", style = MaterialTheme.typography.titleLarge)
                Text(
                    "좌우 스와이프: 시계 · 위아래 스와이프: 무드등",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text("밝기 ${(uiState.settings.brightness * 100).toInt()}%")
        Slider(
            value = uiState.settings.brightness,
            onValueChange = { value -> onSettingsChange { it.copy(brightness = value) } },
            valueRange = 0.05f..1f,
            steps = 18,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.settings.customColors.take(3).forEachIndexed { index, value ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(value), CircleShape)
                        .semantics { contentDescription = "무드등 색상 ${index + 1}" },
                )
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onExit) {
                Icon(Icons.Default.FullscreenExit, contentDescription = null)
                Text("스탠바이 종료", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun PageIndicator(page: StandbyPage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        StandbyPage.entries.forEach { item ->
            Box(
                Modifier
                    .size(if (item == page) 7.dp else 5.dp)
                    .background(
                        if (item == page) Color.White.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.28f),
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun FoldAwareStandbyLayout(
    posture: FoldPosture,
    reverseVerticalPanes: Boolean,
    clock: @Composable () -> Unit,
    ambient: @Composable () -> Unit,
) {
    Layout(
        content = {
            Box(Modifier.fillMaxSize().clipToBounds()) { clock() }
            Box(Modifier.fillMaxSize().clipToBounds()) { ambient() }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth.coerceAtLeast(1)
        val height = constraints.maxHeight.coerceAtLeast(1)
        val arrangement = FoldLayoutCalculator.calculate(width, height, posture, reverseVerticalPanes)
        val clockRect = arrangement.clock
        val ambientRect = arrangement.ambient
        val clockPlaceable = measurables[0].measure(Constraints.fixed(clockRect.width, clockRect.height))
        val ambientPlaceable = measurables[1].measure(Constraints.fixed(ambientRect.width, ambientRect.height))
        layout(width, height) {
            clockPlaceable.place(clockRect.left, clockRect.top)
            ambientPlaceable.place(ambientRect.left, ambientRect.top)
        }
    }
}

@Composable
private fun rememberBurnInOffset(enabled: Boolean): Offset {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var index by remember(enabled) { mutableLongStateOf(0L) }
    LaunchedEffect(enabled, lifecycle) {
        if (!enabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(90_000L)
                index++
            }
        }
    }
    if (!enabled) return Offset.Zero
    val offsets = remember {
        listOf(
            Offset(-4f, -2f),
            Offset(3f, -4f),
            Offset(4f, 3f),
            Offset(-2f, 4f),
            Offset.Zero,
        )
    }
    return offsets[(index % offsets.size).toInt()]
}

@Composable
private fun StandbyWindowEffects(brightness: Float, keepScreenOn: Boolean) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return
    val window = activity.window

    DisposableEffect(window, view) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    DisposableEffect(window, keepScreenOn) {
        if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (keepScreenOn) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    SideEffect {
        window.attributes = window.attributes.apply {
            screenBrightness = brightness.coerceIn(0.05f, 1f)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
