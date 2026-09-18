package com.blossom.foldstand.ui.settings

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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blossom.foldstand.domain.AmbientPreset
import com.blossom.foldstand.domain.AutoDimOption
import com.blossom.foldstand.domain.ClockStyle
import com.blossom.foldstand.domain.DualScreenStatus
import com.blossom.foldstand.domain.StandbySettings
import com.blossom.foldstand.domain.readableName
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: StandbySettings,
    dualScreenStatus: DualScreenStatus,
    onSettingsChange: ((StandbySettings) -> StandbySettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
                    ChoiceRow(
                        values = AmbientPreset.entries,
                        selected = settings.ambientPreset,
                        label = { it.label },
                        onSelected = { preset -> onSettingsChange { it.copy(ambientPreset = preset) } },
                    )
                    Text("사용자 지정 색상", style = MaterialTheme.typography.titleSmall)
                    settings.customColors.take(3).forEachIndexed { index, selectedColor ->
                        ColorPickerRow(
                            index = index,
                            selectedColor = selectedColor,
                            onColorSelected = { color ->
                                onSettingsChange { current ->
                                    val next = current.customColors.toMutableList()
                                    while (next.size < 3) next += COLOR_PALETTE[next.size]
                                    next[index] = color
                                    current.copy(customColors = next)
                                }
                            },
                        )
                    }
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
                    SwitchRow("번인 방지", "시계 위치를 주기적으로 미세 이동", settings.burnInProtection) {
                        onSettingsChange { old -> old.copy(burnInProtection = it) }
                    }
                    SwitchRow("세로 접힘 좌우 반전", "세로 힌지에서 시계를 오른쪽에 표시", settings.reverseVerticalPanes) {
                        onSettingsChange { old -> old.copy(reverseVerticalPanes = it) }
                    }
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
private fun ColorPickerRow(index: Int, selectedColor: Long, onColorSelected: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("색상 ${index + 1}", modifier = Modifier.width(62.dp), style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            COLOR_PALETTE.forEach { value ->
                val selected = value == selectedColor
                Surface(
                    onClick = { onColorSelected(value) },
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(value),
                    border = if (selected) BorderStroke(3.dp, Color.White) else BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                ) {}
            }
        }
    }
}

private val COLOR_PALETTE = listOf(
    0xFF355C7DL,
    0xFF6C5B7BL,
    0xFFC06C84L,
    0xFF2F6F64L,
    0xFF617A55L,
    0xFF355070L,
    0xFF774360L,
    0xFFB56576L,
    0xFF8A5A44L,
)
