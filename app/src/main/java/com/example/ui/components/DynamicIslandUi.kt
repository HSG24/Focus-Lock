package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlockedAppGroup
import com.example.data.DynamicIslandConfig
import com.example.data.DynamicIslandEvent
import com.example.data.DynamicIslandRepository
import com.example.data.DynamicIslandState
import com.example.data.FocusScheduleRepository
import com.example.data.MilestoneKind
import com.example.data.NotificationBlockerRepository
import com.example.ui.theme.DotGothic16
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live overlay host used both inside MainActivity (in-app fallback) and inside
 * DynamicIslandOverlayService (system-wide floating pill).
 */
@Composable
fun DynamicIslandOverlayHost(
    state: DynamicIslandState,
    isSystemOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberFocusHaptics()
    val event = state.activeEvent
    val config = state.config

    // Auto-dismiss countdown (paused while expanded; live preview always auto-hides after previewAutoHideMs)
    LaunchedEffect(
        event?.id,
        state.previewPulseSeq,
        state.previewAutoHideMs,
        state.isExpanded,
        config.autoDismissSeconds,
        config.notificationPersistUntilCleared
    ) {
        if (event == null || state.isExpanded) return@LaunchedEffect
        val previewMs = state.previewAutoHideMs
        if (previewMs != null) {
            delay(previewMs)
            DynamicIslandRepository.dismissActive(context)
            return@LaunchedEffect
        }
        val isNotifPersistent = event is DynamicIslandEvent.NotificationSummary && config.notificationPersistUntilCleared
        if (isNotifPersistent || config.autoDismissSeconds <= 0) return@LaunchedEffect
        delay(config.autoDismissSeconds * 1000L)
        DynamicIslandRepository.dismissActive(context)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!isSystemOverlay) Modifier.statusBarsPadding() else Modifier)
            .padding(top = config.topOffsetDp.dp, start = 14.dp, end = 14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = event != null && (config.enabled || state.previewAutoHideMs != null),
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
            ) { -it } + fadeIn() + scaleIn(initialScale = 0.86f),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)
            ) { -it } + fadeOut() + scaleOut(targetScale = 0.88f)
        ) {
            if (event != null) {
                NothingDynamicIslandPill(
                    event = event,
                    config = config,
                    isExpanded = state.isExpanded,
                    onToggleExpand = {
                        if (config.enableHaptics) haptics.tick()
                        DynamicIslandRepository.setExpanded(context, !state.isExpanded)
                    },
                    onDismiss = {
                        if (config.enableHaptics) haptics.tap()
                        if (event is DynamicIslandEvent.NotificationSummary) {
                            NotificationBlockerRepository.dismissSummarySheet(context)
                        }
                        DynamicIslandRepository.dismissActive(context)
                    },
                    onClearNotifications = {
                        if (config.enableHaptics) haptics.confirm()
                        NotificationBlockerRepository.clearSummaryLog(context)
                        DynamicIslandRepository.dismissActive(context)
                    },
                    onSkipScheduleToday = { scheduleId ->
                        if (config.enableHaptics) haptics.confirm()
                        FocusScheduleRepository.markSkippedForToday(context, scheduleId)
                        DynamicIslandRepository.dismissActive(context)
                    }
                )
            }
        }
    }
}

/**
 * Reusable Nothing dot-matrix Dynamic Island pill.
 * Used identically for live events and the interactive Settings -> Dynamic Island Studio previews.
 */
@Composable
fun NothingDynamicIslandPill(
    event: DynamicIslandEvent,
    config: DynamicIslandConfig,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
    onClearNotifications: () -> Unit,
    onSkipScheduleToday: (String) -> Unit,
    isInteractivePreview: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val useMono = config.useNothingMonoStyle

    val containerBg = if (useMono) Color(0xFF060608) else palette.surface
    val elevatedBg = if (useMono) Color(0xFF141418) else palette.surfaceElevated
    val primaryText = if (useMono) Color(0xFFFAFAFC) else palette.textPrimary
    val secondaryText = if (useMono) Color(0xFF9A9AA6) else palette.textSecondary
    val accentDot = when {
        event is DynamicIslandEvent.Milestone && event.kind == MilestoneKind.GOAL_REACHED -> palette.success
        event is DynamicIslandEvent.Milestone && event.kind == MilestoneKind.STREAK_EXTENDED -> palette.warning
        useMono -> Color(0xFFFF2E38)
        else -> palette.primary
    }
    val onAccentColor = if (useMono) Color.White else palette.onPrimary

    val targetWidth by animateDpAsState(
        targetValue = if (isExpanded) 348.dp else config.widthPreset.compactWidthDp.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "island_width"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 30.dp else (config.compactHeightDp / 2).dp,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
        label = "island_corner"
    )
    val pillShape = RoundedCornerShape(cornerRadius)
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    val dragModifier = if (config.swipeUpToDismiss && !isInteractivePreview) {
        Modifier.pointerInput(event.id) {
            detectVerticalDragGestures(
                onDragStart = { dragAccumulated = 0f },
                onVerticalDrag = { _, dragAmount ->
                    dragAccumulated += dragAmount
                    if (dragAccumulated < -48f) {
                        dragAccumulated = 0f
                        onDismiss()
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .widthIn(max = targetWidth)
            .fillMaxWidth()
            .clip(pillShape)
            .background(containerBg, pillShape)
            .then(dragModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleExpand
            )
            .testTag(if (isExpanded) "dynamic_island_expanded" else "dynamic_island_compact")
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                    fadeOut(spring(stiffness = Spring.StiffnessMedium))
            },
            label = "island_content_morph"
        ) { expanded ->
            if (!expanded) {
                CompactIslandContent(
                    event = event,
                    config = config,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    accentDot = accentDot,
                    elevatedBg = elevatedBg
                )
            } else {
                ExpandedIslandContent(
                    event = event,
                    config = config,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    accentDot = accentDot,
                    onAccentColor = onAccentColor,
                    elevatedBg = elevatedBg,
                    onCollapse = onToggleExpand,
                    onDismiss = onDismiss,
                    onClearNotifications = onClearNotifications,
                    onSkipScheduleToday = onSkipScheduleToday
                )
            }
        }
    }
}

@Composable
private fun CompactIslandContent(
    event: DynamicIslandEvent,
    config: DynamicIslandConfig,
    primaryText: Color,
    secondaryText: Color,
    accentDot: Color,
    elevatedBg: Color
) {
    var nowMs by remember(event.id) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(event) {
        if (event is DynamicIslandEvent.UpcomingSchedule) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(config.compactHeightDp.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (event) {
            is DynamicIslandEvent.NotificationSummary -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (config.showStatusLed) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentDot)
                        )
                    }
                    // Tiny overlapping real app icons preview inside the compact island
                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                        event.groups.take(3).forEach { grp ->
                            RealAppIconCircle(
                                packageName = grp.packageName,
                                appLabel = grp.appLabel,
                                useRealIcon = config.useRealAppIcons,
                                sizeDp = 20,
                                fallbackBg = elevatedBg,
                                fallbackTint = accentDot
                            )
                        }
                    }
                    Text(
                        text = "${event.totalCount} BLOCKED",
                        fontFamily = DotGothic16,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "TAP",
                    fontFamily = JetBrainsMono,
                    fontSize = 9.5.sp,
                    color = secondaryText
                )
            }

            is DynamicIslandEvent.UpcomingSchedule -> {
                val remSec = ((event.startsAtMs - nowMs).coerceAtLeast(0L)) / 1000L
                val mm = remSec / 60L
                val ss = remSec % 60L
                val timerText = String.format(Locale.US, "%02d:%02d", mm, ss)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (config.showStatusLed) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentDot)
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = event.scheduleName.uppercase(Locale.US),
                        fontFamily = DotGothic16,
                        fontSize = 12.sp,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = timerText,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentDot
                )
            }

            is DynamicIslandEvent.Milestone -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (config.showStatusLed) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentDot)
                        )
                    }
                    Icon(
                        imageVector = milestoneIconFor(event.kind),
                        contentDescription = null,
                        tint = accentDot,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = event.title,
                        fontFamily = DotGothic16,
                        fontSize = 12.sp,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = event.badgeText,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentDot
                )
            }
        }
    }
}

@Composable
private fun ExpandedIslandContent(
    event: DynamicIslandEvent,
    config: DynamicIslandConfig,
    primaryText: Color,
    secondaryText: Color,
    accentDot: Color,
    onAccentColor: Color,
    elevatedBg: Color,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    onClearNotifications: () -> Unit,
    onSkipScheduleToday: (String) -> Unit
) {
    val pillShape = remember { RoundedCornerShape(50) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (event) {
            is DynamicIslandEvent.NotificationSummary -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (config.showStatusLed) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentDot)
                            )
                        }
                        Column {
                            Text(
                                text = "BLOCKED NOTIFICATIONS",
                                fontFamily = DotGothic16,
                                fontSize = 13.sp,
                                letterSpacing = 0.8.sp,
                                color = primaryText
                            )
                            Text(
                                text = "${event.totalCount} silenced across ${event.groups.size} app${if (event.groups.size == 1) "" else "s"}",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.5.sp,
                                color = secondaryText
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(elevatedBg)
                            .clickable(onClick = onCollapse),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Collapse",
                            tint = secondaryText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Scrollable notification list showing each app's real full-color icon
                val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    event.groups.forEach { group ->
                        IslandBlockedAppGroupRow(
                            group = group,
                            useRealIcon = config.useRealAppIcons,
                            timeFmt = timeFmt,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            accentDot = accentDot,
                            elevatedBg = elevatedBg
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .background(elevatedBg, pillShape)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keep",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryText
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .background(accentDot, pillShape)
                            .clickable(onClick = onClearNotifications)
                            .padding(vertical = 9.dp)
                            .testTag("island_clear_notifications_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onAccentColor
                        )
                    }
                }
            }

            is DynamicIslandEvent.UpcomingSchedule -> {
                var nowMs by remember(event.id) { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(event.id) {
                    while (true) {
                        nowMs = System.currentTimeMillis()
                        delay(1000L)
                    }
                }
                val remSec = ((event.startsAtMs - nowMs).coerceAtLeast(0L)) / 1000L
                val timerText = String.format(Locale.US, "%02d:%02d", remSec / 60L, remSec % 60L)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(accentDot.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = accentDot,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = event.scheduleName,
                                fontFamily = DotGothic16,
                                fontSize = 14.sp,
                                color = primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${event.timeRangeLabel} · Locks in $timerText",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.5.sp,
                                color = secondaryText
                            )
                        }
                    }
                }

                // Skip Today ONLY appears when expanded (as requested by user)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .background(elevatedBg, pillShape)
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Dismiss",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryText
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1.25f)
                            .clip(pillShape)
                            .background(accentDot, pillShape)
                            .clickable { onSkipScheduleToday(event.scheduleId) }
                            .padding(vertical = 9.dp, horizontal = 10.dp)
                            .testTag("island_skip_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SkipNext,
                            contentDescription = null,
                            tint = onAccentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Skip Today",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onAccentColor
                        )
                    }
                }
            }

            is DynamicIslandEvent.Milestone -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentDot.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = milestoneIconFor(event.kind),
                            contentDescription = null,
                            tint = accentDot,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = event.title,
                            fontFamily = DotGothic16,
                            fontSize = 14.5.sp,
                            color = primaryText
                        )
                        Text(
                            text = event.subtitle,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = secondaryText
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(accentDot.copy(alpha = 0.18f), pillShape)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = event.badgeText,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentDot
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(pillShape)
                        .background(elevatedBg, pillShape)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dismiss",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun IslandBlockedAppGroupRow(
    group: BlockedAppGroup,
    useRealIcon: Boolean,
    timeFmt: SimpleDateFormat,
    primaryText: Color,
    secondaryText: Color,
    accentDot: Color,
    elevatedBg: Color
) {
    val rowShape = remember { RoundedCornerShape(20.dp) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(elevatedBg, rowShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            RealAppIconCircle(
                packageName = group.packageName,
                appLabel = group.appLabel,
                useRealIcon = useRealIcon,
                sizeDp = 28,
                fallbackBg = Color(0xFF22222A),
                fallbackTint = accentDot
            )
            Text(
                text = group.appLabel,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentDot.copy(alpha = 0.16f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${group.count}",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentDot
                )
            }
        }

        group.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryText.copy(alpha = 0.94f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.snippet.isNotBlank()) {
                        Text(
                            text = item.snippet,
                            fontSize = 11.sp,
                            color = secondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeFmt.format(Date(item.timestampMs)),
                    fontFamily = JetBrainsMono,
                    fontSize = 9.5.sp,
                    color = secondaryText.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun RealAppIconCircle(
    packageName: String,
    appLabel: String,
    useRealIcon: Boolean,
    sizeDp: Int,
    fallbackBg: Color,
    fallbackTint: Color
) {
    val context = LocalContext.current
    val realIcon by produceState<ImageBitmap?>(
        initialValue = if (useRealIcon) DynamicIslandRepository.getCachedRealAppIcon(packageName) else null,
        key1 = packageName,
        key2 = useRealIcon
    ) {
        if (useRealIcon && value == null) {
            value = DynamicIslandRepository.loadRealAppIconAsync(context, packageName)
        }
    }

    val badgeColor = remember(packageName, fallbackBg) {
        when {
            packageName.contains("whatsapp") -> Color(0xFF25D366)
            packageName.contains("instagram") -> Color(0xFFE1306C)
            packageName.contains("discord") -> Color(0xFF5865F2)
            packageName.contains("gm") || packageName.contains("mail") -> Color(0xFFEA4335)
            else -> fallbackBg
        }
    }

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(if (realIcon != null) Color.Transparent else badgeColor),
        contentAlignment = Alignment.Center
    ) {
        if (realIcon != null) {
            Image(
                bitmap = realIcon!!,
                contentDescription = appLabel,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
            )
        } else if (appLabel.isNotBlank() && useRealIcon) {
            Text(
                text = appLabel.first().uppercaseChar().toString(),
                fontSize = (sizeDp * 0.44f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = appLabel,
                tint = fallbackTint,
                modifier = Modifier.size((sizeDp * 0.56f).dp)
            )
        }
    }
}

private fun milestoneIconFor(kind: MilestoneKind) = when (kind) {
    MilestoneKind.GOAL_REACHED -> Icons.Outlined.EmojiEvents
    MilestoneKind.HALF_GOAL -> Icons.Outlined.AutoAwesome
    MilestoneKind.STREAK_EXTENDED -> Icons.Outlined.LocalFireDepartment
    MilestoneKind.RANK_UP -> Icons.Outlined.AutoAwesome
}
