package com.blossom.foldstand.ui.settings

import android.app.TimePickerDialog
import android.graphics.Color as AndroidColor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blossom.foldstand.BuildConfig
import com.blossom.foldstand.data.UpdateState
import com.blossom.foldstand.domain.AmbientPreset
import com.blossom.foldstand.domain.AutoDimOption
import com.blossom.foldstand.domain.ClockStyle
import com.blossom.foldstand.domain.DEFAULT_AMBIENT_COLORS
import com.blossom.foldstand.domain.DualScreenStatus
import com.blossom.foldstand.domain.StandbySettings
import com.blossom.foldstand.domain.NightModeOption
import com.blossom.foldstand.domain.readableName
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: StandbySettings,
    dualScreenStatus: DualScreenStatus,
    onSettingsChange: ((StandbySettings) -> StandbySettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    updateState: UpdateState = UpdateState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
    onInstallUpdate: (File) -> Unit = {},
    onPickAlarmRingtone: () -> Unit = {},
    onRequestAlarmNotificationPermission: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("스타일 설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                SettingSection("시계", Icons.Default.Schedule) {
                    ChoiceRow(
                        values = ClockStyle.entries,
                        selected = settings.clockStyle,
                        label = { it.label },
                        onSelected = { style -> onSettingsChange { it.copy(clockStyle = style) } },
                    )
                    SwitchRow("24시간제", "끄면 오전/오후 형식으로 표시", settings.use24Hour) {
                        onSettingsChange { old -> old.copy(use24Hour = it) }
                    }
                    SwitchRow("초 표시", "켜진 경우에만 초 단위로 시간을 갱신", settings.showSeconds) {
                        onSettingsChange { old -> old.copy(showSeconds = it) }
                    }
                    SwitchRow("날짜 표시", null, settings.showDate) {
                        onSettingsChange { old -> old.copy(showDate = it) }
                    }
                    SwitchRow("배터리 표시", null, settings.showBattery) {
                        onSettingsChange { old -> old.copy(showBattery = it) }
                    }
                }
            }

            item {
                SettingSection("무드등", Icons.Default.ColorLens) {
                    Text(
                        "빠른 모드",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChoiceRow(
                        values = AmbientPreset.entries,
                        selected = settings.ambientPreset,
                        label = { it.label },
                        onSelected = { preset -> onSettingsChange { it.copy(ambientPreset = preset) } },
                    )
                    Text("자유 색상 프로필", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "색상 원을 선택한 뒤 색조·채도·밝기를 조절하세요. 최대 6개의 색이 그라데이션과 오로라에 사용됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    settings.customColors.take(6).forEachIndexed { index, selectedColor ->
                        FreeColorPickerRow(
                            index = index,
                            selectedColor = selectedColor,
                            onColorSelected = { color ->
                                onSettingsChange { current ->
                                    val next = current.customColors.toMutableList()
                                    while (next.size <= index) {
                                        next += DEFAULT_AMBIENT_COLORS[next.size.coerceAtMost(DEFAULT_AMBIENT_COLORS.lastIndex)]
                                    }
                                    next[index] = color
                                    current.copy(customColors = next)
                                }
                            },
                        )
                    }
                    Text("대표 색상", style = MaterialTheme.typography.titleSmall)
                    ChoiceRow(
                        values = settings.customColors.indices.toList(),
                        selected = settings.ambientColorIndex,
                        label = { "${it + 1}" },
                        onSelected = { index -> onSettingsChange { it.copy(ambientColorIndex = index) } },
                    )
                }
            }

            item {
                SettingSection("화면", Icons.Default.Brightness6) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("앱 내부 밝기", fontWeight = FontWeight.Medium)
                            Text(
                                "StandBy 화면에만 적용",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${(settings.brightness * 100).roundToInt()}%")
                    }
                    Slider(
                        value = settings.brightness,
                        onValueChange = { value -> onSettingsChange { it.copy(brightness = value) } },
                        valueRange = 0.05f..1f,
                        steps = 18,
                        modifier = Modifier.semantics { contentDescription = "앱 내부 밝기" },
                    )
                    SwitchRow("화면 계속 켜기", "StandBy가 전경일 때만 적용", settings.keepScreenOn) {
                        onSettingsChange { old -> old.copy(keepScreenOn = it) }
                    }
                    Text("자동 어둡게", style = MaterialTheme.typography.titleSmall)
                    ChoiceRow(
                        values = AutoDimOption.entries,
                        selected = settings.autoDim,
                        label = { it.label },
                        onSelected = { value -> onSettingsChange { it.copy(autoDim = value) } },
                    )
                    SwitchRow("절전 애니메이션", "15fps 이하로 부드럽게 움직임", settings.powerSavingAnimation) {
                        onSettingsChange { old -> old.copy(powerSavingAnimation = it) }
                    }
                    Text("야간 모드", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "켬은 항상 붉은 톤과 저밝기, 자동은 조도센서가 어두움을 감지했을 때만 적용합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChoiceRow(
                        values = NightModeOption.entries,
                        selected = settings.nightMode,
                        label = { it.label },
                        onSelected = { option -> onSettingsChange { it.copy(nightMode = option) } },
                    )
                    SwitchRow("번인 방지", "시계 위치를 주기적으로 미세 이동", settings.burnInProtection) {
                        onSettingsChange { old -> old.copy(burnInProtection = it) }
                    }
                    SwitchRow("세로 접힘 좌우 반전", "세로 힌지에서 시계를 오른쪽에 표시", settings.reverseVerticalPanes) {
                        onSettingsChange { old -> old.copy(reverseVerticalPanes = it) }
                    }
                    SwitchRow(
                        "커버 화면 정보 모드",
                        "듀얼 화면에서 내부 무드등 대신 시계·일정·알림 요약을 표시",
                        settings.coverOnlyMode,
                    ) {
                        onSettingsChange { old -> old.copy(coverOnlyMode = it) }
                    }
                }
            }

            item {
                SettingSection("알람", Icons.Default.Alarm) {
                    val context = LocalContext.current
                    SwitchRow(
                        "FoldStand 알람",
                        "매일 같은 시각에 기기 알림 소리로 울립니다.",
                        settings.alarmEnabled,
                    ) {
                        onSettingsChange { old -> old.copy(alarmEnabled = it) }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("알람 시각", fontWeight = FontWeight.Medium)
                            Text(
                                "%02d:%02d".format(settings.alarmHour, settings.alarmMinute),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        onSettingsChange { old -> old.copy(alarmHour = hour, alarmMinute = minute) }
                                    },
                                    settings.alarmHour,
                                    settings.alarmMinute,
                                    settings.use24Hour,
                                ).show()
                            },
                        ) { Text("시간 선택") }
                    }
                    OutlinedButton(onClick = onPickAlarmRingtone, modifier = Modifier.fillMaxWidth()) {
                        Text(if (settings.alarmRingtoneUri == null) "기본 알람 소리" else "알람 소리 선택됨")
                    }
                    OutlinedButton(onClick = onRequestAlarmNotificationPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("알람 알림 권한 허용")
                    }
                    Text(
                        "알림 권한과 시스템 알림 소리를 사용합니다. 배터리 절전 정책에 따라 제조사가 알람을 제한할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingSection("시작 방식", Icons.Default.Tune) {
                    Text(
                        "수동 실행은 항상 사용할 수 있으며 충전 여부로 차단되지 않습니다.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    SwitchRow("충전 시작 시 제안", "앱 안에서만 시작을 제안 · 기본 꺼짐", settings.suggestWhenCharging) {
                        onSettingsChange { old -> old.copy(suggestWhenCharging = it) }
                    }
                    SwitchRow("반접힘 감지 시 제안", "앱 안에서만 시작을 제안 · 기본 꺼짐", settings.suggestWhenHalfOpened) {
                        onSettingsChange { old -> old.copy(suggestWhenHalfOpened = it) }
                    }
                }
            }

            item {
                SettingSection("실험실", Icons.Default.Science) {
                    Text("듀얼 화면 capability", fontWeight = FontWeight.Medium)
                    Text(
                        dualScreenStatus.readableName(),
                        color = if (dualScreenStatus == DualScreenStatus.Available) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        "기기 모델명이 아니라 WindowAreaController의 실시간 결과만 사용합니다. 미지원 기기에서는 내부 화면 분할 모드가 유지됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SettingSection("앱 업데이트", Icons.Default.SystemUpdate) {
                    Text(
                        "현재 버전 ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    UpdateSettingsContent(
                        state = updateState,
                        onCheck = onCheckForUpdates,
                        onDownload = onDownloadUpdate,
                        onInstall = onInstallUpdate,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateSettingsContent(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit,
) {
    Text(
        "업데이트 확인을 누르면 앱 안에서 APK를 검증·내려받고 Android 설치 화면으로 이어집니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    when (state) {
        UpdateState.Idle -> Button(onClick = onCheck) { Text("업데이트 확인") }
        UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("새 버전을 확인하고 있어요…", modifier = Modifier.padding(start = 10.dp))
        }
        UpdateState.UpToDate -> {
            Text("현재 최신 버전이에요.", color = MaterialTheme.colorScheme.primary)
            Button(onClick = onCheck) { Text("다시 확인") }
        }
        is UpdateState.Available -> {
            Text("${state.info.versionName} 업데이트가 있어요.", color = MaterialTheme.colorScheme.primary)
            Button(onClick = onDownload) { Text("앱에서 다운로드") }
        }
        is UpdateState.Downloading -> {
            Text("앱 안에서 APK 다운로드 중 · ${state.progress}%")
            LinearProgressIndicator(
                progress = { state.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is UpdateState.Ready -> {
            Text("다운로드가 완료되었습니다. 설치를 누르면 다음 단계로 진행합니다.")
            Button(onClick = { onInstall(state.file) }) { Text("앱에서 설치") }
        }
        is UpdateState.WaitingForInstallPermission -> {
            Text("FoldStand의 ‘알 수 없는 앱 설치’를 허용하고 돌아온 뒤 다시 설치를 누르세요.")
            Button(onClick = { onInstall(state.file) }) { Text("설치 다시 시도") }
        }
        is UpdateState.Installing -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Android 설치 세션을 준비하고 있어요…", modifier = Modifier.padding(start = 10.dp))
            }
        }
        is UpdateState.InstallerOpened -> {
            Text("Android 설치 화면에서 업데이트를 승인해 주세요.")
            Button(onClick = { onInstall(state.file) }) { Text("설치 화면 다시 열기") }
        }
        is UpdateState.Error -> {
            Text(state.message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onCheck) { Text("다시 시도") }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    title,
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceRow(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.forEach { value ->
            val isSelected = value == selected
            Surface(
                modifier = Modifier
                    .height(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .clickable { onSelected(value) },
                shape = RoundedCornerShape(15.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f),
                ),
            ) {
                Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(label(value), color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified)
                }
            }
        }
    }
}

@Composable
private fun FreeColorPickerRow(index: Int, selectedColor: Long, onColorSelected: (Long) -> Unit) {
    val hsv = remember(selectedColor) {
        FloatArray(3).also { values ->
            AndroidColor.colorToHSV((selectedColor and 0xFFFF_FFFFL).toInt(), values)
        }
    }
    var hue by remember(selectedColor) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(selectedColor) { mutableFloatStateOf(hsv[1]) }
    var value by remember(selectedColor) { mutableFloatStateOf(hsv[2]) }

    fun updateColor(nextHue: Float = hue, nextSaturation: Float = saturation, nextValue: Float = value) {
        val argb = AndroidColor.HSVToColor(floatArrayOf(nextHue, nextSaturation, nextValue))
        onColorSelected(argb.toLong() and 0xFFFF_FFFFL)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Color(selectedColor),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.72f)),
            ) {}
            Text("색상 ${index + 1}", modifier = Modifier.padding(start = 10.dp), fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(
                "#%08X".format(selectedColor),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                updateColor(nextHue = it)
            },
            valueRange = 0f..360f,
            steps = 35,
            modifier = Modifier.semantics { contentDescription = "색상 ${index + 1} 색조" },
        )
        Slider(
            value = saturation,
            onValueChange = {
                saturation = it
                updateColor(nextSaturation = it)
            },
            valueRange = 0f..1f,
            steps = 19,
            modifier = Modifier.semantics { contentDescription = "색상 ${index + 1} 채도" },
        )
        Slider(
            value = value,
            onValueChange = {
                value = it
                updateColor(nextValue = it)
            },
            valueRange = 0.05f..1f,
            steps = 19,
            modifier = Modifier.semantics { contentDescription = "색상 ${index + 1} 밝기" },
        )
    }
}
