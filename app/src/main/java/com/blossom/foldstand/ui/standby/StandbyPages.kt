package com.blossom.foldstand.ui.standby

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blossom.foldstand.BuildConfig
import com.blossom.foldstand.data.CalendarRepository
import com.blossom.foldstand.data.NotificationRepository
import com.blossom.foldstand.domain.CalendarEvent
import com.blossom.foldstand.domain.BatteryState
import com.blossom.foldstand.domain.NotificationItem
import com.blossom.foldstand.domain.NightModeOption
import com.blossom.foldstand.domain.StandbySettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private val CardShape = RoundedCornerShape(28.dp)
private val CardColor = Color(0xFF171717)
private val SecondaryText = Color(0xFFC6C6C6)

/** A glanceable cover-screen composition inspired by StandBy's clock + widget pair. */
@Composable
fun CoverStandbyPane(
    settings: StandbySettings,
    battery: BatteryState,
    calendarPermissionGranted: Boolean,
    modifier: Modifier = Modifier,
    showClock: Boolean = true,
) {
    val context = LocalContext.current
    val events by rememberUpcomingEvents(context, calendarPermissionGranted)
    val notifications by rememberNotifications(context, BuildConfig.NOTIFICATION_ACCESS_AVAILABLE)
    val ambientLux by rememberAmbientLux(settings.nightMode == NightModeOption.Auto)
    val darkEnvironment = ambientLux?.let { it < 12f }
        ?: (java.time.LocalTime.now().hour >= 22 || java.time.LocalTime.now().hour < 7)
    val nightTint = when (settings.nightMode) {
        NightModeOption.On -> true
        NightModeOption.Off -> false
        NightModeOption.Auto -> darkEnvironment
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(18.dp)) {
        val narrow = maxWidth < maxHeight * 0.82f
        if (showClock && narrow) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().weight(0.62f)) {
                    ClockPane(settings = settings, battery = battery, nightTint = nightTint)
                }
                CoverInfoRow(battery, events, notifications, Modifier.weight(0.38f))
            }
        } else if (showClock) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.weight(0.62f).fillMaxSize()) {
                    ClockPane(settings = settings, battery = battery, nightTint = nightTint)
                }
                CoverInfoColumn(settings, battery, events, notifications, Modifier.weight(0.38f))
            }
        } else if (narrow) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.weight(0.62f))
                CoverInfoRow(battery, events, notifications, Modifier.weight(0.38f))
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Spacer(Modifier.weight(0.62f))
                CoverInfoColumn(settings, battery, events, notifications, Modifier.weight(0.38f))
            }
        }
    }
}

@Composable
private fun CoverInfoRow(
    battery: BatteryState,
    events: List<CalendarEvent>,
    notifications: List<NotificationItem>,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CoverInfoCard(
            title = "다음 일정",
            value = events.firstOrNull()?.title ?: "일정 없음",
            modifier = Modifier.weight(1f),
        )
        CoverInfoCard(
            title = "알림 · 배터리",
            value = "${notifications.size}개 · ${battery.percent?.let { "$it%" } ?: "—"}",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CoverInfoColumn(
    settings: StandbySettings,
    battery: BatteryState,
    events: List<CalendarEvent>,
    notifications: List<NotificationItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CoverInfoCard(
            title = "다음 일정",
            value = events.firstOrNull()?.let { "${it.title}\n${formatEventTime(it)}" } ?: "일정 없음",
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        CoverInfoCard(
            title = "다음 알람",
            value = if (settings.alarmEnabled) "%02d:%02d".format(settings.alarmHour, settings.alarmMinute) else "꺼짐",
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        CoverInfoCard(
            title = "알림 · 배터리",
            value = "${notifications.size}개 · ${battery.percent?.let { "$it%" } ?: "—"}",
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Composable
private fun CoverInfoCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF1C1C1E),
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = SecondaryText, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun StandbyWidgetsPane(
    calendarPermissionGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val events by rememberUpcomingEvents(context, calendarPermissionGranted)
    val notifications by rememberNotifications(context, BuildConfig.NOTIFICATION_ACCESS_AVAILABLE)
    Column(
        modifier = modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("위젯", color = SecondaryText, style = MaterialTheme.typography.labelLarge)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            if (maxWidth < 520.dp) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CalendarWidgetCard(
                        events = events,
                        calendarPermissionGranted = calendarPermissionGranted,
                        onRequestPermission = onRequestCalendarPermission,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                    NotificationWidgetCard(
                        notifications = notifications,
                        available = BuildConfig.NOTIFICATION_ACCESS_AVAILABLE,
                        onOpenSettings = onOpenNotificationSettings,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            } else {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CalendarWidgetCard(
                        events = events,
                        calendarPermissionGranted = calendarPermissionGranted,
                        onRequestPermission = onRequestCalendarPermission,
                        modifier = Modifier.weight(1f),
                    )
                    NotificationWidgetCard(
                        notifications = notifications,
                        available = BuildConfig.NOTIFICATION_ACCESS_AVAILABLE,
                        onOpenSettings = onOpenNotificationSettings,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarPage(
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val yearMonth = remember(today) { YearMonth.from(today) }
    val context = LocalContext.current
    val events by rememberUpcomingEvents(context, permissionGranted)
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFE66B6B))
            Column(Modifier.padding(start = 12.dp)) {
                Text("${today.monthValue}월 ${today.dayOfMonth}일", fontSize = 30.sp, fontWeight = FontWeight.Light)
                Text("${today.year}년 ${today.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.KOREAN)}", color = SecondaryText)
            }
        }
        Surface(shape = CardShape, color = CardColor, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${yearMonth.monthValue}월", style = MaterialTheme.typography.titleLarge)
                WeekdayHeader()
                MonthGrid(yearMonth, today)
            }
        }
        if (!permissionGranted) {
            PermissionCard(
                icon = Icons.Default.CalendarMonth,
                title = "일정을 표시하려면 권한이 필요합니다",
                description = "기기의 캘린더를 읽기 전용으로 사용합니다.",
                button = "캘린더 권한 허용",
                onClick = onRequestPermission,
            )
        } else if (events.isEmpty()) {
            Text("앞으로 7일 안에 예정된 일정이 없습니다.", color = SecondaryText)
        } else {
            Text("다가오는 일정", style = MaterialTheme.typography.titleMedium)
            events.take(4).forEach { event -> EventRow(event) }
        }
    }
}

@Composable
fun NotificationsPage(
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notifications by rememberNotifications(context, BuildConfig.NOTIFICATION_ACCESS_AVAILABLE)
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFE66B6B))
            Text("알림", modifier = Modifier.padding(start = 12.dp), fontSize = 30.sp, fontWeight = FontWeight.Light)
        }
        if (!BuildConfig.NOTIFICATION_ACCESS_AVAILABLE) {
            NotificationUnavailableCard()
        } else if (notifications.isEmpty()) {
            PermissionCard(
                icon = Icons.Default.Security,
                title = "알림 접근을 켜면 최근 알림을 보여줍니다",
                description = "FoldStand는 알림을 서버로 보내지 않고 기기 안에 최근 12개만 보관합니다.",
                button = "알림 접근 설정",
                onClick = onOpenNotificationSettings,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(count = notifications.size) { index -> NotificationRow(notifications[index]) }
            }
        }
    }
}

@Composable
private fun CalendarWidgetCard(
    events: List<CalendarEvent>,
    calendarPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier,
) {
    Surface(shape = CardShape, color = CardColor, modifier = modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFE66B6B))
                Text("일정", modifier = Modifier.padding(start = 8.dp), color = SecondaryText)
            }
            Text("${LocalDate.now().dayOfMonth}", fontSize = 62.sp, fontWeight = FontWeight.Light)
            if (!calendarPermissionGranted) {
                Text("캘린더 권한을 허용하면 일정을 표시합니다.", color = SecondaryText, fontSize = 13.sp)
                OutlinedButton(onClick = onRequestPermission) { Text("권한 허용") }
            } else if (events.isEmpty()) {
                Text("예정된 일정 없음", color = SecondaryText)
            } else {
                events.take(3).forEach { event ->
                    Text(event.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatEventTime(event), color = SecondaryText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun NotificationWidgetCard(
    notifications: List<NotificationItem>,
    available: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier,
) {
    Surface(shape = CardShape, color = CardColor, modifier = modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF8AA9FF))
                Text("알림", modifier = Modifier.padding(start = 8.dp), color = SecondaryText)
            }
            Text(if (available) "${notifications.size}" else "—", fontSize = 62.sp, fontWeight = FontWeight.Light)
            if (!available) {
                Text("직접 설치본에서는 알림 접근 기능을 제외했습니다.", color = SecondaryText, fontSize = 13.sp)
            } else if (notifications.isEmpty()) {
                Text("알림 접근을 허용하면 최근 알림을 보여줍니다.", color = SecondaryText, fontSize = 13.sp)
                OutlinedButton(onClick = onOpenSettings) { Text("설정 열기") }
            } else {
                notifications.take(3).forEach { item ->
                    Text(item.title.ifBlank { item.appName }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.text, maxLines = 1, overflow = TextOverflow.Ellipsis, color = SecondaryText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun NotificationUnavailableCard() {
    Surface(shape = CardShape, color = CardColor, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE66B6B))
            Text("알림 기능은 전체 기능본에서 사용할 수 있습니다", style = MaterialTheme.typography.titleMedium)
            Text(
                "브라우저 직접 설치본은 Google Play 프로텍트 차단을 피하기 위해 다른 앱의 알림에 접근하지 않습니다.",
                color = SecondaryText,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    button: String,
    onClick: () -> Unit,
) {
    Surface(shape = CardShape, color = CardColor, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFFE66B6B))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = SecondaryText)
            Button(onClick = onClick) {
                Text(button)
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(38.dp).background(Color(0xFFE66B6B), RoundedCornerShape(3.dp)))
        Column(Modifier.padding(start = 12.dp)) {
            Text(event.title)
            Text(formatEventTime(event), color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem) {
    Surface(shape = RoundedCornerShape(20.dp), color = CardColor, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(Color(0xFFE66B6B), RoundedCornerShape(50)))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(item.title.ifBlank { item.appName }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.text, color = SecondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.appName, color = SecondaryText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEach { Text(it, color = SecondaryText) }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, today: LocalDate) {
    val firstOffset = month.atDay(1).dayOfWeek.value % 7
    val days = (0 until firstOffset).map { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(220.dp)) {
        items(days) { day ->
            Box(Modifier.size(30.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                if (day != null) {
                    val isToday = day == today
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isToday) Color(0xFFE66B6B) else Color.Transparent,
                    ) { Text("${day.dayOfMonth}", modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)) }
                }
            }
        }
    }
}

@Composable
private fun rememberUpcomingEvents(context: Context, permissionGranted: Boolean): State<List<CalendarEvent>> =
    produceState(emptyList(), context, permissionGranted) {
        if (!permissionGranted) return@produceState
        while (isActive) {
            value = withContext(Dispatchers.IO) {
                runCatching { CalendarRepository.readUpcoming(context) }.getOrDefault(emptyList())
            }
            delay(60_000L)
        }
}

@Composable
private fun rememberNotifications(
    context: Context,
    enabled: Boolean,
): State<List<NotificationItem>> =
    produceState(if (enabled) emptyList() else emptyList(), context, enabled) {
        if (!enabled) return@produceState
        while (isActive) {
            value = withContext(Dispatchers.IO) {
                runCatching { NotificationRepository.read(context) }.getOrDefault(emptyList())
            }
            delay(5_000L)
        }
}

private fun formatEventTime(event: CalendarEvent): String {
    if (event.allDay) return "종일"
    val start = Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.systemDefault())
    return start.format(DateTimeFormatter.ofPattern("M월 d일 · HH:mm", Locale.KOREAN))
}
