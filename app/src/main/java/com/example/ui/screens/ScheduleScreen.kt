package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.RestartAlt
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FocusSchedule
import com.example.data.FocusScheduleRepository
import com.example.data.FocusSettingsState
import com.example.data.FocusXpRepository
import com.example.data.LockModuleAlignment
import com.example.data.LockModuleSize
import com.example.data.LockSpacingDensity
import com.example.data.ScheduleRepeatMode
import com.example.data.ScheduledBottomDockMode
import com.example.data.ScheduledLockLayoutConfig
import com.example.data.ScheduledLockModuleConfig
import com.example.data.ScheduledLockModuleType
import com.example.data.ScheduledTimerStyle
import com.example.ui.components.ElasticMorphingSegmentedPill
import com.example.ui.components.ElasticPillSegment
import com.example.ui.components.ScheduledLockScreen
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.focusGlassPanel
import com.example.ui.components.rememberFocusHaptics
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import java.util.Calendar

private enum class ScheduleSubTab {
    SCHEDULES,
    LOCK_STUDIO
}

@Composable
fun ScheduleScreen(
    settingsState: FocusSettingsState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()

    val scheduleState by FocusScheduleRepository.observe(context).collectAsStateWithLifecycle()
    val xpState by FocusXpRepository.observe(context).collectAsStateWithLifecycle()
    val iconCache by com.example.data.FocusSettingsRepository.appIconsState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(ScheduleSubTab.SCHEDULES) }
    var editingSchedule by remember { mutableStateOf<FocusSchedule?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showFullscreenPreview by remember { mutableStateOf(false) }

    val nextWindow = remember(scheduleState.schedules) {
        FocusScheduleRepository.findNextUpcomingWindow(scheduleState.schedules)
    }

    val goalMs = settingsState.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
    val dailyGoalPercent = ((xpState.todayFocusMs.toFloat() / goalMs.toFloat()) * 100f)
        .toInt()
        .coerceIn(0, 999)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .navigationBarsPadding()
            .testTag("schedule_screen_root")
    ) {
        ThemeBackdropLayer(palette = palette)

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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

                if (activeTab == ScheduleSubTab.SCHEDULES) {
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
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surfaceElevated)
                            .clickable {
                                haptics.tap()
                                showFullscreenPreview = true
                            }
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                            .testTag("schedule_full_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fullscreen,
                            contentDescription = "Full Preview",
                            tint = palette.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Preview",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                    }
                }
            }

            // Elastic Segmented Switcher: Schedules | Lock Studio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                ElasticMorphingSegmentedPill(
                    segments = listOf(
                        ElasticPillSegment("Schedules", "schedule_tab_list"),
                        ElasticPillSegment("Lock Studio", "schedule_tab_studio")
                    ),
                    selectedIndex = if (activeTab == ScheduleSubTab.SCHEDULES) 0 else 1,
                    onSelectIndex = { idx ->
                        activeTab = if (idx == 0) ScheduleSubTab.SCHEDULES else ScheduleSubTab.LOCK_STUDIO
                    },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(spring(stiffness = Spring.StiffnessMedium))
                },
                label = "schedule_subtab_content",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    ScheduleSubTab.SCHEDULES -> {
                        SchedulesListContent(
                            schedules = scheduleState.schedules,
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

                    ScheduleSubTab.LOCK_STUDIO -> {
                        ScheduledLockStudioContent(
                            layoutConfig = scheduleState.layoutConfig,
                            settingsState = settingsState,
                            dailyGoalPercent = dailyGoalPercent,
                            currentStreak = xpState.currentStreak,
                            iconCache = iconCache,
                            sampleScheduleName = nextWindow?.schedule?.name ?: "Deep Focus",
                            sampleTimeRange = nextWindow?.schedule?.formattedTimeRange() ?: "9:00 AM → 11:00 AM",
                            onOpenFullPreview = {
                                haptics.tap()
                                showFullscreenPreview = true
                            }
                        )
                    }
                }
            }
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

    if (showFullscreenPreview) {
        val now = remember { System.currentTimeMillis() }
        Dialog(
            onDismissRequest = { showFullscreenPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ScheduledLockScreen(
                    scheduleName = nextWindow?.schedule?.name ?: "Deep Focus",
                    timeRangeLabel = nextWindow?.schedule?.formattedTimeRange() ?: "9:00 AM → 11:00 AM",
                    startTimeMs = now - 25 * 60_000L,
                    endTimeMs = now + 55 * 60_000L,
                    layoutConfig = scheduleState.layoutConfig,
                    dailyGoalPercent = dailyGoalPercent,
                    currentStreak = xpState.currentStreak,
                    allowedApps = settingsState.allowedApps,
                    appLayout = settingsState.lockAppLayout,
                    pillButtonSizeDp = settingsState.minimalPillButtonSizeDp,
                    iconCache = iconCache,
                    isPreview = false
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.surfaceElevated.copy(alpha = 0.92f))
                        .clickable {
                            haptics.tap()
                            showFullscreenPreview = false
                        }
                        .padding(horizontal = 13.dp, vertical = 8.dp)
                        .testTag("close_fullscreen_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close Preview",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Close",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SchedulesListContent(
    schedules: List<FocusSchedule>,
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    text = "Timer-only strict locks — auto-unlocks when time ends. Auto-skips today if a manual OTP session is already active.",
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
                    Text(
                        text = "No Schedules Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Create daily, weekday, or custom focus blocks with a personalized timer-only lock screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.5.sp,
                        color = palette.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        .focusGlassPanel(shape = cardShape, palette = palette)
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
private fun ScheduledLockStudioContent(
    layoutConfig: ScheduledLockLayoutConfig,
    settingsState: FocusSettingsState,
    dailyGoalPercent: Int,
    currentStreak: Int,
    iconCache: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    sampleScheduleName: String,
    sampleTimeRange: String,
    onOpenFullPreview: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val now = remember { System.currentTimeMillis() }
    var mantraDraft by remember(layoutConfig.customMantra) {
        mutableStateOf(layoutConfig.customMantra)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Phone-Frame Interactive Example Preview (updates immediately on every change!)
        item(key = "live_preview_frame") {
            val frameShape = remember { RoundedCornerShape(34.dp) }
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(236.dp)
                        .height(412.dp)
                        .clip(frameShape)
                        .background(palette.surfaceElevated.copy(alpha = 0.55f), frameShape)
                        .padding(6.dp)
                        .clickable(onClick = onOpenFullPreview)
                        .testTag("lock_studio_live_preview_box")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp))
                    ) {
                        ScheduledLockScreen(
                            scheduleName = sampleScheduleName,
                            timeRangeLabel = sampleTimeRange,
                            startTimeMs = now - 25 * 60_000L,
                            endTimeMs = now + 55 * 60_000L,
                            layoutConfig = layoutConfig,
                            dailyGoalPercent = dailyGoalPercent,
                            currentStreak = currentStreak,
                            allowedApps = settingsState.allowedApps,
                            appLayout = settingsState.lockAppLayout,
                            pillButtonSizeDp = settingsState.minimalPillButtonSizeDp,
                            iconCache = iconCache,
                            isPreview = true
                        )
                    }
                }
                Text(
                    text = "Live preview · Tap preview for fullscreen",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.5.sp,
                    color = palette.textSecondary
                )
            }
        }

        // Global Styling Controls (Timer Style, Density, Breathing Cadence, Mantra)
        item(key = "global_studio_controls") {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .focusGlassPanel(shape = RoundedCornerShape(28.dp), palette = palette)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LOCK STYLE",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = palette.textSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surfaceElevated)
                            .clickable {
                                haptics.tap()
                                FocusScheduleRepository.resetLayoutDefaults(context)
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset Layout",
                            tint = palette.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text("Reset", fontSize = 11.sp, color = palette.primary)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Timer Display", fontSize = 12.sp, color = palette.textSecondary)
                    ElasticMorphingSegmentedPill(
                        segments = ScheduledTimerStyle.entries.map { ElasticPillSegment(it.label) },
                        selectedIndex = ScheduledTimerStyle.entries.indexOf(layoutConfig.timerStyle),
                        onSelectIndex = { idx ->
                            FocusScheduleRepository.setTimerStyle(context, ScheduledTimerStyle.entries[idx])
                        },
                        height = 38.dp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Vertical Spacing", fontSize = 12.sp, color = palette.textSecondary)
                    ElasticMorphingSegmentedPill(
                        segments = LockSpacingDensity.entries.map { ElasticPillSegment(it.label) },
                        selectedIndex = LockSpacingDensity.entries.indexOf(layoutConfig.spacingDensity),
                        onSelectIndex = { idx ->
                            FocusScheduleRepository.setSpacingDensity(context, LockSpacingDensity.entries[idx])
                        },
                        height = 38.dp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bottom Dock", fontSize = 12.sp, color = palette.textSecondary)
                    ElasticMorphingSegmentedPill(
                        segments = ScheduledBottomDockMode.entries.map { ElasticPillSegment(it.label) },
                        selectedIndex = ScheduledBottomDockMode.entries.indexOf(layoutConfig.bottomDockMode),
                        onSelectIndex = { idx ->
                            FocusScheduleRepository.setBottomDockMode(context, ScheduledBottomDockMode.entries[idx])
                        },
                        height = 38.dp
                    )
                }

                OutlinedTextField(
                    value = mantraDraft,
                    onValueChange = { next ->
                        if (next.length <= 90) {
                            mantraDraft = next
                            FocusScheduleRepository.setCustomMantra(context, next)
                        }
                    },
                    label = { Text("Focus Mantra", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Per-Module Reorder, Toggle, Size (S/M/L), and Alignment (L/C/R) Controls
        items(items = layoutConfig.modules, key = { it.type.name }) { mod ->
            val idx = layoutConfig.modules.indexOf(mod)
            LockStudioModuleCard(
                module = mod,
                canMoveUp = idx > 0,
                canMoveDown = idx < layoutConfig.modules.lastIndex,
                onToggle = { enabled ->
                    haptics.tick()
                    FocusScheduleRepository.toggleModule(context, mod.type, enabled)
                },
                onMoveUp = {
                    haptics.tick()
                    FocusScheduleRepository.moveModule(context, mod.type, -1)
                },
                onMoveDown = {
                    haptics.tick()
                    FocusScheduleRepository.moveModule(context, mod.type, +1)
                },
                onSelectSize = { size ->
                    FocusScheduleRepository.setModuleSize(context, mod.type, size)
                },
                onSelectAlignment = { align ->
                    FocusScheduleRepository.setModuleAlignment(context, mod.type, align)
                }
            )
        }
    }
}

@Composable
private fun LockStudioModuleCard(
    module: ScheduledLockModuleConfig,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSelectSize: (LockModuleSize) -> Unit,
    onSelectAlignment: (LockModuleAlignment) -> Unit
) {
    val palette = LocalFocusTheme.current
    val isCoreTimer = module.type == ScheduledLockModuleType.COUNTDOWN_TIMER

    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .focusGlassPanel(shape = RoundedCornerShape(26.dp), palette = palette)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag("lock_module_card_${module.type.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = module.type.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (module.enabled) palette.textPrimary else palette.textSecondary
                )
                Text(
                    text = if (isCoreTimer) "Always visible on scheduled locks" else module.type.subtitle,
                    fontSize = 11.5.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceElevated)
                        .clickable(enabled = canMoveUp, onClick = onMoveUp)
                        .testTag("move_up_${module.type.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = if (canMoveUp) palette.textPrimary else palette.textSecondary.copy(alpha = 0.32f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceElevated)
                        .clickable(enabled = canMoveDown, onClick = onMoveDown)
                        .testTag("move_down_${module.type.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = if (canMoveDown) palette.textPrimary else palette.textSecondary.copy(alpha = 0.32f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (!isCoreTimer) {
                    Switch(
                        checked = module.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = palette.onPrimary,
                            checkedTrackColor = palette.primary,
                            uncheckedThumbColor = palette.textSecondary,
                            uncheckedTrackColor = palette.surfaceElevated
                        )
                    )
                }
            }
        }

        AnimatedVisibility(visible = module.enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SIZE",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.5.sp,
                        color = palette.textSecondary
                    )
                    ElasticMorphingSegmentedPill(
                        segments = LockModuleSize.entries.map { ElasticPillSegment(it.shortLabel) },
                        selectedIndex = LockModuleSize.entries.indexOf(module.size),
                        onSelectIndex = { idx -> onSelectSize(LockModuleSize.entries[idx]) },
                        height = 34.dp
                    )
                }

                Column(
                    modifier = Modifier.weight(1.35f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "POSITION",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.5.sp,
                        color = palette.textSecondary
                    )
                    ElasticMorphingSegmentedPill(
                        segments = LockModuleAlignment.entries.map { ElasticPillSegment(it.shortLabel) },
                        selectedIndex = LockModuleAlignment.entries.indexOf(module.alignment),
                        onSelectIndex = { idx -> onSelectAlignment(LockModuleAlignment.entries[idx]) },
                        height = 34.dp
                    )
                }
            }
        }
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

    var name by remember(initialSchedule) { mutableStateOf(initialSchedule?.name ?: "Deep Focus") }
    var startHour by remember(initialSchedule) { mutableIntStateOf(initialSchedule?.startHour ?: 9) }
    var startMinute by remember(initialSchedule) { mutableIntStateOf(initialSchedule?.startMinute ?: 0) }
    var endHour by remember(initialSchedule) { mutableIntStateOf(initialSchedule?.endHour ?: 11) }
    var endMinute by remember(initialSchedule) { mutableIntStateOf(initialSchedule?.endMinute ?: 0) }
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

                // Start & Stop time pickers
                TimeStepperRow(
                    label = "START TIME",
                    hour = startHour,
                    minute = startMinute,
                    onHourChange = { haptics.tick(); startHour = it },
                    onMinuteChange = { haptics.tick(); startMinute = it }
                )

                TimeStepperRow(
                    label = "STOP TIME",
                    hour = endHour,
                    minute = endMinute,
                    onHourChange = { haptics.tick(); endHour = it },
                    onMinuteChange = { haptics.tick(); endMinute = it }
                )

                // Quick duration preset pills relative to Start Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(30 to "30m", 60 to "1h", 90 to "1.5h", 120 to "2h").forEach { (mins, label) ->
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
    onMinuteChange: (Int) -> Unit
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
                )
                Text(
                    text = "${String.format("%02d", hour)}h",
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
                        .clickable { onMinuteChange((minute + 55) % 60) }
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                )
                Text(
                    text = "${String.format("%02d", minute)}m",
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
                        .clickable { onMinuteChange((minute + 5) % 60) }
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
    }
}
