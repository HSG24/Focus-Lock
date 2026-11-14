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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import com.example.Session
import com.example.calculateMaxStreak
import com.example.calculateStreak
import com.example.data.FocusRank
import com.example.data.FocusSettingsRepository
import com.example.data.FocusXpRepository
import com.example.data.FocusXpState
import com.example.fetchAllSessions
import com.example.formatDurationMinutes
import com.example.getFormattedDate
import com.example.isSameDay
import com.example.isSameDayCalendar
import com.example.ui.components.FocusLockEmblem
import com.example.ui.components.ThemeBackdropLayer
import com.example.ui.components.ThemedDivider
import com.example.ui.components.performClockTickHaptic
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import com.example.widget.NothingFocusWidgetUpdater
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

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
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val xpState by FocusXpRepository.observe(context).collectAsStateWithLifecycle()

    var allSessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var isLoadingStats by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInputText by remember(dailyGoalMinutes) {
        mutableStateOf(dailyGoalMinutes.toString())
    }

    val refreshStats = remember(dailyGoalMinutes) {
        {
            coroutineScope.launch {
                if (allSessions.isEmpty()) {
                    isLoadingStats = true
                }
                val loaded = fetchAllSessions()
                allSessions = loaded
                FocusXpRepository.syncWithRemoteSessions(context, loaded, dailyGoalMinutes)
                NothingFocusWidgetUpdater.scheduleUpdateAll(context)
                isLoadingStats = false
            }
            Unit
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

    val today = remember(selectedDate) { Calendar.getInstance() }
    val isSelectedToday = remember(selectedDate, today) { isSameDayCalendar(selectedDate, today) }

    val sessionsForDay = remember(allSessions, selectedDate) {
        allSessions.filter { isSameDay(it.startTime, selectedDate) }
    }
    val totalTimeMs = remember(sessionsForDay) { sessionsForDay.sumOf { it.duration } }
    val sessionCount = sessionsForDay.size
    val avgTimeMs = if (sessionCount > 0) totalTimeMs / sessionCount else 0L
    val longestTimeMs = remember(sessionsForDay) {
        if (sessionsForDay.isNotEmpty()) sessionsForDay.maxOf { it.duration } else 0L
    }
    val currentStreak = remember(allSessions) { calculateStreak(allSessions) }
    val maxStreak = remember(allSessions) { calculateMaxStreak(allSessions) }

    val dailyGoalMs = dailyGoalMinutes.coerceAtLeast(1L) * 60 * 1000L
    val progressFraction = (totalTimeMs.toFloat() / dailyGoalMs.toFloat()).coerceIn(0f, 1f)
    val progressPercentage = (progressFraction * 100).toInt()
    val heroGlowBrush = remember(palette) {
        Brush.radialGradient(
            colors = listOf(
                palette.primaryGlow.copy(alpha = 0.42f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .navigationBarsPadding()
    ) {
        ThemeBackdropLayer(palette = palette)

        Column(modifier = Modifier.fillMaxSize()) {
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
                            fontFamily = palette.displayFont,
                            color = palette.textPrimary
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
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Stats",
                            tint = palette.textPrimary
                        )
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
                            tint = palette.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Date Selector Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border),
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
                            tint = palette.textPrimary
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
                            color = palette.textPrimary,
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
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                            exit = fadeOut() + shrinkHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
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
                                        color = palette.primaryContainer,
                                        border = BorderStroke(1.dp, palette.primaryBorderTint)
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
                                                color = palette.primary
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = palette.primary
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
                            tint = if (isSelectedToday) palette.textPrimary.copy(alpha = 0.22f) else palette.textPrimary
                        )
                    }
                }
            }

            ThemedDivider(palette = palette)

            if (isLoadingStats) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.5.dp,
                        color = palette.primary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Focus Rank & XP Card
                    item(key = "rank_xp_card", contentType = "rank_card") {
                        FocusRankXpCard(
                            xpState = xpState,
                            dailyGoalMinutes = dailyGoalMinutes
                        )
                    }

                    // Hero Total Focus Time Card
                    item(key = "hero_focus_time", contentType = "hero_card") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = palette.surface,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(heroGlowBrush)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 26.dp, horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TOTAL FOCUS TIME",
                                        style = MaterialTheme.typography.labelSmall,
                                        letterSpacing = 1.4.sp,
                                        color = palette.textSecondary
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
                                        fontFamily = palette.numeralFont,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = palette.primaryContainer,
                                        border = BorderStroke(1.dp, palette.primaryBorderTint)
                                    ) {
                                        Text(
                                            text = if (sessionCount == 1) "1 session completed" else "$sessionCount sessions completed",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Daily Focus Goal Card
                    item(key = "daily_goal_card", contentType = "goal_card") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = palette.surface,
                            border = BorderStroke(1.dp, palette.border)
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
                                            tint = if (progressPercentage >= 100) palette.success else palette.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Daily Goal",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 15.sp,
                                            color = palette.textPrimary
                                        )
                                    }
                                    Text(
                                        text = "$progressPercentage%",
                                        fontFamily = palette.numeralFont,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progressPercentage >= 100) palette.success else palette.primary
                                    )
                                }

                                DailyGoalProgressBar(
                                    progressFraction = progressFraction,
                                    goalReached = progressPercentage >= 100
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val goalHours = dailyGoalMinutes / 60
                                    val goalMinutes = dailyGoalMinutes % 60
                                    val goalText = if (goalHours > 0) {
                                        "${goalHours}h" + if (goalMinutes > 0) " ${goalMinutes}m" else ""
                                    } else {
                                        "${goalMinutes}m"
                                    }
                                    Text(
                                        text = "Target: $goalText / day",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "Edit Goal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                view.performKeyTapHaptic()
                                                goalInputText = dailyGoalMinutes.toString()
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
                    item(key = "summary_metrics", contentType = "metrics_row") {
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
                    item(key = "streaks_row", contentType = "streaks_row") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StreakCard(
                                title = "Current Streak",
                                days = currentStreak,
                                icon = Icons.Outlined.LocalFireDepartment,
                                tint = palette.warning,
                                badgeBg = palette.surfaceElevated,
                                modifier = Modifier.weight(1f)
                            )
                            StreakCard(
                                title = "Best Streak",
                                days = maxStreak,
                                icon = Icons.Outlined.EmojiEvents,
                                tint = palette.primary,
                                badgeBg = palette.primaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (xpState.history.isNotEmpty()) {
                        item(key = "xp_history_card", contentType = "xp_history") {
                            XpHistoryLedgerCard(xpState = xpState)
                        }
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

@Composable
private fun FocusRankXpCard(
    xpState: FocusXpState,
    dailyGoalMinutes: Long
) {
    val palette = LocalFocusTheme.current
    val nextRank = remember(xpState.rank) {
        val entries = FocusRank.entries
        val idx = entries.indexOf(xpState.rank)
        if (idx >= 0 && idx < entries.lastIndex) entries[idx + 1] else null
    }

    val goalMs = dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
    val remainingMs = (goalMs - xpState.todayFocusMs).coerceAtLeast(0L)
    val goalSafe = remainingMs == 0L

    val animatedXpProgress by animateFloatAsState(
        targetValue = xpState.progressToNextRank.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "xp_rank_progress"
    )
    val xpBarBrush = remember(palette) {
        Brush.horizontalGradient(listOf(palette.primaryDeep, palette.primary))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("focus_rank_xp_card"),
        shape = RoundedCornerShape(22.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.primaryContainer)
                            .border(1.dp, palette.primaryBorderTint, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MilitaryTech,
                            contentDescription = xpState.rank.title,
                            tint = palette.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = xpState.rank.title.uppercase(Locale.US),
                            fontFamily = palette.displayFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = xpState.rank.badgeCode,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.5.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.primaryContainer,
                    border = BorderStroke(1.dp, palette.primaryBorderTint)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${xpState.totalXp} XP",
                            fontFamily = palette.numeralFont,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                    }
                }
            }

            // Progress Bar to next Rank
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.surfaceElevated)
                ) {
                    if (animatedXpProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    scaleX = animatedXpProgress.coerceAtLeast(0.03f)
                                }
                                .clip(RoundedCornerShape(4.dp))
                                .background(xpBarBrush)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (nextRank != null) {
                            "${xpState.xpToNextRank} XP to ${nextRank.title}"
                        } else {
                            "MAX RANK ACHIEVED"
                        },
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "+10 XP/min · +100 goal bonus",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.5.sp,
                        color = palette.textSecondary
                    )
                }
            }

            ThemedDivider(palette = palette)

            // Today's Goal Status Pill: Safe vs. Penalty Risk
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (goalSafe) palette.success.copy(alpha = 0.12f) else palette.primaryContainer,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (goalSafe) palette.success.copy(alpha = 0.35f) else palette.primaryBorderTint
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_goal_penalty_status")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (goalSafe) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (goalSafe) palette.success else palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    val remainingMins = (remainingMs / 60_000L).coerceAtLeast(1L)
                    val remHrs = remainingMins / 60L
                    val remMins = remainingMins % 60L
                    val remStr = if (remHrs > 0) "${remHrs}h ${remMins}m" else "${remMins}m"

                    Text(
                        text = if (goalSafe) {
                            "SAFE · Today's Goal Met (+100 XP)"
                        } else {
                            "PENALTY RISK · $remStr left before midnight (-150 XP)"
                        },
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (goalSafe) palette.success else palette.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun XpHistoryLedgerCard(xpState: FocusXpState) {
    val palette = LocalFocusTheme.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "RECENT XP LEDGER",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.2.sp,
                color = palette.textSecondary
            )

            xpState.history.take(5).forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.5.sp,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    val isPositive = entry.deltaXp >= 0
                    Text(
                        text = if (isPositive) "+${entry.deltaXp} XP" else "${entry.deltaXp} XP",
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) palette.success else palette.primary
                    )
                }
                if (index < xpState.history.take(5).lastIndex) {
                    ThemedDivider(palette = palette)
                }
            }
        }
    }
}

@Composable
private fun DailyGoalProgressBar(
    progressFraction: Float,
    goalReached: Boolean
) {
    val palette = LocalFocusTheme.current
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "daily_goal_progress"
    )
    val primaryBrush = remember(palette) {
        Brush.horizontalGradient(listOf(palette.primaryDeep, palette.primary))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(palette.surfaceElevated)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        scaleX = animatedProgress.coerceAtLeast(0.02f)
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (goalReached) {
                            Modifier.background(palette.success)
                        } else {
                            Modifier.background(primaryBrush)
                        }
                    )
            )
        }
    }
}

@Composable
private fun StatMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontFamily = palette.numeralFont,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
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
    val palette = LocalFocusTheme.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border)
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
                    .border(1.dp, palette.border, CircleShape),
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
                    fontFamily = palette.numeralFont,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = palette.textSecondary,
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
