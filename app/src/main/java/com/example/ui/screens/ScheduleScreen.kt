package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AlarmOn
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FocusSchedule
import com.example.data.FocusScheduleRepository
import com.example.data.FocusSettingsState
import com.example.data.ScheduleRepeatMode
import com.example.service.ScheduleAlarmScheduler
import com.example.ui.components.ScenicPageVariant
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemeScenicBackdropOverlay
import com.example.ui.components.focusGlassPanel
import com.example.ui.components.rememberFocusHaptics
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScheduleScreen(
    settingsState: FocusSettingsState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()

    val scheduleState by FocusScheduleRepository.observe(context).collectAsStateWithLifecycle()

    var editingSchedule by remember { mutableStateOf<FocusSchedule?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var hasOverlayPermission by remember {
        mutableStateOf(runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false))
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(ScheduleAlarmScheduler.canScheduleExactAlarms(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)
                hasExactAlarmPermission = ScheduleAlarmScheduler.canScheduleExactAlarms(context)
                ScheduleAlarmScheduler.syncAllAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val nextWindow = remember(scheduleState.schedules) {
        FocusScheduleRepository.findNextUpcomingWindow(scheduleState.schedules)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .navigationBarsPadding()
            .testTag("schedule_screen_root")
    ) {
        ThemeBackdropLayer(palette = palette)
        ThemeScenicBackdropOverlay(
            variant = ScenicPageVariant.SCHEDULE,
            palette = palette
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Minimal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 18.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = palette.displayFont,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (nextWindow != null) {
                            val rel = FocusScheduleRepository.formatRelativeCountdown(nextWindow.startTimeMs)
                            if (nextWindow.isCurrentlyActive) "Active · ${nextWindow.schedule.name}"
                            else "Next · ${nextWindow.schedule.name} in $rel"
                        } else {
                            "Automated strict countdown locks"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.primary)
                        .clickable {
                            haptics.tap()
                            editingSchedule = null
                            showCreateDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .testTag("add_schedule_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New Schedule",
                        tint = palette.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "New",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onPrimary
                    )
                }
            }

            SchedulesListContent(
                schedules = scheduleState.schedules,
                needsOverlayPermission = !hasOverlayPermission,
                needsExactAlarmPermission = !hasExactAlarmPermission,
                onRequestOverlayPermission = {
                    haptics.tap()
                    runCatching {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(intent)
                    }
                },
                onRequestExactAlarmPermission = {
                    haptics.tap()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}")
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        }
                    }
                },
                onCreateClick = {
                    haptics.tap()
                    editingSchedule = null
                    showCreateDialog = true
                },
                onEditSchedule = { sched ->
                    haptics.tap()
                    editingSchedule = sched
                    showCreateDialog = true
                },
                onToggleSchedule = { id, enabled ->
                    haptics.tick()
                    FocusScheduleRepository.setScheduleEnabled(context, id, enabled)
                },
                onUnskipSchedule = { id ->
                    haptics.tick()
                    FocusScheduleRepository.unskipForToday(context, id)
                },
                onDeleteSchedule = { id ->
                    haptics.reject()
                    FocusScheduleRepository.deleteSchedule(context, id)
                }
            )
        }
    }

    if (showCreateDialog) {
        ScheduleEditorDialog(
            initialSchedule = editingSchedule,
            onDismiss = { showCreateDialog = false },
            onSave = { saved ->
                haptics.confirm()
                FocusScheduleRepository.addOrUpdateSchedule(context, saved)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun SchedulesListContent(
    schedules: List<FocusSchedule>,
    needsOverlayPermission: Boolean,
    needsExactAlarmPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onCreateClick: () -> Unit,
    onEditSchedule: (FocusSchedule) -> Unit,
    onToggleSchedule: (String, Boolean) -> Unit,
    onUnskipSchedule: (String) -> Unit,
    onDeleteSchedule: (String) -> Unit
) {
    val palette = LocalFocusTheme.current
    val cardShape = remember { RoundedCornerShape(30.dp) }
    val pillShape = remember { RoundedCornerShape(50) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // One-tap permission pills when required for reliable on-time background locking
        if (needsOverlayPermission) {
            item(key = "schedule_overlay_permission_pill") {
                SchedulePermissionPill(
                    icon = Icons.Outlined.Layers,
                    title = "Display Over Other Apps",
                    subtitle = "Required to show the scheduled lock screen automatically.",
                    actionLabel = "Allow",
                    onClick = onRequestOverlayPermission,
                    testTag = "schedule_grant_overlay_button"
                )
            }
        }

        if (needsExactAlarmPermission) {
            item(key = "schedule_exact_alarm_permission_pill") {
                SchedulePermissionPill(
                    icon = Icons.Outlined.AlarmOn,
                    title = "Allow Exact Alarms",
                    subtitle = "Required on Android 12+ so schedules start on the exact minute.",
                    actionLabel = "Enable",
                    onClick = onRequestExactAlarmPermission,
                    testTag = "schedule_grant_exact_alarm_button"
                )
            }
        }

        // Quiet rule notice pill
        item(key = "schedule_rule_pill") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .focusGlassPanel(shape = RoundedCornerShape(24.dp), palette = palette)
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockClock,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = "Timer-only strict locks — unlocks automatically when time ends. Auto-skips today if a manual OTP session is already active.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.5.sp,
                    color = palette.textSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        if (schedules.isEmpty()) {
            item(key = "empty_schedules_card") {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .focusGlassPanel(shape = cardShape, palette = palette)
                        .padding(horizontal = 24.dp, vertical = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(84.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val r = size.minDimension / 2f - 4.dp.toPx()
                            when (palette.theme) {
                                AppVisualTheme.LIQUID_GLASS -> {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.05f),
                                                Color.White.copy(alpha = 0.24f),
                                                Color.White.copy(alpha = 0.05f)
                                            )
                                        ),
                                        radius = r,
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                                AppVisualTheme.NOTHING_DOT -> {
                                    val dots = 16
                                    for (i in 0 until dots) {
                                        val rad = Math.toRadians(-90.0 + (360.0 / dots) * i)
                                        drawCircle(
                                            color = if (i % 4 == 0) palette.primary else palette.borderHighlight,
                                            radius = if (i % 4 == 0) 2.dp.toPx() else 1.1.dp.toPx(),
                                            center = Offset(cx + (r * cos(rad)).toFloat(), cy + (r * sin(rad)).toFloat())
                                        )
                                    }
                                }
                                AppVisualTheme.SOLAR_AMBER -> {
                                    val ticks = 24
                                    for (i in 0 until ticks) {
                                        val rad = Math.toRadians(-90.0 + (360.0 / ticks) * i)
                                        val major = i % 6 == 0
                                        val inner = r - if (major) 4.dp.toPx() else 2.dp.toPx()
                                        drawLine(
                                            color = if (major) palette.primary.copy(alpha = 0.72f) else palette.borderHighlight,
                                            start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                                            end = Offset(cx + (r * cos(rad)).toFloat(), cy + (r * sin(rad)).toFloat()),
                                            strokeWidth = if (major) 1.5.dp.toPx() else 0.85.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(palette.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Text(
                        text = "No Schedules Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = palette.displayFont,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Create daily, weekday, or custom focus blocks that lock automatically on time.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.5.sp,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onCreateClick,
                        shape = pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier.testTag("empty_create_schedule_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Schedule", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            items(items = schedules, key = { it.id }) { schedule ->
                val skippedToday = FocusScheduleRepository.isSkippedToday(schedule)
                val nextOcc = remember(schedule) {
                    FocusScheduleRepository.computeNextOccurrence(schedule)
                }
                val durMins = schedule.durationMinutes()
                val durLabel = if (durMins >= 60) {
                    val h = durMins / 60
                    val m = durMins % 60
                    if (m > 0) "${h}h ${m}m" else "${h}h"
                } else "${durMins}m"

                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .focusGlassPanel(
                            shape = cardShape,
                            palette = palette,
                            elevated = schedule.enabled
                        )
                        .clickable { onEditSchedule(schedule) }
                        .padding(horizontal = 18.dp, vertical = 15.dp)
                        .testTag("schedule_card_${schedule.id}"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = schedule.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (schedule.enabled) palette.textPrimary else palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = schedule.formattedTimeRange(),
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (schedule.enabled) palette.primary else palette.textSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { onDeleteSchedule(schedule.id) },
                                modifier = Modifier.testTag("delete_schedule_${schedule.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete ${schedule.name}",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Switch(
                                checked = schedule.enabled,
                                onCheckedChange = { onToggleSchedule(schedule.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = palette.onPrimary,
                                    checkedTrackColor = palette.primary,
                                    uncheckedThumbColor = palette.textSecondary,
                                    uncheckedTrackColor = palette.surfaceElevated
                                ),
                                modifier = Modifier.testTag("toggle_schedule_${schedule.id}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScheduleMetaPill(text = schedule.repeatSummary(), accent = false)
                        ScheduleMetaPill(text = durLabel, accent = false)
                        if (skippedToday) {
                            Box(
                                modifier = Modifier
                                    .clip(pillShape)
                                    .background(palette.warning.copy(alpha = 0.16f), pillShape)
                                    .clickable { onUnskipSchedule(schedule.id) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Skipped today · Restore",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.warning
                                )
                            }
                        } else if (schedule.enabled && nextOcc != null) {
                            val rel = FocusScheduleRepository.formatRelativeCountdown(nextOcc.startTimeMs)
                            ScheduleMetaPill(
                                text = if (nextOcc.isCurrentlyActive) "Active now" else "In $rel",
                                accent = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedulePermissionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalFocusTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .focusGlassPanel(shape = RoundedCornerShape(26.dp), palette = palette, elevated = true)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(palette.warning.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.warning,
                modifier = Modifier.size(17.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = palette.textSecondary,
                lineHeight = 14.5.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(palette.primary)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = actionLabel,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.onPrimary
            )
        }
    }
}

@Composable
private fun ScheduleMetaPill(text: String, accent: Boolean) {
    val palette = LocalFocusTheme.current
    val shape = remember { RoundedCornerShape(50) }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (accent) palette.primaryContainer else palette.surfaceElevated, shape)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (accent) palette.primary else palette.textSecondary
        )
    }
}

@Composable
private fun ScheduleEditorDialog(
    initialSchedule: FocusSchedule?,
    onDismiss: () -> Unit,
    onSave: (FocusSchedule) -> Unit
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()

    val defaultTimes = remember(initialSchedule) {
        if (initialSchedule != null) {
            listOf(
                initialSchedule.startHour,
                initialSchedule.startMinute,
                initialSchedule.endHour,
                initialSchedule.endMinute
            )
        } else {
            val startCal = Calendar.getInstance().apply { add(Calendar.MINUTE, 2) }
            val endCal = (startCal.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
            listOf(
                startCal.get(Calendar.HOUR_OF_DAY),
                startCal.get(Calendar.MINUTE),
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE)
            )
        }
    }

    var name by remember(initialSchedule) { mutableStateOf(initialSchedule?.name ?: "Deep Focus") }
    var startHour by remember(initialSchedule) { mutableIntStateOf(defaultTimes[0]) }
    var startMinute by remember(initialSchedule) { mutableIntStateOf(defaultTimes[1]) }
    var endHour by remember(initialSchedule) { mutableIntStateOf(defaultTimes[2]) }
    var endMinute by remember(initialSchedule) { mutableIntStateOf(defaultTimes[3]) }
    var repeatMode by remember(initialSchedule) {
        mutableStateOf(initialSchedule?.repeatMode ?: ScheduleRepeatMode.DAILY)
    }
    var customDays by remember(initialSchedule) {
        mutableStateOf(
            initialSchedule?.customDays ?: setOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Text(
                text = if (initialSchedule == null) "New Schedule" else "Edit Schedule",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 28) name = it },
                    label = { Text("Schedule Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Start & Stop time pickers (1 minute per tap, 00..59)
                TimeStepperRow(
                    label = "START TIME",
                    hour = startHour,
                    minute = startMinute,
                    onHourChange = { haptics.tick(); startHour = it },
                    onMinuteChange = { haptics.tick(); startMinute = it },
                    tagPrefix = "start"
                )

                TimeStepperRow(
                    label = "STOP TIME",
                    hour = endHour,
                    minute = endMinute,
                    onHourChange = { haptics.tick(); endHour = it },
                    onMinuteChange = { haptics.tick(); endMinute = it },
                    tagPrefix = "stop"
                )

                // Quick duration preset pills relative to Start Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h").forEach { (mins, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(palette.surfaceElevated)
                                .clickable {
                                    haptics.tick()
                                    val total = (startHour * 60 + startMinute + mins) % (24 * 60)
                                    endHour = total / 60
                                    endMinute = total % 60
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$label",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = palette.primary
                            )
                        }
                    }
                }

                // Repeat mode pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "REPEAT",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.5.sp,
                        color = palette.textSecondary
                    )
                    val firstRow = listOf(ScheduleRepeatMode.DAILY, ScheduleRepeatMode.WEEKDAYS, ScheduleRepeatMode.WEEKENDS)
                    val secondRow = listOf(ScheduleRepeatMode.CUSTOM, ScheduleRepeatMode.ONCE)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        firstRow.forEach { mode ->
                            val selected = repeatMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) palette.primary else palette.surfaceElevated)
                                    .clickable { haptics.tick(); repeatMode = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.shortLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) palette.onPrimary else palette.textSecondary
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        secondRow.forEach { mode ->
                            val selected = repeatMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) palette.primary else palette.surfaceElevated)
                                    .clickable { haptics.tick(); repeatMode = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.shortLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) palette.onPrimary else palette.textSecondary
                                )
                            }
                        }
                    }
                }

                // Custom days circular pills
                AnimatedVisibility(visible = repeatMode == ScheduleRepeatMode.CUSTOM) {
                    val days = listOf(
                        Calendar.MONDAY to "M",
                        Calendar.TUESDAY to "T",
                        Calendar.WEDNESDAY to "W",
                        Calendar.THURSDAY to "T",
                        Calendar.FRIDAY to "F",
                        Calendar.SATURDAY to "S",
                        Calendar.SUNDAY to "S"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { (dow, letter) ->
                            val active = dow in customDays
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (active) palette.primary else palette.surfaceElevated)
                                    .clickable {
                                        haptics.tick()
                                        customDays = if (active && customDays.size > 1) {
                                            customDays - dow
                                        } else {
                                            customDays + dow
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) palette.onPrimary else palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = if (initialSchedule != null) {
                        initialSchedule.copy(
                            name = name.trim().ifBlank { "Focus Block" },
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            repeatMode = repeatMode,
                            customDays = customDays,
                            skippedDateKey = null
                        )
                    } else {
                        FocusScheduleRepository.createNewSchedule(
                            name = name,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            repeatMode = repeatMode,
                            customDays = customDays
                        )
                    }
                    onSave(result)
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                modifier = Modifier.testTag("save_schedule_button")
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textSecondary)
            }
        }
    )
}

@Composable
private fun TimeStepperRow(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    tagPrefix: String
) {
    val palette = LocalFocusTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                color = palette.textSecondary
            )
            Text(
                text = FocusSchedule.formatClock(hour, minute),
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(palette.surfaceElevated)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "−",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onHourChange((hour + 23) % 24) }
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .testTag("${tagPrefix}_hour_minus")
                )
                Text(
                    text = "${String.format(Locale.US, "%02d", hour)}h",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = palette.textPrimary
                )
                Text(
                    text = "+",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onHourChange((hour + 1) % 24) }
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .testTag("${tagPrefix}_hour_plus")
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(palette.surfaceElevated)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "−",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onMinuteChange((minute + 59) % 60) }
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .testTag("${tagPrefix}_minute_minus")
                )
                Text(
                    text = "${String.format(Locale.US, "%02d", minute)}m",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = palette.textPrimary,
                    modifier = Modifier.testTag("${tagPrefix}_minute_value")
                )
                Text(
                    text = "+",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onMinuteChange((minute + 1) % 60) }
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .testTag("${tagPrefix}_minute_plus")
                )
            }
        }
    }
}
