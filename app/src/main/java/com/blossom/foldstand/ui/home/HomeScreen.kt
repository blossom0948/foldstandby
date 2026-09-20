package com.blossom.foldstand.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blossom.foldstand.domain.DualScreenStatus
import com.blossom.foldstand.domain.FoldPosture
import com.blossom.foldstand.domain.StandbyUiState
import com.blossom.foldstand.domain.readableName
import com.blossom.foldstand.data.UpdateState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    uiState: StandbyUiState,
    onStartStandby: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartDualScreen: () -> Unit,
    onDismissManualNotice: () -> Unit,
    modifier: Modifier = Modifier,
    updateState: UpdateState = UpdateState.Idle,
    onDownloadUpdate: () -> Unit = {},
    onInstallUpdate: (java.io.File) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "FoldStand",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "폴드를 탁자 위의 시계와 무드등으로",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            StandbyPreview(uiState, Modifier.fillMaxWidth().widthIn(max = 720.dp))
        }

        if (updateState !is UpdateState.Idle && updateState !is UpdateState.UpToDate && updateState !is UpdateState.Checking) {
            item {
                UpdateCard(
                    state = updateState,
                    onDownload = onDownloadUpdate,
                    onInstall = onInstallUpdate,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                )
            }
        }

        if (!uiState.settings.hasSeenManualStartNotice) {
            item {
                NoticeCard(
                    text = "충전 중이 아니어도 언제든 스탠바이를 시작할 수 있어요.",
                    onDismiss = onDismissManualNotice,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                )
            }
        }

        if (uiState.battery.percent != null && uiState.battery.percent <= 15 && !uiState.battery.isCharging) {
            item {
                InfoCard(
                    icon = { Icon(Icons.Default.BatteryAlert, contentDescription = null) },
                    text = "배터리가 ${uiState.battery.percent}%예요. 설정에서 밝기를 낮추면 더 오래 사용할 수 있습니다.",
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                )
            }
        }

        if (uiState.settings.suggestWhenCharging && uiState.battery.isCharging) {
            item {
                InfoCard(
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    text = "충전을 시작했어요. 스탠바이를 켜 볼까요?",
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                )
            }
        }

        if (uiState.settings.suggestWhenHalfOpened && uiState.foldPosture is FoldPosture.HalfOpened) {
            item {
                InfoCard(
                    icon = { Icon(Icons.Default.ScreenRotation, contentDescription = null) },
                    text = "반접힘 자세가 감지됐어요. 스탠바이에 적합한 상태입니다.",
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                )
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onStartStandby,
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("home_start_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = null)
                    Text("스탠바이 시작", modifier = Modifier.padding(start = 10.dp), fontSize = 18.sp)
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Text("스타일 설정", modifier = Modifier.padding(start = 10.dp))
                }
                OutlinedButton(
                    onClick = onStartDualScreen,
                    enabled = uiState.dualScreenStatus == DualScreenStatus.Available,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.Smartphone, contentDescription = null)
                    Text("듀얼 화면 실험 기능", modifier = Modifier.padding(start = 10.dp))
                }
                if (uiState.dualScreenStatus != DualScreenStatus.Checking &&
                    uiState.dualScreenStatus != DualScreenStatus.Available &&
                    uiState.dualScreenStatus != DualScreenStatus.Active
                ) {
                    OutlinedButton(
                        onClick = onStartStandby,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = null)
                        Text("내부 화면 분할로 시작", modifier = Modifier.padding(start = 10.dp))
                    }
                }
                Text(
                    text = dualScreenExplanation(uiState.dualScreenStatus),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            val batteryText = uiState.battery.percent?.let { "$it%" } ?: "확인 중"
            val statusText = if (uiState.battery.isCharging) {
                "충전 중 · 배터리 $batteryText"
            } else {
                "배터리 사용 중 · $batteryText"
            }
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (uiState.battery.isCharging) Color(0xFF79D69F) else Color(0xFFA6A6A6)),
                )
                Text(
                    statusText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun UpdateCard(
    state: UpdateState,
    onDownload: () -> Unit,
    onInstall: (java.io.File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val info = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Downloading -> state.info
        is UpdateState.Ready -> state.info
        is UpdateState.WaitingForInstallPermission -> state.info
        is UpdateState.Installing -> state.info
        is UpdateState.InstallerOpened -> state.info
        else -> null
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("새 버전${info?.let { " ${it.versionName}" } ?: ""}", style = MaterialTheme.typography.titleMedium)
            when (state) {
                is UpdateState.Available -> {
                    Text(
                        "FoldStand 앱 안에서 업데이트 파일을 검증·내려받아 설치할 수 있습니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onDownload) { Text("업데이트 다운로드") }
                }
                is UpdateState.Downloading -> {
                    Text("다운로드 중 ${state.progress}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is UpdateState.Ready -> {
                    Text(
                        "다운로드가 완료되었습니다. GitHub 브라우저를 열지 않고 Android 설치 화면으로 전달합니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { onInstall(state.file) }) {
                        Text("업데이트 설치")
                    }
                }
                is UpdateState.WaitingForInstallPermission -> {
                    Text("FoldStand의 ‘알 수 없는 앱 설치’를 허용하고 돌아온 뒤 다시 설치를 누르세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onInstall(state.file) }) { Text("설치 다시 시도") }
                }
                is UpdateState.Installing -> {
                    Text("Android 설치 세션을 준비하고 있어요…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is UpdateState.InstallerOpened -> {
                    Text("Android 설치 화면에서 업데이트를 승인해 주세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onInstall(state.file) }) { Text("설치 화면 다시 열기") }
                }
                is UpdateState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
}

@Composable
private fun StandbyPreview(uiState: StandbyUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(260.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(0.54f).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = LocalTime.now().format(DateTimeFormatter.ofPattern(if (uiState.settings.use24Hour) "HH:mm" else "a h:mm")),
                    fontSize = if (uiState.settings.clockStyle.name == "Minimal") 42.sp else 50.sp,
                    fontWeight = if (uiState.settings.clockStyle.name == "Minimal") FontWeight.Light else FontWeight.Black,
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.09f),
                ) {
                    Text(
                        uiState.foldPosture.readableName(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Surface(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxSize()
                    .padding(10.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1C1C1E),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("위젯", color = Color(0xFFC6C6C6), style = MaterialTheme.typography.labelLarge)
                    Text("다음 일정", color = Color(0xFFC6C6C6), style = MaterialTheme.typography.labelSmall)
                    Text("일정 없음", style = MaterialTheme.typography.titleMedium)
                    Text("알람", color = Color(0xFFC6C6C6), style = MaterialTheme.typography.labelSmall)
                    Text("07:00", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("위아래 스와이프: 위젯 변경", color = Color(0xFFC6C6C6), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun NoticeCard(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            TextButton(onClick = onDismiss) { Text("확인") }
        }
    }
}

@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Text(text, modifier = Modifier.weight(1f))
        }
    }
}

private fun dualScreenExplanation(status: DualScreenStatus): String = when (status) {
    DualScreenStatus.Checking -> "커버 화면 capability를 확인하고 있습니다."
    DualScreenStatus.Available -> "시스템 승인을 거쳐 내부 화면에는 무드등, 보조 화면에는 시계를 표시합니다."
    DualScreenStatus.Active -> "듀얼 화면 세션이 실행 중입니다."
    DualScreenStatus.Unavailable -> "현재 접힘 상태나 다른 화면 세션 때문에 동시 표시를 시작할 수 없습니다. 내부 화면 분할은 바로 사용할 수 있습니다."
    DualScreenStatus.Unsupported -> "이 기기는 외부·내부 동시 표시 API를 제공하지 않습니다. 내부 화면 분할로 실행해 주세요."
    is DualScreenStatus.Error -> "듀얼 화면을 시작하지 못했습니다. 내부 화면 분할로 실행해 주세요. (${status.reason})"
}
