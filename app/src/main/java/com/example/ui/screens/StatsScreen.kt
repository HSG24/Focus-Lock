package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.Session
import com.example.calculateMaxStreak
import com.example.calculateStreak
import com.example.data.FocusSettingsRepository
import com.example.data.FocusSettingsState
import com.example.fetchAllSessions
import com.example.formatDurationMinutes
import com.example.getFormattedDate
import com.example.isSameDay
import com.example.isSameDayCalendar
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.performClockTickHaptic
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.CrimsonBorderTint
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateGray
import com.example.ui.theme.StreakAmber
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    settingsState: FocusSettingsState,
    onOpenShare: (Long?) -> Unit,
    isLiveShareActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var allSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var isLoadingStats by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInputText by remember(settingsState.dailyGoalMinutes) {
        mutableStateOf(settingsState.dailyGoalMinutes.toString())
    }

    val refreshStats = {
        coroutineScope.launch {
            isLoadingStats = true
            allSessions = fetchAllSessions()
            isLoadingStats = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val today = Calendar.getInstance()
    val isSelectedToday = isSameDayCalendar(selectedDate, today)

    val sessionsForDay = remember(allSessions, selectedDate) {
        allSessions.filter { isSameDay(it.startTime, selectedDate) }
    }
    val totalTimeMs = sessionsForDay.sumOf { it.duration }
    val sessionCount = sessionsForDay.size
    val avgTimeMs = if (sessionCount > 0) totalTimeMs / sessionCount else 0L
    val longestTimeMs = if (sessionCount > 0) sessionsForDay.maxOf { it.duration } else 0L
    val currentStreak = remember(allSessions) { calculateStreak(allSessions) }
    val maxStreak = remember(allSessions) { calculateMaxStreak(allSessions) }

    val dailyGoalMs = settingsState.dailyGoalMinutes * 60 * 1000L
    val progressFraction = (totalTimeMs.toFloat() / dailyGoalMs.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessLow
        ),
        label = "daily_goal_progress"
    )
    val progressPercentage = (progressFraction * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusLockEmblem(
                        size = 26.dp,
                        showAmbientGlow = false
                    )
                    Text(
                        text = "Focus Insights",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureWhite
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        view.performKeyTapHaptic()
                        onOpenShare(selectedDate.timeInMillis)
                    },
                    modifier = Modifier.testTag("stats_share_button")
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Stats",
                            tint = PureWhite
                        )
                        if (isLiveShareActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(FocusEmerald)
                                    .border(
                                        width = 1.dp,
                                        color = OledBlack,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        view.performKeyTapHaptic()
                        showDatePicker = true
                    },
                    modifier = Modifier.testTag("calendar_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Select Custom Date",
                        tint = SlateGray
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = OledBlack
            )
        )

        // Obsidian Date Selector Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        view.performClockTickHaptic()
                        selectedDate = (selectedDate.clone() as Calendar).apply {
                            add(Calendar.DATE, -1)
                        }
                    },
                    modifier = Modifier.testTag("prev_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day",
                        modifier = Modifier.size(20.dp),
                        tint = PureWhite
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getFormattedDate(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                view.performKeyTapHaptic()
                                showDatePicker = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )

                    AnimatedVisibility(
                        visible = !isSelectedToday,
                        enter = fadeIn() + expandHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = fadeOut() + shrinkHorizontally(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        view.performClockTickHaptic()
                                        selectedDate = Calendar.getInstance()
                                    }
                                    .padding(horizontal = 4.dp)
                                    .testTag("jump_today_button")
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = CrimsonContainer,
                                    border = BorderStroke(1.dp, CrimsonBorderTint)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CrimsonPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = CrimsonPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (!isSelectedToday) {
                            view.performClockTickHaptic()
                            selectedDate = (selectedDate.clone() as Calendar).apply {
                                add(Calendar.DATE, 1)
                            }
                        }
                    },
                    enabled = !isSelectedToday,
                    modifier = Modifier.testTag("next_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day",
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelectedToday) PureWhite.copy(alpha = 0.22f) else PureWhite
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = ObsidianBorder
        )

        if (isLoadingStats) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.5.dp,
                    color = CrimsonPrimary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Total Focus Time Card with subtle crimson radial sheen
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            CrimsonGlow.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "TOTAL FOCUS TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.4.sp,
                                    color = SlateGray
                                )

                                val hours = totalTimeMs / (1000 * 60 * 60)
                                val minutes = (totalTimeMs % (1000 * 60 * 60)) / (1000 * 60)
                                val seconds = (totalTimeMs % (1000 * 60)) / 1000
                                val formattedTime = when {
                                    hours > 0 -> "${hours}h ${minutes}m"
                                    minutes > 0 -> "${minutes}m ${seconds}s"
                                    else -> "${seconds}s"
                                }

                                Text(
                                    text = if (totalTimeMs > 0) formattedTime else "0s",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = CrimsonContainer,
                                    border = BorderStroke(1.dp, CrimsonBorderTint)
                                ) {
                                    Text(
                                        text = if (sessionCount == 1) "1 session completed" else "$sessionCount sessions completed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PureWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Daily Focus Goal Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = if (progressPercentage >= 100) FocusEmerald else CrimsonPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Daily Goal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 15.sp,
                                        color = PureWhite
                                    )
                                }
                                Text(
                                    text = "$progressPercentage%",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (progressPercentage >= 100) FocusEmerald else CrimsonPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ObsidianElevated)
                            ) {
                                if (animatedProgress > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedProgress.coerceAtLeast(0.02f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (progressPercentage >= 100) {
                                                    Brush.horizontalGradient(listOf(FocusEmerald, FocusEmerald))
                                                } else {
                                                    Brush.horizontalGradient(listOf(CrimsonDeep, CrimsonPrimary))
                                                }
                                            )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val goalHours = settingsState.dailyGoalMinutes / 60
                                val goalMinutes = settingsState.dailyGoalMinutes % 60
                                val goalText = if (goalHours > 0) {
                                    "${goalHours}h" + if (goalMinutes > 0) " ${goalMinutes}m" else ""
                                } else {
                                    "${goalMinutes}m"
                                }
                                Text(
                                    text = "Target: $goalText / day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateGray
                                )
                                Text(
                                    text = "Edit Goal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CrimsonPrimary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            view.performKeyTapHaptic()
                                            goalInputText = settingsState.dailyGoalMinutes.toString()
                                            showGoalDialog = true
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .testTag("edit_goal_button")
                                )
                            }
                        }
                    }
                }

                // 3-Column Summary Metrics
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMiniCard(
                            label = "Sessions",
                            value = "$sessionCount",
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "Average",
                            value = formatDurationMinutes(avgTimeMs),
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            label = "Longest",
                            value = formatDurationMinutes(longestTimeMs),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Streaks Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StreakCard(
                            title = "Current Streak",
                            days = currentStreak,
                            icon = Icons.Outlined.LocalFireDepartment,
                            tint = StreakAmber,
                            badgeBg = ObsidianElevated,
                            modifier = Modifier.weight(1f)
                        )
                        StreakCard(
                            title = "Best Streak",
                            days = maxStreak,
                            icon = Icons.Outlined.EmojiEvents,
                            tint = CrimsonPrimary,
                            badgeBg = CrimsonContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        SleekDatePickerDialog(
            onDateSelected = { selectedDateMillis ->
                if (selectedDateMillis != null) {
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
            containerColor = ObsidianSurface,
            titleContentColor = PureWhite,
            textContentColor = SlateGray,
            title = { Text("Daily Focus Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Set your daily target focus duration (minutes):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGray
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
                        val parsed = goalInputText.toLongOrNull() ?: 60L
                        if (parsed > 0) {
                            FocusSettingsRepository.setDailyGoalMinutes(context, parsed)
                        }
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonPrimary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", color = SlateGray)
                }
            }
        )
    }
}

@Composable
private fun StatMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SlateGray
            )
        }
    }
}

@Composable
private fun StreakCard(
    title: String,
    days: Int,
    icon: ImageVector,
    tint: Color,
    badgeBg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeBg)
                    .border(1.dp, ObsidianBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$days ${if (days == 1) "day" else "days"}",
                    fontFamily = JetBrainsMono,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleekDatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(containerColor = ObsidianSurface),
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    onDismiss()
                }
            ) {
                Text("OK", color = CrimsonPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateGray)
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(containerColor = ObsidianSurface)
        )
    }
}
