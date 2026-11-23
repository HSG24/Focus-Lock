package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FocusSettingsRepository
import com.example.data.FocusStatsRepository
import com.example.data.FocusXpRepository
import com.example.data.ShareRepository
import com.example.data.StatsSyncStatus
import com.example.data.StatsTimeframe
import com.example.ui.components.FocusHeatmapCard
import com.example.ui.components.RankXpBottomSheet
import com.example.ui.components.ScenicPageVariant
import com.example.ui.components.StatsDateNavigatorSection
import com.example.ui.components.StatsSyncStatusBadge
import com.example.ui.components.StatsTimeframeSwitcher
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemeScenicBackdropOverlay
import com.example.ui.components.TopBarRankXpPill
import com.example.ui.components.UnifiedBentoMetricsGrid
import com.example.ui.components.UnifiedHeroInfographicCard
import com.example.ui.components.focusGlassPanel
import com.example.ui.components.rememberFocusHaptics
import com.example.ui.components.rememberSpringPressScale
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    dailyGoalMinutes: Long,
    onOpenShare: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberFocusHaptics()
    val palette = LocalFocusTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val statsState by FocusStatsRepository.observe(context).collectAsStateWithLifecycle()
    val xpState by FocusXpRepository.observe(context).collectAsStateWithLifecycle()

    var selectedTimeframe by remember { mutableStateOf(StatsTimeframe.DAY) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedBucketIndex by remember { mutableStateOf<Int?>(null) }
    var isManualRefreshing by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showRankXpSheet by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInputText by remember(dailyGoalMinutes) {
        mutableStateOf(dailyGoalMinutes.toString())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                FocusStatsRepository.refreshNow(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val report = remember(
        statsState.sessions,
        selectedTimeframe,
        selectedDate,
        dailyGoalMinutes
    ) {
        FocusStatsRepository.buildPeriodReport(
            sessions = statsState.sessions,
            timeframe = selectedTimeframe,
            selectedDate = selectedDate,
            dailyGoalMinutes = dailyGoalMinutes
        )
    }

    val heatmapData = remember(
        statsState.sessions,
        dailyGoalMinutes
    ) {
        FocusStatsRepository.buildHeatmapData(
            sessions = statsState.sessions,
            dailyGoalMinutes = dailyGoalMinutes,
            weeksCount = 14
        )
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures {
                    selectedBucketIndex = null
                }
            }
            .testTag("stats_screen_root")
    ) {
        ThemeBackdropLayer(palette = palette)
        ThemeScenicBackdropOverlay(
            variant = ScenicPageVariant.STATS,
            palette = palette
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Calm, minimal Fitbit-inspired top header with circular pill buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "Stats",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = palette.displayFont,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatsSyncStatusBadge(
                        syncStatus = statsState.syncStatus,
                        pendingUploadCount = statsState.pendingUploadCount
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TopBarRankXpPill(
                        xpState = xpState,
                        onClick = {
                            haptics.tap()
                            selectedBucketIndex = null
                            showRankXpSheet = true
                        }
                    )

                    StatsHeaderCircleButton(
                        icon = Icons.Outlined.Share,
                        contentDescription = "Share Stats",
                        onClick = {
                            haptics.tap()
                            selectedBucketIndex = null
                            onOpenShare(selectedDate.timeInMillis)
                        },
                        modifier = Modifier.testTag("stats_share_button")
                    )

                    StatsHeaderCircleButton(
                        icon = Icons.Outlined.CalendarMonth,
                        contentDescription = "Select Custom Date",
                        onClick = {
                            haptics.tap()
                            selectedBucketIndex = null
                            showDatePicker = true
                        },
                        modifier = Modifier.testTag("calendar_picker_button")
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = isManualRefreshing || statsState.syncStatus == StatsSyncStatus.SYNCING,
                onRefresh = {
                    haptics.gestureStart()
                    isManualRefreshing = true
                    FocusStatsRepository.refreshNow(context)
                    coroutineScope.launch {
                        delay(550L)
                        isManualRefreshing = false
                        haptics.confirm()
                    }
                },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 6.dp,
                        end = 20.dp,
                        bottom = 116.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "timeframe_switcher", contentType = "switcher") {
                        StatsTimeframeSwitcher(
                            selectedTimeframe = selectedTimeframe,
                            onSelectTimeframe = { newTimeframe ->
                                selectedTimeframe = newTimeframe
                                selectedBucketIndex = null
                            },
                            modifier = Modifier.widthIn(max = 600.dp)
                        )
                    }

                    item(key = "date_navigator_strip", contentType = "date_nav") {
                        StatsDateNavigatorSection(
                            report = report,
                            onStepBackward = {
                                selectedBucketIndex = null
                                selectedDate = when (selectedTimeframe) {
                                    StatsTimeframe.DAY -> (selectedDate.clone() as Calendar).apply { add(Calendar.DATE, -1) }
                                    StatsTimeframe.WEEK -> FocusStatsRepository.stepMonthClippedWeekBackward(selectedDate)
                                    StatsTimeframe.MONTH -> (selectedDate.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                                }
                            },
                            onStepForward = {
                                if (report.canStepForward) {
                                    selectedBucketIndex = null
                                    val now = Calendar.getInstance()
                                    val candidate = when (selectedTimeframe) {
                                        StatsTimeframe.DAY -> (selectedDate.clone() as Calendar).apply { add(Calendar.DATE, 1) }
                                        StatsTimeframe.WEEK -> FocusStatsRepository.stepMonthClippedWeekForward(selectedDate, now)
                                        StatsTimeframe.MONTH -> (selectedDate.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                                    }
                                    selectedDate = if (candidate.after(now)) now else candidate
                                }
                            },
                            onJumpToToday = {
                                selectedBucketIndex = null
                                selectedDate = Calendar.getInstance()
                            },
                            onOpenCalendarPicker = {
                                showDatePicker = true
                            },
                            modifier = Modifier.widthIn(max = 600.dp)
                        )
                    }

                    item(key = "unified_hero_infographic", contentType = "hero_infographic") {
                        UnifiedHeroInfographicCard(
                            report = report,
                            dailyGoalMinutes = dailyGoalMinutes,
                            selectedBucketIndex = selectedBucketIndex,
                            onSelectBucket = { idx ->
                                if (idx == null) {
                                    selectedBucketIndex = null
                                } else if ((report.chartBuckets.getOrNull(idx)?.durationMs ?: 0L) > 0L) {
                                    selectedBucketIndex = if (selectedBucketIndex == idx) null else idx
                                }
                            },
                            onScrubBucket = { idx ->
                                if ((report.chartBuckets.getOrNull(idx)?.durationMs ?: 0L) > 0L) {
                                    selectedBucketIndex = idx
                                }
                            },
                            onEditGoal = {
                                goalInputText = dailyGoalMinutes.toString()
                                showGoalDialog = true
                            },
                            modifier = Modifier.widthIn(max = 600.dp)
                        )
                    }

                    item(key = "bento_metrics_grid", contentType = "bento_grid") {
                        val selectedBucket = selectedBucketIndex?.let { idx ->
                            report.chartBuckets.getOrNull(idx)
                        }
                        UnifiedBentoMetricsGrid(
                            report = report,
                            selectedBucket = selectedBucket,
                            modifier = Modifier.widthIn(max = 600.dp)
                        )
                    }

                    item(key = "focus_heatmap_card", contentType = "heatmap") {
                        FocusHeatmapCard(
                            heatmap = heatmapData,
                            onJumpToDay = { dayMillis ->
                                selectedBucketIndex = null
                                selectedTimeframe = StatsTimeframe.DAY
                                selectedDate = Calendar.getInstance().apply {
                                    timeInMillis = dayMillis
                                }
                            },
                            modifier = Modifier.widthIn(max = 600.dp)
                        )
                    }
                }
            }
        }
    }

    if (showRankXpSheet) {
        RankXpBottomSheet(
            xpState = xpState,
            dailyGoalMinutes = dailyGoalMinutes,
            onDismiss = { showRankXpSheet = false }
        )
    }

    if (showDatePicker) {
        SleekDatePickerDialog(
            initialLocalDateMs = selectedDate.timeInMillis,
            onDateSelected = { localStartOfDayMillis ->
                if (localStartOfDayMillis != null) {
                    haptics.confirm()
                    selectedBucketIndex = null
                    selectedDate = Calendar.getInstance().apply {
                        timeInMillis = localStartOfDayMillis
                    }
                }
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            shape = RoundedCornerShape(30.dp),
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Daily Focus Goal", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Target focus duration in minutes:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = goalInputText,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                goalInputText = newValue
                            }
                        },
                        label = { Text("Minutes") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.confirm()
                        val parsed = goalInputText.toLongOrNull() ?: 240L
                        if (parsed > 0) {
                            FocusSettingsRepository.setDailyGoalMinutes(context, parsed)
                        }
                        showGoalDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptics.tap()
                        showGoalDialog = false
                    }
                ) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatsHeaderCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.90f)

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .focusGlassPanel(shape = CircleShape, palette = palette),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = palette.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleekDatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    initialLocalDateMs: Long = System.currentTimeMillis()
) {
    val palette = LocalFocusTheme.current
    val todayUtcMillis = remember {
        ShareRepository.localStartOfDayToUtcPickerMillis(
            ShareRepository.startOfDayMs(System.currentTimeMillis())
        )
    }
    val initialUtcMillis = remember(initialLocalDateMs) {
        ShareRepository.localStartOfDayToUtcPickerMillis(
            ShareRepository.startOfDayMs(initialLocalDateMs)
        )
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= todayUtcMillis
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(containerColor = palette.surface),
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedUtc = datePickerState.selectedDateMillis
                    val localMs = selectedUtc?.let { ShareRepository.utcDatePickerMillisToLocalStartOfDay(it) }
                    onDateSelected(localMs)
                    onDismiss()
                }
            ) {
                Text("OK", color = palette.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textSecondary)
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(containerColor = palette.surface)
        )
    }
}
