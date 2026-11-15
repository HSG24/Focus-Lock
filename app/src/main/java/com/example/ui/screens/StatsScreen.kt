package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import com.example.data.StatsTimeframe
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.RankXpBottomSheet
import com.example.ui.components.StatsDateNavigatorSection
import com.example.ui.components.StatsSyncStatusBadge
import com.example.ui.components.StatsTimeframeSwitcher
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemedDivider
import com.example.ui.components.TopBarRankXpPill
import com.example.ui.components.UnifiedBentoMetricsGrid
import com.example.ui.components.UnifiedHeroInfographicCard
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.LocalFocusTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    dailyGoalMinutes: Long,
    onOpenShare: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val palette = LocalFocusTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val statsState by FocusStatsRepository.observe(context).collectAsStateWithLifecycle()
    val xpState by FocusXpRepository.observe(context).collectAsStateWithLifecycle()

    var selectedTimeframe by remember { mutableStateOf(StatsTimeframe.DAY) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedBucketIndex by remember { mutableStateOf<Int?>(null) }

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Sleek Compact Top Header Bar with Emblem, Title, Sync Indicator, Rank/XP Pill, Share & Calendar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusLockEmblem(
                        size = 24.dp,
                        showAmbientGlow = false
                    )
                    Column {
                        Text(
                            text = "Focus Insights",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = palette.displayFont,
                            fontSize = 18.sp,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatsSyncStatusBadge(
                            syncStatus = statsState.syncStatus,
                            pendingUploadCount = statsState.pendingUploadCount
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    TopBarRankXpPill(
                        xpState = xpState,
                        onClick = {
                            view.performKeyTapHaptic()
                            selectedBucketIndex = null
                            showRankXpSheet = true
                        },
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    IconButton(
                        onClick = {
                            view.performKeyTapHaptic()
                            selectedBucketIndex = null
                            onOpenShare(selectedDate.timeInMillis)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("stats_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Stats",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            view.performKeyTapHaptic()
                            selectedBucketIndex = null
                            showDatePicker = true
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("calendar_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Select Custom Date",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            ThemedDivider(palette = palette)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
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
                            selectedDate = (selectedDate.clone() as Calendar).apply {
                                when (selectedTimeframe) {
                                    StatsTimeframe.DAY -> add(Calendar.DATE, -1)
                                    StatsTimeframe.WEEK -> add(Calendar.DATE, -7)
                                    StatsTimeframe.MONTH -> add(Calendar.MONTH, -1)
                                }
                            }
                        },
                        onStepForward = {
                            if (report.canStepForward) {
                                selectedBucketIndex = null
                                val now = Calendar.getInstance()
                                val candidate = (selectedDate.clone() as Calendar).apply {
                                    when (selectedTimeframe) {
                                        StatsTimeframe.DAY -> add(Calendar.DATE, 1)
                                        StatsTimeframe.WEEK -> add(Calendar.DATE, 7)
                                        StatsTimeframe.MONTH -> add(Calendar.MONTH, 1)
                                    }
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
                        onSelectStripDay = { dayMillis ->
                            selectedBucketIndex = null
                            selectedDate = Calendar.getInstance().apply {
                                timeInMillis = dayMillis
                            }
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
                            selectedBucketIndex = if (idx == null || selectedBucketIndex == idx) null else idx
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
            onDateSelected = { selectedDateMillis ->
                if (selectedDateMillis != null) {
                    selectedBucketIndex = null
                    selectedDate = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                    }
                }
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Daily Focus Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Set your daily target focus duration (minutes):",
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        view.performKeyTapHaptic()
                        val parsed = goalInputText.toLongOrNull() ?: 240L
                        if (parsed > 0) {
                            FocusSettingsRepository.setDailyGoalMinutes(context, parsed)
                        }
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleekDatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(containerColor = palette.surface),
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
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
