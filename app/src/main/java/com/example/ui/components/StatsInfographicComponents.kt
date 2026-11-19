package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

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
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale = rememberSpringPressScale(interactionSource, pressedScale = 0.94f)
    val pillShape = remember { RoundedCornerShape(50) }
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
        Box(
            modifier = Modifier.focusGlassPanel(
                shape = pillShape,
                palette = palette,
                isPressed = isPressed
            )
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
        containerColor = if (palette.isLiquidGlass) Color(0xFA090B10) else palette.surface,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = Modifier.testTag("rank_xp_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(palette.primaryContainer),
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
                    color = palette.primaryContainer
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(50))
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
                                .clip(RoundedCornerShape(50))
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

            Surface(
                shape = RoundedCornerShape(50),
                color = if (goalSafe) palette.success.copy(alpha = 0.14f) else palette.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_goal_penalty_status")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (goalSafe) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (goalSafe) palette.success else palette.primary,
                        modifier = Modifier.size(17.dp)
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RANK LADDER • 6 TIERS",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.1.sp,
                    color = palette.textSecondary
                )

                val currentOrdinal = xpState.rank.ordinal
                val ladderShape = remember { RoundedCornerShape(28.dp) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGlassPanel(shape = ladderShape, palette = palette, elevated = true)
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
                                                shape = RoundedCornerShape(50),
                                                color = palette.primary
                                            ) {
                                                Text(
                                                    text = "CURRENT",
                                                    fontFamily = JetBrainsMono,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.5.dp)
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

/**
 * Elastic iOS-style Day / Week / Month pill switcher for Google Health / Fitbit-style Stats.
 */
@Composable
fun StatsTimeframeSwitcher(
    selectedTimeframe: StatsTimeframe,
    onSelectTimeframe: (StatsTimeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val segments = remember {
        listOf(
            ElasticPillSegment("Day", testTag = "timeframe_day_tab"),
            ElasticPillSegment("Week", testTag = "timeframe_week_tab"),
            ElasticPillSegment("Month", testTag = "timeframe_month_tab")
        )
    }
    val selectedIndex = StatsTimeframe.entries.indexOf(selectedTimeframe).coerceAtLeast(0)

    ElasticMorphingSegmentedPill(
        segments = segments,
        selectedIndex = selectedIndex,
        onSelectIndex = { idx ->
            StatsTimeframe.entries.getOrNull(idx)?.let(onSelectTimeframe)
        },
        height = 42.dp,
        fontSize = 13.sp,
        fontFamily = palette.displayFont,
        modifier = modifier
    )
}

/**
 * Ultra-minimal single-row Fitbit / Google Health floating date pill selector.
 * Replaces the bulky multi-row date strip while keeping swipe navigation,
 * calendar picker tap, and inline TODAY / CURRENT quick-jump pill.
 */
@Composable
fun StatsDateNavigatorSection(
    report: StatsPeriodReport,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onJumpToToday: () -> Unit,
    onOpenCalendarPicker: () -> Unit,
    onSelectStripDay: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val view = LocalView.current
    val currentCanForward by rememberUpdatedState(report.canStepForward)
    val currentOnBack by rememberUpdatedState(onStepBackward)
    val currentOnFwd by rememberUpdatedState(onStepForward)
    var dragAccumulated by remember { mutableFloatStateOf(0f) }
    var slideDirection by remember { mutableIntStateOf(0) }
    val pillShape = remember { RoundedCornerShape(50) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .focusGlassPanel(shape = pillShape, palette = palette)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccumulated += dragAmount },
                    onDragEnd = {
                        val threshold = 64f
                        when {
                            dragAccumulated > threshold -> {
                                slideDirection = -1
                                haptics.segment()
                                currentOnBack()
                            }
                            dragAccumulated < -threshold && currentCanForward -> {
                                slideDirection = 1
                                haptics.segment()
                                currentOnFwd()
                            }
                        }
                        dragAccumulated = 0f
                    },
                    onDragCancel = { dragAccumulated = 0f }
                )
            }
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircularNavChevronButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Previous Period",
                enabled = true,
                onClick = {
                    slideDirection = -1
                    view.performClockTickHaptic()
                    onStepBackward()
                },
                modifier = Modifier.testTag("prev_day_button")
            )

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = report.headerDateLabel,
                    transitionSpec = {
                        val dir = if (slideDirection == 0) 1 else slideDirection
                        (slideInHorizontally(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                        ) { w -> (w * 0.22f * dir).toInt() } + fadeIn()) togetherWith
                            (slideOutHorizontally(
                                animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium)
                            ) { w -> (-w * 0.22f * dir).toInt() } + fadeOut())
                    },
                    label = "stats_date_label_slide",
                    modifier = Modifier.weight(1f, fill = false)
                ) { label ->
                    Text(
                        text = label,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(pillShape)
                            .clickable {
                                view.performKeyTapHaptic()
                                onOpenCalendarPicker()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                AnimatedVisibility(
                    visible = !report.isCurrentPeriod,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    val jumpLabel = when (report.timeframe) {
                        StatsTimeframe.DAY -> "TODAY"
                        StatsTimeframe.WEEK, StatsTimeframe.MONTH -> "CURRENT"
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .defaultMinSize(minHeight = 36.dp)
                            .clip(pillShape)
                            .background(palette.primaryContainer, pillShape)
                            .clickable {
                                slideDirection = 1
                                view.performClockTickHaptic()
                                onJumpToToday()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("jump_today_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = jumpLabel,
                                fontFamily = JetBrainsMono,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }

            CircularNavChevronButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Next Period",
                enabled = report.canStepForward,
                onClick = {
                    if (report.canStepForward) {
                        slideDirection = 1
                        view.performClockTickHaptic()
                        onStepForward()
                    }
                },
                modifier = Modifier.testTag("next_day_button")
            )
        }
    }
}

@Composable
private fun CircularNavChevronButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.88f)

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) palette.surfaceElevated.copy(alpha = 0.85f)
                    else palette.surfaceElevated.copy(alpha = 0.30f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) palette.textPrimary else palette.textSecondary.copy(alpha = 0.30f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Fitbit / Google Health-inspired minimal hero & capsule bar chart card.
 * Features an airy editorial focus total, tappable goal pill, sleek full-pill progress bar,
 * and interactive capsule bar chart where 0s bars remain completely non-clickable.
 */
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

    val animatedProgress by animateFloatAsState(
        targetValue = report.progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fitbit_goal_progress_bar"
    )

    val heroCardShape = remember { RoundedCornerShape(32.dp) }
    val pillShape = remember { RoundedCornerShape(50) }
    val selectedBucket = selectedBucketIndex?.let { idx ->
        report.chartBuckets.getOrNull(idx)
    }

    val goalShort = remember(dailyGoalMinutes) {
        val h = dailyGoalMinutes / 60L
        val m = dailyGoalMinutes % 60L
        when {
            h > 0L && m > 0L -> "${h}h ${m}m"
            h > 0L -> "${h}h"
            else -> "${m}m"
        }
    }
    val goalTargetBadge = when (report.timeframe) {
        StatsTimeframe.DAY -> "$goalShort goal"
        StatsTimeframe.WEEK -> "${FocusStatsRepository.formatDurationShort(report.goalTargetMs)} wk"
        StatsTimeframe.MONTH -> "${FocusStatsRepository.formatDurationShort(report.goalTargetMs)} mo"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .focusGlassPanel(
                shape = heroCardShape,
                palette = palette
            )
            .testTag("hero_infographic_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Editorial Top Header Row: Focus Total (left) + Goal Edit Pill (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (report.timeframe) {
                            StatsTimeframe.DAY -> "Focus time"
                            StatsTimeframe.WEEK -> "Weekly focus"
                            StatsTimeframe.MONTH -> "Monthly focus"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textSecondary
                    )

                    RollingMetricText(
                        text = FocusStatsRepository.formatDurationHero(report.totalFocusMs),
                        style = TextStyle(
                            fontFamily = palette.numeralFont,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp
                        ),
                        color = palette.textPrimary
                    )
                }

                // Sleek borderless goal edit pill
                val goalInteraction = remember { MutableInteractionSource() }
                val goalScale = rememberSpringPressScale(goalInteraction, pressedScale = 0.93f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = goalScale
                            scaleY = goalScale
                        }
                        .defaultMinSize(minHeight = 36.dp)
                        .clip(pillShape)
                        .background(
                            if (report.isGoalMet) palette.success.copy(alpha = 0.14f)
                            else palette.surfaceElevated,
                            pillShape
                        )
                        .clickable(
                            interactionSource = goalInteraction,
                            indication = null
                        ) {
                            view.performKeyTapHaptic()
                            onEditGoal()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("edit_goal_button")
                ) {
                    Text(
                        text = goalTargetBadge,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (report.isGoalMet) palette.success else palette.textPrimary
                    )
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Daily Goal",
                        tint = if (report.isGoalMet) palette.success else palette.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Fitbit-Style Horizontal Full-Pill Goal Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when {
                        report.isGoalMet -> "Goal reached"
                        report.totalFocusMs == 0L -> "${FocusStatsRepository.formatDurationShort(report.remainingToGoalMs)} to daily goal"
                        else -> "${FocusStatsRepository.formatDurationShort(report.remainingToGoalMs)} remaining"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = if (report.isGoalMet) palette.success else palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = pillShape,
                        color = if (report.isGoalMet) palette.success.copy(alpha = 0.16f) else palette.primaryContainer
                    ) {
                        Text(
                            text = "${report.progressPercent}%",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.isGoalMet) palette.success else palette.primary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(pillShape)
                        .background(palette.surfaceElevated)
                ) {
                    if (animatedProgress > 0f) {
                        val fillBrush = remember(palette, report.isGoalMet) {
                            when {
                                report.isGoalMet -> Brush.horizontalGradient(
                                    listOf(palette.success.copy(alpha = 0.85f), palette.success)
                                )
                                palette.isLiquidGlass -> Brush.horizontalGradient(
                                    listOf(Color(0xFF8A99B2), Color(0xFFE3EBF8))
                                )
                                else -> Brush.horizontalGradient(
                                    listOf(palette.primaryDeep, palette.primary)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    scaleX = animatedProgress.coerceAtLeast(0.03f)
                                }
                                .clip(pillShape)
                                .background(fillBrush)
                        )
                    }
                }
            }

            // Selected Bar Inspection Pill OR Quiet Chart Caption
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = selectedBucket,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "fitbit_selected_bucket_pill"
                ) { bucket ->
                    if (bucket != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(pillShape)
                                .background(palette.primaryContainer, pillShape)
                                .clickable {
                                    haptics.tick()
                                    onSelectBucket(null)
                                }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                                .testTag("selected_period_pill")
                        ) {
                            Text(
                                text = bucket.tooltipTitle,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear selection",
                                tint = palette.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        Text(
                            text = when (report.timeframe) {
                                StatsTimeframe.DAY -> "Hourly activity"
                                StatsTimeframe.WEEK -> "Daily activity"
                                StatsTimeframe.MONTH -> "Monthly activity"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            // Minimal Capsule Pill Bar Chart (zero-data bars stay completely non-clickable)
            FitbitPillBarChart(
                reportKey = "${report.timeframe.name}_${report.headerDateLabel}",
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
private fun FitbitPillBarChart(
    reportKey: String,
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

    // Smooth event-driven spring bar rise whenever period/timeframe changes (zero idle loop)
    val barGrowth = remember { Animatable(1f) }
    LaunchedEffect(reportKey) {
        barGrowth.snapTo(0.18f)
        barGrowth.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    // Event-driven spring pop when selecting or switching bars (zero idle animation)
    val selectionPop = remember { Animatable(if (selectedIndex != null) 1f else 0f) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null) {
            selectionPop.snapTo(0.25f)
            selectionPop.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
            )
        } else {
            selectionPop.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    val currentBuckets by rememberUpdatedState(buckets)
    val currentOnSelectBucket by rememberUpdatedState(onSelectBucket)
    val currentOnScrubBucket by rememberUpdatedState(onScrubBucket)
    var lastScrubbedBucket by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isGoalRefChart && !showGoalLine) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.surfaceElevated
                ) {
                    Text(
                        text = goalLineLabel,
                        fontFamily = JetBrainsMono,
                        fontSize = 9.5.sp,
                        color = palette.textSecondary,
                        modifier = Modifier
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                            .testTag("chart_goal_caption")
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val minMonthSlotWidth = 22.dp
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
                            if (needsHorizontalScroll) Modifier.horizontalScroll(hScrollState)
                            else Modifier
                        )
                ) {
                    Column(
                        modifier = Modifier.width(contentWidth),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("stats_bar_chart_canvas")
                                    .pointerInput(needsHorizontalScroll) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            if (size.width <= 0 || currentBuckets.isEmpty()) return@awaitEachGesture

                                            val slotWidth = size.width.toFloat() / currentBuckets.size.toFloat()
                                            val downIdx = (down.position.x / slotWidth).toInt()
                                                .coerceIn(0, currentBuckets.lastIndex)
                                            val downBucket = currentBuckets.getOrNull(downIdx)

                                            // Zero-data (0s) bars remain completely non-interactive so taps & swipes pass straight through.
                                            if (downBucket == null || downBucket.durationMs <= 0L) {
                                                return@awaitEachGesture
                                            }

                                            val touchSlop = viewConfiguration.touchSlop
                                            var isScrubbing = false
                                            var handedOffToScroll = false
                                            lastScrubbedBucket = -1

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id }
                                                    ?: event.changes.firstOrNull()
                                                    ?: break

                                                if (!change.pressed) {
                                                    if (!isScrubbing && !handedOffToScroll && !change.isConsumed) {
                                                        change.consume()
                                                        currentOnSelectBucket(downIdx)
                                                    }
                                                    break
                                                }

                                                val totalDx = change.position.x - down.position.x
                                                val totalDy = change.position.y - down.position.y

                                                if (!isScrubbing && !handedOffToScroll) {
                                                    if (hypot(totalDx.toDouble(), totalDy.toDouble()) > touchSlop) {
                                                        if (abs(totalDy) >= abs(totalDx) || needsHorizontalScroll) {
                                                            handedOffToScroll = true
                                                            break
                                                        } else {
                                                            isScrubbing = true
                                                            lastScrubbedBucket = downIdx
                                                            currentOnScrubBucket(downIdx)
                                                        }
                                                    }
                                                }

                                                if (isScrubbing) {
                                                    change.consume()
                                                    val idx = (change.position.x / slotWidth).toInt()
                                                        .coerceIn(0, currentBuckets.lastIndex)
                                                    val hoverBucket = currentBuckets.getOrNull(idx)
                                                    if (idx != lastScrubbedBucket && hoverBucket != null && hoverBucket.durationMs > 0L) {
                                                        lastScrubbedBucket = idx
                                                        currentOnScrubBucket(idx)
                                                    }
                                                }
                                            }
                                            lastScrubbedBucket = -1
                                        }
                                    }
                            ) {
                                val count = buckets.size
                                if (count == 0) return@Canvas
                                val w = size.width
                                val h = size.height
                                val topPad = 18.dp.toPx()
                                val bottomPad = 6.dp.toPx()
                                val plotH = (h - topPad - bottomPad).coerceAtLeast(14f)
                                val slotW = w / count.toFloat()

                                val barGapRatio = when (timeframe) {
                                    StatsTimeframe.WEEK -> 0.44f
                                    StatsTimeframe.DAY -> 0.36f
                                    StatsTimeframe.MONTH -> 0.34f
                                }
                                val maxBarW = when (timeframe) {
                                    StatsTimeframe.WEEK -> 22.dp.toPx()
                                    StatsTimeframe.MONTH -> 14.dp.toPx()
                                    StatsTimeframe.DAY -> 10.dp.toPx()
                                }
                                val barW = (slotW * (1f - barGapRatio)).coerceIn(4.dp.toPx(), maxBarW)
                                val pillRadius = CornerRadius(barW / 2f, barW / 2f)
                                val growth = barGrowth.value.coerceIn(0f, 1f)
                                val selPop = selectionPop.value.coerceIn(0f, 1.15f)

                                val trackColor = if (palette.isLiquidGlass) {
                                    Color.White.copy(alpha = 0.055f)
                                } else {
                                    palette.surfaceElevated.copy(alpha = 0.55f)
                                }

                                // Theme-matched luminous selection colors (high contrast, zero borders)
                                val selectedHighlightTop = when (palette.theme) {
                                    AppVisualTheme.NOTHING_CRIMSON -> Color(0xFFFF8A90)
                                    AppVisualTheme.LIQUID_GLASS -> Color(0xFFFFFFFF)
                                    AppVisualTheme.SOLAR_AMBER -> Color(0xFFFFE596)
                                    AppVisualTheme.NORDIC_SAGE -> Color(0xFFDCF8EC)
                                }
                                val selectedHighlightBottom = when (palette.theme) {
                                    AppVisualTheme.NOTHING_CRIMSON -> palette.primary
                                    AppVisualTheme.LIQUID_GLASS -> Color(0xFFB4C8E8)
                                    AppVisualTheme.SOLAR_AMBER -> palette.primary
                                    AppVisualTheme.NORDIC_SAGE -> palette.primary
                                }
                                val spotlightTint = when (palette.theme) {
                                    AppVisualTheme.LIQUID_GLASS -> Color(0xFFD8E4FA)
                                    else -> palette.primary
                                }
                                val beaconCoreColor = when (palette.theme) {
                                    AppVisualTheme.NOTHING_CRIMSON -> Color.White
                                    AppVisualTheme.LIQUID_GLASS -> Color.White
                                    AppVisualTheme.SOLAR_AMBER -> Color(0xFFFFF4CC)
                                    AppVisualTheme.NORDIC_SAGE -> Color(0xFFE8FFF4)
                                }

                                buckets.forEachIndexed { idx, bucket ->
                                    val centerX = slotW * idx + slotW / 2f
                                    val leftX = centerX - barW / 2f
                                    val hasValue = bucket.durationMs > 0L
                                    val isSelected = hasValue && selectedIndex == idx
                                    val dimmed = selectedIndex != null && !isSelected

                                    // 1. Background capsule track (illuminated full-column spotlight pill when selected)
                                    if (isSelected && selPop > 0.02f) {
                                        val haloW = (barW + 10.dp.toPx() * selPop).coerceAtMost(slotW * 0.96f)
                                        val haloLeft = centerX - haloW / 2f
                                        val haloTop = (topPad - 6.dp.toPx()).coerceAtLeast(2f)
                                        val haloH = (plotH + 10.dp.toPx()).coerceAtMost(h - haloTop)
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    spotlightTint.copy(alpha = 0.22f * selPop.coerceAtMost(1f)),
                                                    spotlightTint.copy(alpha = 0.12f * selPop.coerceAtMost(1f)),
                                                    spotlightTint.copy(alpha = 0.04f * selPop.coerceAtMost(1f))
                                                ),
                                                startY = haloTop,
                                                endY = haloTop + haloH
                                            ),
                                            topLeft = Offset(haloLeft, haloTop),
                                            size = Size(haloW, haloH),
                                            cornerRadius = CornerRadius(haloW / 2f, haloW / 2f)
                                        )
                                    } else {
                                        drawRoundRect(
                                            color = if (dimmed) trackColor.copy(alpha = trackColor.alpha * 0.65f) else trackColor,
                                            topLeft = Offset(leftX, topPad),
                                            size = Size(barW, plotH),
                                            cornerRadius = pillRadius
                                        )
                                    }

                                    if (hasValue) {
                                        val rawRatio = (bucket.durationMs.toFloat() / scaleMaxMs.toFloat())
                                            .coerceIn(0.08f, 1f)
                                        val activeBarW = if (isSelected) {
                                            (barW * (1f + 0.22f * selPop)).coerceAtMost(slotW * 0.86f)
                                        } else {
                                            barW
                                        }
                                        val activeLeftX = centerX - activeBarW / 2f
                                        val activeRadius = CornerRadius(activeBarW / 2f, activeBarW / 2f)
                                        val barH = (plotH * rawRatio * growth).coerceAtLeast(activeBarW)
                                        val topY = topPad + (plotH - barH)

                                        if (isSelected) {
                                            // Soft luminous ambient bloom behind the active bar
                                            val glowPad = 4.dp.toPx() * selPop.coerceAtMost(1f)
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        spotlightTint.copy(alpha = 0.34f * selPop.coerceAtMost(1f)),
                                                        spotlightTint.copy(alpha = 0.10f * selPop.coerceAtMost(1f))
                                                    ),
                                                    startY = (topY - glowPad).coerceAtLeast(0f),
                                                    endY = topPad + plotH
                                                ),
                                                topLeft = Offset(activeLeftX - glowPad * 0.65f, (topY - glowPad * 0.4f).coerceAtLeast(0f)),
                                                size = Size(activeBarW + glowPad * 1.3f, barH + glowPad * 0.6f),
                                                cornerRadius = CornerRadius((activeBarW + glowPad) / 2f, (activeBarW + glowPad) / 2f)
                                            )

                                            // High-contrast luminous theme-matched selected bar fill
                                            drawRoundRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(selectedHighlightTop, selectedHighlightBottom),
                                                    startY = topY,
                                                    endY = topPad + plotH
                                                ),
                                                topLeft = Offset(activeLeftX, topY),
                                                size = Size(activeBarW, barH),
                                                cornerRadius = activeRadius
                                            )

                                            // Subtle crisp top specular cap bead inside the selected pill
                                            val capInset = (activeBarW * 0.22f).coerceAtLeast(1.5.dp.toPx())
                                            val capSize = (activeBarW - capInset * 2f).coerceAtLeast(2.dp.toPx())
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.55f * selPop.coerceAtMost(1f)),
                                                radius = (capSize * 0.38f).coerceAtMost(3.dp.toPx()),
                                                center = Offset(centerX, topY + activeBarW * 0.5f)
                                            )

                                            // Floating active beacon dot above the selected bar cap
                                            val beaconY = (topY - 8.dp.toPx()).coerceAtLeast(5.dp.toPx())
                                            val beaconOuterR = 4.8.dp.toPx() * selPop.coerceAtMost(1f)
                                            val beaconInnerR = 2.6.dp.toPx() * selPop.coerceAtMost(1f)
                                            drawCircle(
                                                color = spotlightTint.copy(alpha = 0.32f * selPop.coerceAtMost(1f)),
                                                radius = beaconOuterR,
                                                center = Offset(centerX, beaconY)
                                            )
                                            drawCircle(
                                                color = beaconCoreColor,
                                                radius = beaconInnerR,
                                                center = Offset(centerX, beaconY)
                                            )
                                        } else {
                                            when (palette.theme) {
                                                AppVisualTheme.LIQUID_GLASS -> {
                                                    drawRoundRect(
                                                        brush = Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFFD2DCEB).copy(alpha = if (dimmed) 0.20f else 0.80f),
                                                                Color(0xFF5D6B84).copy(alpha = if (dimmed) 0.11f else 0.42f)
                                                            ),
                                                            startY = topY,
                                                            endY = topPad + plotH
                                                        ),
                                                        topLeft = Offset(activeLeftX, topY),
                                                        size = Size(activeBarW, barH),
                                                        cornerRadius = activeRadius
                                                    )
                                                }
                                                AppVisualTheme.NORDIC_SAGE -> {
                                                    val topCol = if (dimmed) palette.primary.copy(alpha = 0.22f) else palette.primary
                                                    drawRoundRect(
                                                        brush = Brush.verticalGradient(
                                                            colors = listOf(
                                                                topCol,
                                                                palette.primarySecondary.copy(alpha = if (dimmed) 0.14f else 0.76f)
                                                            ),
                                                            startY = topY,
                                                            endY = topPad + plotH
                                                        ),
                                                        topLeft = Offset(activeLeftX, topY),
                                                        size = Size(activeBarW, barH),
                                                        cornerRadius = activeRadius
                                                    )
                                                }
                                                else -> {
                                                    val barCol = when {
                                                        dimmed -> palette.primary.copy(alpha = 0.22f)
                                                        bucket.isCurrentPeriod -> palette.primary
                                                        else -> palette.primary.copy(alpha = 0.82f)
                                                    }
                                                    drawRoundRect(
                                                        color = barCol,
                                                        topLeft = Offset(activeLeftX, topY),
                                                        size = Size(activeBarW, barH),
                                                        cornerRadius = activeRadius
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Quiet pill dot for zero-data bars
                                        val dotH = 3.5.dp.toPx()
                                        drawRoundRect(
                                            color = if (palette.isLiquidGlass) {
                                                Color.White.copy(alpha = if (dimmed) 0.08f else 0.14f)
                                            } else {
                                                palette.textSecondary.copy(alpha = if (dimmed) 0.12f else 0.20f)
                                            },
                                            topLeft = Offset(leftX, topPad + plotH - dotH),
                                            size = Size(barW, dotH),
                                            cornerRadius = CornerRadius(dotH / 2f, dotH / 2f)
                                        )
                                    }
                                }

                                // Dashed horizontal goal reference line when highest bar >= 50% of daily goal
                                if (showGoalLine) {
                                    val goalRatio = (goalReferenceMs.toFloat() / scaleMaxMs.toFloat()).coerceIn(0.05f, 1f)
                                    val goalY = topPad + plotH * (1f - goalRatio)
                                    drawLine(
                                        color = palette.textSecondary.copy(alpha = 0.45f),
                                        start = Offset(0f, goalY),
                                        end = Offset(w, goalY),
                                        strokeWidth = 1.2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                    )
                                }
                            }
                        }

                        // Minimal Pill-Aligned X-Axis Labels with Filled Active Pill Highlight
                        val axisPillBg = if (palette.isLiquidGlass) Color(0xFF283142) else palette.primary
                        val axisPillFg = if (palette.isLiquidGlass) palette.textPrimary else palette.onPrimary

                        when (timeframe) {
                            StatsTimeframe.WEEK -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
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
                                                fontSize = 9.5.sp,
                                                lineHeight = 11.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = if (isSel || bucket.isCurrentPeriod) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSel -> axisPillFg
                                                    bucket.isCurrentPeriod -> palette.textPrimary
                                                    else -> palette.textSecondary
                                                },
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (isSel) axisPillBg else Color.Transparent)
                                                    .padding(
                                                        horizontal = if (isSel) 7.dp else 2.dp,
                                                        vertical = if (isSel) 3.dp else 2.dp
                                                    )
                                            )
                                        }
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
                                                    isSel -> axisPillFg
                                                    bucket.isCurrentPeriod -> palette.textPrimary
                                                    else -> palette.textSecondary
                                                },
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (isSel) axisPillBg else Color.Transparent)
                                                    .padding(
                                                        horizontal = if (isSel) 5.dp else 1.dp,
                                                        vertical = 2.dp
                                                    )
                                                    .testTag("month_axis_label_${bucket.axisLabel}")
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
                                    listOf("12a", "6a", "12p", "6p", "12a").forEach { label ->
                                        Text(
                                            text = label,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 9.5.sp,
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
                        shape = RoundedCornerShape(50),
                        color = palette.surfaceElevated.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .testTag("chart_goal_line_badge")
                    ) {
                        Text(
                            text = goalLineLabel,
                            fontFamily = JetBrainsMono,
                            fontSize = 9.sp,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Airy 2×2 Fitbit / Google Health metric capsules with circular tinted icon badges
 * and zero dividers or boxy borders.
 */
@Composable
fun UnifiedBentoMetricsGrid(
    report: StatsPeriodReport,
    selectedBucket: StatsChartBucket? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current

    AnimatedContent(
        targetState = selectedBucket,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "fitbit_metrics_capsules_mode",
        modifier = modifier
            .fillMaxWidth()
            .testTag("bento_metrics_grid")
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

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FitbitMetricCapsule(
                        icon = Icons.Outlined.Bolt,
                        iconTint = palette.primary,
                        label = "Sessions",
                        value = "${bucket.sessionCount}",
                        modifier = Modifier.weight(1f)
                    )
                    FitbitMetricCapsule(
                        icon = Icons.Outlined.AccessTime,
                        iconTint = palette.primary,
                        label = "Duration",
                        value = FocusStatsRepository.formatDurationHero(bucket.durationMs),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FitbitMetricCapsule(
                        icon = Icons.Outlined.PieChart,
                        iconTint = palette.primarySecondary,
                        label = "Share",
                        value = "$periodSharePct%",
                        modifier = Modifier.weight(1f)
                    )
                    FitbitMetricCapsule(
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        iconTint = palette.success,
                        label = "Goal impact",
                        value = goalImpactValue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            val currentUnit = if (report.currentStreak == 1) "day" else "days"
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FitbitMetricCapsule(
                        icon = Icons.Outlined.Bolt,
                        iconTint = palette.primary,
                        label = "Sessions",
                        value = "${report.sessionCount}",
                        modifier = Modifier.weight(1f)
                    )
                    FitbitMetricCapsule(
                        icon = Icons.Outlined.AccessTime,
                        iconTint = palette.primarySecondary,
                        label = "Average",
                        value = formatDurationMinutes(report.avgSessionMs),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FitbitMetricCapsule(
                        icon = Icons.Outlined.Timer,
                        iconTint = palette.primary,
                        label = "Longest",
                        value = formatDurationMinutes(report.longestSessionMs),
                        modifier = Modifier.weight(1f)
                    )
                    FitbitMetricCapsule(
                        icon = Icons.Outlined.LocalFireDepartment,
                        iconTint = palette.primary,
                        label = "Streak",
                        value = "${report.currentStreak} $currentUnit",
                        trailingBadge = "Best ${report.maxStreak}d",
                        trailingBadgeIcon = Icons.Outlined.EmojiEvents,
                        trailingBadgeTint = palette.warning,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bento_streak_cell")
                    )
                }
            }
        }
    }
}

@Composable
private fun FitbitMetricCapsule(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingBadge: String? = null,
    trailingBadgeIcon: ImageVector? = null,
    trailingBadgeTint: Color = iconTint
) {
    val palette = LocalFocusTheme.current
    val capsuleShape = remember { RoundedCornerShape(26.dp) }

    Box(
        modifier = modifier
            .focusGlassPanel(shape = capsuleShape, palette = palette)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (trailingBadge != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surfaceElevated)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        if (trailingBadgeIcon != null) {
                            Icon(
                                imageVector = trailingBadgeIcon,
                                contentDescription = null,
                                tint = trailingBadgeTint,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = trailingBadge,
                            fontFamily = JetBrainsMono,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RollingMetricText(
                    text = value,
                    style = TextStyle(
                        fontFamily = palette.numeralFont,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.textPrimary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.5.sp,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
