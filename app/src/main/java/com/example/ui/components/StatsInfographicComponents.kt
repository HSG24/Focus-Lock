package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusRank
import com.example.data.FocusStatsRepository
import com.example.data.FocusXpState
import com.example.data.StatsChartBucket
import com.example.data.StatsPeriodReport
import com.example.data.StatsSyncStatus
import com.example.data.StatsTimeframe
import com.example.formatDurationMinutes
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun StatsSyncStatusBadge(
    syncStatus: StatsSyncStatus,
    pendingUploadCount: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    AnimatedVisibility(
        visible = syncStatus != StatsSyncStatus.SYNCED,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val isOffline = syncStatus == StatsSyncStatus.OFFLINE_CACHED
        val dotColor = if (isOffline) palette.warning else palette.primary
        val labelText = when {
            isOffline && pendingUploadCount > 0 -> "Offline · $pendingUploadCount queued"
            isOffline -> "Offline · Cached"
            else -> "Syncing..."
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .padding(top = 2.dp)
                .testTag("stats_sync_indicator")
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = labelText,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TopBarRankXpPill(
    xpState: FocusXpState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberSpringPressScale(interactionSource, pressedScale = 0.94f)
    val animatedProgress by animateFloatAsState(
        targetValue = xpState.progressToNextRank.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "top_bar_xp_progress"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("top_bar_rank_xp_pill"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = palette.surface,
            border = BorderStroke(0.75.dp, palette.border)
        ) {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = "Focus Rank and XP",
                        tint = palette.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${xpState.rank.title.uppercase(Locale.US)} · ${xpState.totalXp} XP",
                        fontFamily = palette.numeralFont,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(palette.surfaceElevated)
                ) {
                    if (animatedProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    scaleX = animatedProgress.coerceAtLeast(0.05f)
                                }
                                .background(palette.primary)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankXpBottomSheet(
    xpState: FocusXpState,
    dailyGoalMinutes: Long,
    onDismiss: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
        label = "sheet_xp_progress"
    )
    val barBrush = remember(palette) {
        Brush.horizontalGradient(listOf(palette.primaryDeep, palette.primary))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.textPrimary,
        modifier = Modifier.testTag("rank_xp_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Rank & Total XP Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.primaryContainer)
                            .border(1.dp, palette.primaryBorderTint, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MilitaryTech,
                            contentDescription = xpState.rank.title,
                            tint = palette.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = xpState.rank.title.uppercase(Locale.US),
                            fontFamily = palette.displayFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = xpState.rank.badgeCode,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${xpState.totalXp} XP",
                            fontFamily = palette.numeralFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                    }
                }
            }

            // XP Progress Bar + Rules
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp))
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
                                .clip(RoundedCornerShape(5.dp))
                                .background(barBrush)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (nextRank != null) {
                            "${xpState.xpToNextRank} XP to ${nextRank.title}"
                        } else {
                            "MAX RANK ACHIEVED"
                        },
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "+10 XP/min • +100 goal bonus",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.5.sp,
                        color = palette.textSecondary
                    )
                }
            }

            // Active Daily Goal Penalty Countdown Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
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
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (goalSafe) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (goalSafe) palette.success else palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    val remainingMins = (remainingMs / 60_000L).coerceAtLeast(1L)
                    val remHrs = remainingMins / 60L
                    val remMins = remainingMins % 60L
                    val remStr = if (remHrs > 0) "${remHrs}h ${remMins}m" else "${remMins}m"

                    Text(
                        text = if (goalSafe) {
                            "SAFE • Today's Goal Met (+100 XP bonus)"
                        } else {
                            "$remStr left before -150 XP midnight penalty"
                        },
                        fontFamily = JetBrainsMono,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (goalSafe) palette.success else palette.primary
                    )
                }
            }

            ThemedDivider(palette = palette)

            // Full 6-Tier Rank Ladder
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RANK LADDER • 6 TIERS",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.1.sp,
                    color = palette.textSecondary
                )

                val currentOrdinal = xpState.rank.ordinal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.background.copy(alpha = 0.55f))
                        .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                ) {
                    FocusRank.entries.forEachIndexed { index, tier ->
                        val isCurrent = tier == xpState.rank
                        val isPast = index < currentOrdinal
                        val thresholdLabel = if (tier.nextTierXp != null) {
                            "${tier.minXp} – ${tier.nextTierXp - 1} XP"
                        } else {
                            "${tier.minXp}+ XP"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCurrent) palette.primaryContainer.copy(alpha = 0.7f) else Color.Transparent
                                )
                                .then(if (isPast) Modifier.graphicsLayer { alpha = 0.42f } else Modifier)
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                                .testTag("rank_tier_${tier.name.lowercase(Locale.US)}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        isCurrent -> Icons.Outlined.MilitaryTech
                                        isPast -> Icons.Outlined.CheckCircle
                                        else -> Icons.Outlined.Lock
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isCurrent -> palette.primary
                                        isPast -> palette.textSecondary.copy(alpha = 0.6f)
                                        else -> palette.textSecondary.copy(alpha = 0.65f)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = tier.title.uppercase(Locale.US),
                                            fontFamily = palette.displayFont,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) palette.textPrimary else palette.textSecondary
                                        )
                                        if (isCurrent) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = palette.primary
                                            ) {
                                                Text(
                                                    text = "CURRENT",
                                                    fontFamily = JetBrainsMono,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = tier.badgeCode,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            Text(
                                text = thresholdLabel,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) palette.primary else palette.textSecondary
                            )
                        }

                        if (index < FocusRank.entries.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(palette.border.copy(alpha = 0.75f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsTimeframeSwitcher(
    selectedTimeframe: StatsTimeframe,
    onSelectTimeframe: (StatsTimeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        border = BorderStroke(0.75.dp, palette.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatsTimeframe.entries.forEach { timeframe ->
                val isSelected = timeframe == selectedTimeframe
                val tag = when (timeframe) {
                    StatsTimeframe.DAY -> "timeframe_day_tab"
                    StatsTimeframe.WEEK -> "timeframe_week_tab"
                    StatsTimeframe.MONTH -> "timeframe_month_tab"
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) palette.primary else Color.Transparent)
                        .clickable {
                            if (!isSelected) {
                                haptics.segment()
                                onSelectTimeframe(timeframe)
                            }
                        }
                        .testTag(tag),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeframe.label,
                        fontFamily = palette.displayFont,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = if (isSelected) palette.onPrimary else palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun StatsDateNavigatorSection(
    report: StatsPeriodReport,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onJumpToToday: () -> Unit,
    onOpenCalendarPicker: () -> Unit,
    onSelectStripDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val view = LocalView.current
    val currentCanForward by rememberUpdatedState(report.canStepForward)
    val currentOnBack by rememberUpdatedState(onStepBackward)
    val currentOnFwd by rememberUpdatedState(onStepForward)
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(0.75.dp, palette.border),
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulated += dragAmount
                    },
                    onDragEnd = {
                        val threshold = 72f
                        when {
                            dragAccumulated > threshold -> {
                                haptics.segment()
                                currentOnBack()
                            }
                            dragAccumulated < -threshold && currentCanForward -> {
                                haptics.segment()
                                currentOnFwd()
                            }
                        }
                        dragAccumulated = 0f
                    },
                    onDragCancel = { dragAccumulated = 0f }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        view.performClockTickHaptic()
                        onStepBackward()
                    },
                    modifier = Modifier.testTag("prev_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Period",
                        modifier = Modifier.size(18.dp),
                        tint = palette.textPrimary
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.headerDateLabel,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                view.performKeyTapHaptic()
                                onOpenCalendarPicker()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )

                    AnimatedVisibility(visible = !report.isCurrentPeriod) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    view.performClockTickHaptic()
                                    onJumpToToday()
                                }
                                .padding(horizontal = 2.dp)
                                .testTag("jump_today_button")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.primaryContainer,
                                border = BorderStroke(1.dp, palette.primaryBorderTint)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    val jumpLabel = when (report.timeframe) {
                                        StatsTimeframe.DAY -> "TODAY"
                                        StatsTimeframe.WEEK, StatsTimeframe.MONTH -> "CURRENT"
                                    }
                                    Text(
                                        text = jumpLabel,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (report.canStepForward) {
                            view.performClockTickHaptic()
                            onStepForward()
                        }
                    },
                    enabled = report.canStepForward,
                    modifier = Modifier.testTag("next_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Period",
                        modifier = Modifier.size(18.dp),
                        tint = if (report.canStepForward) {
                            palette.textPrimary
                        } else {
                            palette.textPrimary.copy(alpha = 0.22f)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = report.timeframe == StatsTimeframe.DAY,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(0.5.dp)
                            .background(palette.border.copy(alpha = 0.65f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        report.weekStrip.forEachIndexed { idx, day ->
                            val isSelectedHighlight = day.isSelected
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelectedHighlight -> palette.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(
                                        if (isSelectedHighlight) {
                                            Modifier.border(
                                                1.dp,
                                                palette.primaryBorderTint,
                                                RoundedCornerShape(10.dp)
                                            )
                                        } else if (day.isToday) {
                                            Modifier.border(
                                                1.dp,
                                                palette.borderHighlight,
                                                RoundedCornerShape(10.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(enabled = !day.isFuture) {
                                        view.performClockTickHaptic()
                                        onSelectStripDay(day.calendarMillis)
                                    }
                                    .padding(vertical = 3.dp)
                                    .testTag("week_strip_day_$idx")
                            ) {
                                Text(
                                    text = day.dayLetter,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        day.isFuture -> palette.textSecondary.copy(alpha = 0.35f)
                                        isSelectedHighlight -> palette.primary
                                        else -> palette.textSecondary
                                    }
                                )
                                Text(
                                    text = day.dayNumber,
                                    fontFamily = palette.numeralFont,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelectedHighlight || day.isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        day.isFuture -> palette.textSecondary.copy(alpha = 0.35f)
                                        isSelectedHighlight -> palette.primary
                                        else -> palette.textPrimary
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                day.hasFocusData && isSelectedHighlight -> palette.primary
                                                day.hasFocusData -> palette.primary.copy(alpha = 0.72f)
                                                else -> Color.Transparent
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedHeroInfographicCard(
    report: StatsPeriodReport,
    dailyGoalMinutes: Long,
    selectedBucketIndex: Int?,
    onSelectBucket: (Int?) -> Unit,
    onScrubBucket: (Int) -> Unit = onSelectBucket,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val view = LocalView.current
    val heroGlowBrush = remember(palette) {
        Brush.radialGradient(
            colors = listOf(
                palette.primaryGlow.copy(alpha = 0.24f),
                Color.Transparent
            )
        )
    }

    val animatedRingProgress by animateFloatAsState(
        targetValue = report.progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "hero_goal_ring_progress"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        border = BorderStroke(0.75.dp, palette.border),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_infographic_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGlowBrush)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Half: Split Hero (Left = Total Focus + Status, Right = Theme Goal Ring + Edit Goal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (report.timeframe) {
                            StatsTimeframe.DAY -> "TOTAL FOCUS TIME"
                            StatsTimeframe.WEEK -> "WEEKLY FOCUS TIME"
                            StatsTimeframe.MONTH -> "MONTHLY FOCUS TIME"
                        },
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = palette.textSecondary
                    )

                    Text(
                        text = FocusStatsRepository.formatDurationHero(report.totalFocusMs),
                        fontFamily = palette.numeralFont,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val sessionWord = if (report.sessionCount == 1) "1 session" else "${report.sessionCount} sessions"
                    val statusSuffix = if (report.isGoalMet) {
                        "Goal Completed"
                    } else {
                        "${FocusStatsRepository.formatDurationShort(report.remainingToGoalMs)} left for goal"
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (report.isGoalMet) {
                            palette.success.copy(alpha = 0.14f)
                        } else {
                            palette.primaryContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            if (report.isGoalMet) palette.success.copy(alpha = 0.4f) else palette.primaryBorderTint
                        )
                    ) {
                        Text(
                            text = "$sessionWord • $statusSuffix",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (report.isGoalMet) palette.success else palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Right: Goal Ring + Compact Target Edit Trigger
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactGoalGaugeRing(
                        progress = animatedRingProgress,
                        percent = report.progressPercent,
                        goalMet = report.isGoalMet,
                        palette = palette,
                        size = 82.dp
                    )

                    val goalHrs = dailyGoalMinutes / 60
                    val goalMins = dailyGoalMinutes % 60
                    val goalShort = if (goalHrs > 0) {
                        "${goalHrs}h" + if (goalMins > 0) "${goalMins}m" else ""
                    } else {
                        "${goalMins}m"
                    }
                    val targetCaption = when (report.timeframe) {
                        StatsTimeframe.DAY -> "$goalShort/day"
                        StatsTimeframe.WEEK -> "${FocusStatsRepository.formatDurationShort(report.goalTargetMs)}/wk"
                        StatsTimeframe.MONTH -> "${FocusStatsRepository.formatDurationShort(report.goalTargetMs)}/mo"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                view.performKeyTapHaptic()
                                onEditGoal()
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("edit_goal_button")
                    ) {
                        Text(
                            text = targetCaption,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = palette.textSecondary
                        )
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Daily Goal",
                            tint = palette.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            ThemedDivider(palette = palette)

            // Bottom Half: Built-In Infographic Chart + Inline Tooltip
            val chartHeaderTitle = when (report.timeframe) {
                StatsTimeframe.DAY -> "24H TIMELINE"
                StatsTimeframe.WEEK -> "7-DAY BREAKDOWN"
                StatsTimeframe.MONTH -> "MONTHLY CONSISTENCY"
            }

            val selectedBucket = selectedBucketIndex?.let { idx ->
                report.chartBuckets.getOrNull(idx)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chartHeaderTitle,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = palette.textSecondary
                )

                AnimatedContent(
                    targetState = selectedBucket,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "chart_tooltip"
                ) { bucket ->
                    if (bucket != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.primaryContainer,
                            border = BorderStroke(1.dp, palette.primaryBorderTint),
                            modifier = Modifier.testTag("selected_period_pill")
                        ) {
                            Text(
                                text = bucket.tooltipTitle,
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Text(
                            text = if (report.totalFocusMs > 0L) "Tap or scrub bars" else "No sessions yet",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = palette.textSecondary.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            InteractiveThemeBarChart(
                timeframe = report.timeframe,
                buckets = report.chartBuckets,
                dailyTargetMs = report.dailyTargetMs,
                selectedIndex = selectedBucketIndex,
                onSelectBucket = { idx ->
                    haptics.tick()
                    onSelectBucket(idx)
                },
                onScrubBucket = { idx ->
                    haptics.tick()
                    onScrubBucket(idx)
                },
                palette = palette
            )
        }
    }
}

@Composable
private fun CompactGoalGaugeRing(
    progress: Float,
    percent: Int,
    goalMet: Boolean,
    palette: FocusThemePalette,
    size: Dp
) {
    val activeColor = if (goalMet) palette.success else palette.primary
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val clamped = progress.coerceIn(0f, 1f)
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = (minOf(this.size.width, this.size.height) / 2f) - 6.dp.toPx()

            when (palette.theme) {
                AppVisualTheme.NOTHING_CRIMSON -> {
                    val totalDots = 28
                    val activeDots = (totalDots * clamped).toInt()
                        .let { if (clamped > 0f && it == 0) 1 else it }
                    for (i in 0 until totalDots) {
                        val angleRad = Math.toRadians(-90.0 + (360.0 / totalDots) * i)
                        val dx = cx + (radius * cos(angleRad)).toFloat()
                        val dy = cy + (radius * sin(angleRad)).toFloat()
                        val isActive = i < activeDots
                        drawCircle(
                            color = if (isActive) activeColor else palette.borderHighlight,
                            radius = if (isActive) 2.6.dp.toPx() else 1.7.dp.toPx(),
                            center = Offset(dx, dy)
                        )
                    }
                }
                AppVisualTheme.CYBER_ICE -> {
                    val strokeW = 4.dp.toPx()
                    drawCircle(
                        color = palette.border,
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = strokeW)
                    )
                    if (clamped > 0f) {
                        drawArc(
                            color = activeColor,
                            startAngle = -90f,
                            sweepAngle = 360f * clamped,
                            useCenter = false,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                        )
                    }
                }
                AppVisualTheme.SOLAR_AMBER -> {
                    val ticks = 40
                    val activeTicks = (ticks * clamped).toInt()
                        .let { if (clamped > 0f && it == 0) 1 else it }
                    for (i in 0 until ticks) {
                        val rad = Math.toRadians(-90.0 + (360.0 / ticks) * i)
                        val isMajor = i % 5 == 0
                        val outer = radius + if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()
                        val inner = radius - if (isMajor) 4.dp.toPx() else 2.dp.toPx()
                        drawLine(
                            color = if (i < activeTicks) activeColor else palette.borderHighlight,
                            start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                            end = Offset(cx + (outer * cos(rad)).toFloat(), cy + (outer * sin(rad)).toFloat()),
                            strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                AppVisualTheme.ULTRAVIOLET_PULSE -> {
                    val strokeW = 5.dp.toPx()
                    drawCircle(
                        color = palette.border,
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = strokeW)
                    )
                    if (clamped > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(palette.primarySecondary, activeColor, palette.primarySecondary)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * clamped,
                            useCenter = false,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                fontFamily = palette.numeralFont,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (goalMet) palette.success else palette.textPrimary
            )
            Text(
                text = "GOAL",
                fontFamily = JetBrainsMono,
                fontSize = 8.5.sp,
                letterSpacing = 0.8.sp,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun InteractiveThemeBarChart(
    timeframe: StatsTimeframe,
    buckets: List<StatsChartBucket>,
    dailyTargetMs: Long,
    selectedIndex: Int?,
    onSelectBucket: (Int) -> Unit,
    onScrubBucket: (Int) -> Unit,
    palette: FocusThemePalette
) {
    if (buckets.isEmpty()) return

    val isGoalRefChart = timeframe == StatsTimeframe.WEEK || timeframe == StatsTimeframe.MONTH
    val goalReferenceMs = dailyTargetMs
    val maxBucketMs = remember(buckets) { buckets.maxOfOrNull { it.durationMs } ?: 0L }
    val highestBarMinutes = maxBucketMs / 60_000f
    val periodGoalMinutes = goalReferenceMs / 60_000f
    val showGoalLine = isGoalRefChart && (highestBarMinutes >= periodGoalMinutes * 0.5f)
    val scaleMaxMs = remember(maxBucketMs, goalReferenceMs, isGoalRefChart, showGoalLine) {
        if (isGoalRefChart) {
            val highestMins = maxBucketMs / 60_000f
            val goalMins = goalReferenceMs / 60_000f
            val yMaxMinutes = if (showGoalLine) {
                maxOf(highestMins * 1.25f, goalMins, 60f)
            } else {
                maxOf(highestMins * 1.25f, 60f)
            }
            (yMaxMinutes * 60_000f).toLong().coerceAtLeast(60L * 60_000L)
        } else {
            maxBucketMs.coerceAtLeast(60_000L)
        }
    }

    val goalLineLabel = remember(timeframe, goalReferenceMs) {
        val base = FocusStatsRepository.formatDurationShort(goalReferenceMs)
        when (timeframe) {
            StatsTimeframe.WEEK, StatsTimeframe.MONTH -> "$base/day goal"
            StatsTimeframe.DAY -> "$base goal"
        }
    }

    val currentBuckets by rememberUpdatedState(buckets)
    val currentOnSelectBucket by rememberUpdatedState(onSelectBucket)
    val currentOnScrubBucket by rememberUpdatedState(onScrubBucket)
    var lastScrubbedBucket by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isGoalRefChart && !showGoalLine) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = goalLineLabel,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                    color = palette.textSecondary.copy(alpha = 0.85f),
                    modifier = Modifier.testTag("chart_goal_caption")
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val minMonthSlotWidth = 20.dp
            val requiredMonthWidth = minMonthSlotWidth * buckets.size
            val needsHorizontalScroll = timeframe == StatsTimeframe.MONTH && requiredMonthWidth > maxWidth
            val contentWidth = if (needsHorizontalScroll) requiredMonthWidth else maxWidth
            val hScrollState = rememberScrollState()

            LaunchedEffect(timeframe, buckets.size, needsHorizontalScroll) {
                if (needsHorizontalScroll && buckets.any { it.isCurrentPeriod }) {
                    hScrollState.scrollTo(hScrollState.maxValue)
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (needsHorizontalScroll) {
                                Modifier.horizontalScroll(hScrollState)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier.width(contentWidth),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(106.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("stats_bar_chart_canvas")
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            if (size.width > 0 && currentBuckets.isNotEmpty()) {
                                                val slotWidth = size.width.toFloat() / currentBuckets.size.toFloat()
                                                val tapped = (offset.x / slotWidth).toInt()
                                                    .coerceIn(0, currentBuckets.lastIndex)
                                                currentOnSelectBucket(tapped)
                                            }
                                        }
                                    }
                                    .then(
                                        if (!needsHorizontalScroll) {
                                            Modifier.pointerInput(Unit) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = { offset ->
                                                        if (size.width > 0 && currentBuckets.isNotEmpty()) {
                                                            val slotWidth = size.width.toFloat() / currentBuckets.size.toFloat()
                                                            val idx = (offset.x / slotWidth).toInt()
                                                                .coerceIn(0, currentBuckets.lastIndex)
                                                            lastScrubbedBucket = idx
                                                            currentOnScrubBucket(idx)
                                                        }
                                                    },
                                                    onHorizontalDrag = { change, _ ->
                                                        change.consume()
                                                        if (size.width > 0 && currentBuckets.isNotEmpty()) {
                                                            val slotWidth = size.width.toFloat() / currentBuckets.size.toFloat()
                                                            val idx = (change.position.x / slotWidth).toInt()
                                                                .coerceIn(0, currentBuckets.lastIndex)
                                                            if (idx != lastScrubbedBucket) {
                                                                lastScrubbedBucket = idx
                                                                currentOnScrubBucket(idx)
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = { lastScrubbedBucket = -1 },
                                                    onDragCancel = { lastScrubbedBucket = -1 }
                                                )
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                val count = buckets.size
                                if (count == 0) return@Canvas
                                val w = size.width
                                val h = size.height
                                val topPad = 8.dp.toPx()
                                val bottomPad = 4.dp.toPx()
                                val plotH = (h - topPad - bottomPad).coerceAtLeast(10f)
                                val slotW = w / count.toFloat()

                                // Optional Cyber Ice subtle horizontal gridlines
                                if (palette.theme == AppVisualTheme.CYBER_ICE) {
                                    for (frac in listOf(0.25f, 0.5f, 0.75f)) {
                                        val gy = topPad + plotH * (1f - frac)
                                        drawLine(
                                            color = palette.primary.copy(alpha = 0.08f),
                                            start = Offset(0f, gy),
                                            end = Offset(w, gy),
                                            strokeWidth = 1f
                                        )
                                    }
                                }

                                // Baseline
                                if (palette.useDottedDividers) {
                                    val dotR = 1.dp.toPx()
                                    val step = 6.dp.toPx()
                                    var dx = dotR
                                    while (dx < w) {
                                        drawCircle(
                                            color = palette.borderHighlight,
                                            radius = dotR,
                                            center = Offset(dx, h - 1.dp.toPx())
                                        )
                                        dx += step
                                    }
                                } else {
                                    drawLine(
                                        color = palette.border,
                                        start = Offset(0f, h - 1.dp.toPx()),
                                        end = Offset(w, h - 1.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Bars
                                val barGapRatio = when (timeframe) {
                                    StatsTimeframe.WEEK -> 0.42f
                                    StatsTimeframe.DAY -> 0.34f
                                    StatsTimeframe.MONTH -> 0.34f
                                }
                                val maxBarW = if (timeframe == StatsTimeframe.MONTH) 18.dp.toPx() else 24.dp.toPx()
                                val barW = (slotW * (1f - barGapRatio)).coerceIn(3.dp.toPx(), maxBarW)
                                val cornerPx = when (palette.theme) {
                                    AppVisualTheme.NOTHING_CRIMSON -> 1.5.dp.toPx()
                                    AppVisualTheme.CYBER_ICE -> 2.dp.toPx()
                                    AppVisualTheme.SOLAR_AMBER -> 3.dp.toPx()
                                    AppVisualTheme.ULTRAVIOLET_PULSE -> 6.dp.toPx()
                                }

                                buckets.forEachIndexed { idx, bucket ->
                                    val centerX = slotW * idx + slotW / 2f
                                    val leftX = centerX - barW / 2f
                                    val isSelected = selectedIndex == idx
                                    val hasValue = bucket.durationMs > 0L
                                    val ratio = if (hasValue) {
                                        (bucket.durationMs.toFloat() / scaleMaxMs.toFloat()).coerceIn(0.06f, 1f)
                                    } else {
                                        0f
                                    }

                                    if (hasValue) {
                                        val barH = plotH * ratio
                                        val topY = topPad + (plotH - barH)
                                        val barColor = when {
                                            isSelected -> palette.primary
                                            selectedIndex != null -> palette.primary.copy(alpha = 0.35f)
                                            bucket.isCurrentPeriod -> palette.primary
                                            else -> palette.primary.copy(alpha = 0.76f)
                                        }

                                        if (palette.theme == AppVisualTheme.ULTRAVIOLET_PULSE) {
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(barColor, palette.primarySecondary.copy(alpha = if (selectedIndex != null && !isSelected) 0.25f else 0.72f)),
                                                    startY = topY,
                                                    endY = topPad + plotH
                                                ),
                                                topLeft = Offset(leftX, topY),
                                                size = Size(barW, barH),
                                                cornerRadius = CornerRadius(cornerPx, cornerPx)
                                            )
                                        } else {
                                            drawRoundRect(
                                                color = barColor,
                                                topLeft = Offset(leftX, topY),
                                                size = Size(barW, barH),
                                                cornerRadius = CornerRadius(cornerPx, cornerPx)
                                            )
                                        }
                                    } else {
                                        val stubH = if (isSelected) 8.dp.toPx() else 3.dp.toPx()
                                        drawRoundRect(
                                            color = if (isSelected) palette.primary.copy(alpha = 0.65f) else palette.borderHighlight.copy(alpha = 0.45f),
                                            topLeft = Offset(leftX, topPad + plotH - stubH),
                                            size = Size(barW, stubH),
                                            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                                        )
                                    }
                                }

                                // Dashed horizontal Goal reference line only when highestBarMinutes >= periodGoalMinutes * 0.5f
                                if (showGoalLine) {
                                    val goalRatio = (goalReferenceMs.toFloat() / scaleMaxMs.toFloat()).coerceIn(0.05f, 1f)
                                    val goalY = topPad + plotH * (1f - goalRatio)
                                    drawLine(
                                        color = palette.textSecondary.copy(alpha = 0.55f),
                                        start = Offset(0f, goalY),
                                        end = Offset(w, goalY),
                                        strokeWidth = 1.2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                    )
                                }
                            }
                        }

                        // X-Axis Labels aligned to slots
                        when (timeframe) {
                            StatsTimeframe.WEEK -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    buckets.forEachIndexed { idx, bucket ->
                                        val isSel = selectedIndex == idx
                                        Text(
                                            text = bucket.axisLabel,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 9.5.sp,
                                            lineHeight = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (isSel || bucket.isCurrentPeriod) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSel -> palette.primary
                                                bucket.isCurrentPeriod -> palette.textPrimary
                                                else -> palette.textSecondary
                                            },
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            StatsTimeframe.MONTH -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    buckets.forEachIndexed { idx, bucket ->
                                        val isSel = selectedIndex == idx
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = bucket.axisLabel,
                                                fontFamily = JetBrainsMono,
                                                fontSize = 8.5.sp,
                                                lineHeight = 10.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                fontWeight = if (isSel || bucket.isCurrentPeriod) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSel -> palette.primary
                                                    bucket.isCurrentPeriod -> palette.textPrimary
                                                    else -> palette.textSecondary
                                                },
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.testTag("month_axis_label_${bucket.axisLabel}")
                                            )
                                        }
                                    }
                                }
                            }
                            StatsTimeframe.DAY -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf("12AM", "6AM", "12PM", "6PM", "12AM").forEach { label ->
                                        Text(
                                            text = label,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 9.sp,
                                            color = palette.textSecondary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showGoalLine) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = palette.surface.copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .testTag("chart_goal_line_badge")
                    ) {
                        Text(
                            text = goalLineLabel,
                            fontFamily = JetBrainsMono,
                            fontSize = 8.5.sp,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedBentoMetricsGrid(
    report: StatsPeriodReport,
    selectedBucket: StatsChartBucket? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.surface,
        border = BorderStroke(0.75.dp, palette.border),
        modifier = modifier
            .fillMaxWidth()
            .testTag("bento_metrics_grid")
    ) {
        AnimatedContent(
            targetState = selectedBucket,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "bento_grid_mode"
        ) { bucket ->
            if (bucket != null) {
                val periodSharePct = if (report.totalFocusMs > 0L) {
                    ((bucket.durationMs.toFloat() / report.totalFocusMs.toFloat()) * 100f)
                        .roundToInt()
                        .coerceIn(0, 100)
                } else {
                    0
                }
                val goalPctRaw = if (report.goalTargetMs > 0L) {
                    (bucket.durationMs.toFloat() / report.goalTargetMs.toFloat()) * 100f
                } else {
                    0f
                }
                val goalImpactValue = when {
                    bucket.durationMs <= 0L -> "0%"
                    goalPctRaw < 1f -> "<1%"
                    else -> "${goalPctRaw.roundToInt().coerceAtMost(999)}%"
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        BentoMetricCell(
                            label = "TOTAL SESSIONS",
                            value = "${bucket.sessionCount}",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                        BentoVerticalDivider(palette = palette)
                        BentoMetricCell(
                            label = "TOTAL TIME",
                            value = FocusStatsRepository.formatDurationHero(bucket.durationMs),
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    BentoHorizontalDivider(palette = palette)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        BentoMetricCell(
                            label = "SHARE OF ${report.timeframe.label}",
                            value = "$periodSharePct%",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                        BentoVerticalDivider(palette = palette)
                        BentoMetricCell(
                            label = "GOAL IMPACT",
                            value = goalImpactValue,
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Row: SESSIONS | AVG SESSION
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        BentoMetricCell(
                            label = "SESSIONS",
                            value = "${report.sessionCount}",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                        BentoVerticalDivider(palette = palette)
                        BentoMetricCell(
                            label = "AVG SESSION",
                            value = formatDurationMinutes(report.avgSessionMs),
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    BentoHorizontalDivider(palette = palette)

                    // Bottom Row: LONGEST FOCUS | STREAK
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        BentoMetricCell(
                            label = "LONGEST FOCUS",
                            value = formatDurationMinutes(report.longestSessionMs),
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                        BentoVerticalDivider(palette = palette)
                        BentoStreakCell(
                            currentStreak = report.currentStreak,
                            maxStreak = report.maxStreak,
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoMetricCell(
    label: String,
    value: String,
    palette: FocusThemePalette,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontFamily = palette.numeralFont,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BentoStreakCell(
    currentStreak: Int,
    maxStreak: Int,
    palette: FocusThemePalette,
    modifier: Modifier = Modifier
) {
    val currentUnit = if (currentStreak == 1) "day" else "days"
    val bestUnit = if (maxStreak == 1) "day" else "days"

    Row(
        modifier = modifier
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("bento_streak_cell"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Current Streak
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = "Current Streak",
                    tint = palette.primary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "STREAK",
                    fontFamily = JetBrainsMono,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.8.sp,
                    color = palette.textSecondary,
                    maxLines = 1
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "$currentStreak",
                    fontFamily = palette.numeralFont,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = currentUnit,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }

        // Right: Best Streak
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = "Best Streak",
                    tint = palette.warning,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "BEST",
                    fontFamily = JetBrainsMono,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textSecondary,
                    maxLines = 1
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "$maxStreak",
                    fontFamily = palette.numeralFont,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = bestUnit,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    }
}

@Composable
private fun BentoHorizontalDivider(palette: FocusThemePalette) {
    if (palette.useDottedDividers) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            val dotR = 0.9.dp.toPx()
            val spacing = 5.dp.toPx()
            var x = dotR
            while (x < size.width) {
                drawCircle(
                    color = palette.borderHighlight,
                    radius = dotR,
                    center = Offset(x, size.height / 2f)
                )
                x += spacing
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border)
        )
    }
}

@Composable
private fun BentoVerticalDivider(palette: FocusThemePalette) {
    if (palette.useDottedDividers) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        ) {
            val dotR = 0.9.dp.toPx()
            val spacing = 5.dp.toPx()
            var y = dotR
            while (y < size.height) {
                drawCircle(
                    color = palette.borderHighlight,
                    radius = dotR,
                    center = Offset(size.width / 2f, y)
                )
                y += spacing
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(palette.border)
        )
    }
}
