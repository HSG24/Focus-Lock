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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BlockedAppGroup
import com.example.data.BlockedNotificationItem
import com.example.data.DynamicIslandEvent
import com.example.data.DynamicIslandRepository
import com.example.data.IslandPreviewKind
import com.example.data.IslandWidthPreset
import com.example.data.MilestoneKind
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlin.math.roundToInt

private enum class IslandStudioTab(val label: String) {
    LOOK("Look"),
    MODES("Modes"),
    BEHAVIOR("Behavior")
}

/**
 * Minimal, compact Nothing Dynamic Island Studio inside Settings.
 * Keeps every customization option cleanly organized into 3 lightweight tabs
 * (Look · Modes · Behavior) and automatically triggers a live top-of-screen island
 * preview for 3.5 seconds whenever any size, offset, or look setting changes.
 */
@Composable
fun DynamicIslandStudioSection() {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val islandState by DynamicIslandRepository.observe(context).collectAsStateWithLifecycle()
    val config = islandState.config

    var expanded by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(IslandStudioTab.LOOK) }
    var previewKind by remember { mutableStateOf(IslandPreviewKind.NOTIFICATION_SUMMARY) }
    var previewExpanded by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "island_studio_chevron"
    )

    val sampleEvent = remember(previewKind) {
        buildSampleIslandEvent(previewKind)
    }

    fun triggerLivePreview(kind: IslandPreviewKind = previewKind) {
        DynamicIslandRepository.showLivePreview(
            context = context,
            event = buildSampleIslandEvent(kind),
            durationMs = 3500L
        )
    }

    val statusSummary = if (!config.enabled) {
        "Off"
    } else {
        "${config.widthPreset.label} · ${if (config.useNothingMonoStyle) "Nothing Mono" else "App Theme"} · ${config.compactHeightDp}dp"
    }

    ThemedSettingsSectionCard(title = "DYNAMIC ISLAND") {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Minimal 1-row header with inline master switch + expand trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.tap()
                        expanded = !expanded
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("dynamic_island_studio_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RadioButtonChecked,
                            contentDescription = null,
                            tint = if (config.enabled) palette.primary else palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Nothing Island",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = statusSummary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { enabled ->
                            haptics.tick()
                            DynamicIslandRepository.updateConfig(context) { it.copy(enabled = enabled) }
                            if (enabled) triggerLivePreview()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = palette.onPrimary,
                            checkedTrackColor = palette.primary,
                            uncheckedThumbColor = palette.textSecondary,
                            uncheckedTrackColor = palette.surfaceElevated
                        ),
                        modifier = Modifier.testTag("switch_island_master")
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = palette.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Compact Interactive Preview Stage
                    val stageShape = remember { RoundedCornerShape(26.dp) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(stageShape)
                            .background(Color(0xFF0E1117), stageShape)
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                            .testTag("dynamic_island_live_preview_stage"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NothingDynamicIslandPill(
                            event = sampleEvent,
                            config = config,
                            isExpanded = previewExpanded,
                            onToggleExpand = {
                                if (config.enableHaptics) haptics.tick()
                                previewExpanded = !previewExpanded
                            },
                            onDismiss = { previewExpanded = false },
                            onClearNotifications = { previewExpanded = false },
                            onSkipScheduleToday = { previewExpanded = false },
                            isInteractivePreview = true
                        )

                        // Compact mode selector chips + Test Live button in one minimal row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                IslandPreviewKind.entries.forEach { kind ->
                                    val selected = kind == previewKind
                                    val chipLabel = when (kind) {
                                        IslandPreviewKind.NOTIFICATION_SUMMARY -> "Notifs"
                                        IslandPreviewKind.UPCOMING_SCHEDULE -> "Sched"
                                        IslandPreviewKind.MILESTONE -> "Goal"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (selected) Color.White.copy(alpha = 0.16f)
                                                else Color.White.copy(alpha = 0.05f)
                                            )
                                            .clickable {
                                                haptics.tick()
                                                previewKind = kind
                                                previewExpanded = false
                                                triggerLivePreview(kind)
                                            }
                                            .padding(horizontal = 9.dp, vertical = 5.dp)
                                            .testTag("island_preview_tab_${kind.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = chipLabel,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selected) Color.White else Color(0xFF9A9AA6)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(palette.primaryContainer)
                                    .clickable {
                                        haptics.tap()
                                        triggerLivePreview()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("test_live_island_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = "Test Live Island",
                                    tint = palette.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Test",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.primary
                                )
                            }
                        }
                    }

                    // Minimal 3-Tab Switcher: Look · Modes · Behavior (keeps only a few controls visible at once)
                    ElasticMorphingSegmentedPill(
                        segments = remember {
                            IslandStudioTab.entries.map {
                                ElasticPillSegment(
                                    label = it.label,
                                    testTag = "island_studio_tab_${it.name.lowercase()}"
                                )
                            }
                        },
                        selectedIndex = IslandStudioTab.entries.indexOf(activeTab),
                        onSelectIndex = { idx ->
                            activeTab = IslandStudioTab.entries.getOrNull(idx) ?: IslandStudioTab.LOOK
                        },
                        height = 38.dp,
                        fontSize = 12.sp
                    )

                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessMedium))
                        },
                        label = "island_studio_tab_body"
                    ) { tab ->
                        when (tab) {
                            IslandStudioTab.LOOK -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Theme & Width side-by-side or stacked compactly
                                    IslandCompactPillControl(
                                        label = "STYLE",
                                        options = listOf("Nothing Mono", "App Theme"),
                                        selectedIndex = if (config.useNothingMonoStyle) 0 else 1,
                                        onSelect = { idx ->
                                            DynamicIslandRepository.updateConfig(context) {
                                                it.copy(useNothingMonoStyle = idx == 0)
                                            }
                                            triggerLivePreview()
                                        }
                                    )

                                    IslandCompactPillControl(
                                        label = "WIDTH",
                                        options = IslandWidthPreset.entries.map { it.label },
                                        selectedIndex = IslandWidthPreset.entries.indexOf(config.widthPreset),
                                        onSelect = { idx ->
                                            DynamicIslandRepository.updateConfig(context) {
                                                it.copy(widthPreset = IslandWidthPreset.entries[idx])
                                            }
                                            triggerLivePreview()
                                        }
                                    )

                                    // Grouped minimal sliders capsule (Height + Top Offset)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(palette.surfaceElevated.copy(alpha = 0.65f))
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        CompactSliderRow(
                                            label = "HEIGHT",
                                            valueLabel = "${config.compactHeightDp} dp",
                                            value = config.compactHeightDp.toFloat(),
                                            range = 32f..48f,
                                            onChange = { v ->
                                                val dp = v.roundToInt().coerceIn(32, 48)
                                                if (dp != config.compactHeightDp) {
                                                    DynamicIslandRepository.updateConfig(context) {
                                                        it.copy(compactHeightDp = dp)
                                                    }
                                                }
                                                triggerLivePreview()
                                            }
                                        )
                                        CompactSliderRow(
                                            label = "TOP OFFSET",
                                            valueLabel = "${config.topOffsetDp} dp",
                                            value = config.topOffsetDp.toFloat(),
                                            range = 0f..36f,
                                            onChange = { v ->
                                                val dp = v.roundToInt().coerceIn(0, 36)
                                                if (dp != config.topOffsetDp) {
                                                    DynamicIslandRepository.updateConfig(context) {
                                                        it.copy(topOffsetDp = dp)
                                                    }
                                                }
                                                triggerLivePreview()
                                            }
                                        )
                                    }

                                    // Compact visual accent toggle chips (replaces 2 tall switch rows)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CompactTogglePillChip(
                                            label = "LED Dot",
                                            selected = config.showStatusLed,
                                            onClick = {
                                                haptics.tick()
                                                DynamicIslandRepository.updateConfig(context) {
                                                    it.copy(showStatusLed = !it.showStatusLed)
                                                }
                                                triggerLivePreview()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("switch_island_status_led")
                                        )
                                        CompactTogglePillChip(
                                            label = "Real App Icons",
                                            selected = config.useRealAppIcons,
                                            onClick = {
                                                haptics.tick()
                                                DynamicIslandRepository.updateConfig(context) {
                                                    it.copy(useRealAppIcons = !it.useRealAppIcons)
                                                }
                                                triggerLivePreview(IslandPreviewKind.NOTIFICATION_SUMMARY)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("switch_island_real_icons")
                                        )
                                    }
                                }
                            }

                            IslandStudioTab.MODES -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(palette.surfaceElevated.copy(alpha = 0.65f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    CompactSingleLineSwitchRow(
                                        label = "Notification Summary",
                                        checked = config.enableNotificationSummary,
                                        onCheckedChange = { v ->
                                            haptics.tick()
                                            DynamicIslandRepository.updateConfig(context) {
                                                it.copy(enableNotificationSummary = v)
                                            }
                                        },
                                        testTag = "switch_island_notif_summary"
                                    )

                                    CompactSingleLineSwitchRow(
                                        label = "Upcoming Schedule Alert",
                                        checked = config.enableScheduleAlert,
                                        onCheckedChange = { v ->
                                            haptics.tick()
                                            DynamicIslandRepository.updateConfig(context) {
                                                it.copy(enableScheduleAlert = v)
                                            }
                                        },
                                        testTag = "switch_island_schedule_alert"
                                    )

                                    AnimatedVisibility(visible = config.enableScheduleAlert) {
                                        val leadOptions = listOf(5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m")
                                        Column(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = "ALERT LEAD TIME",
                                                fontFamily = JetBrainsMono,
                                                fontSize = 9.5.sp,
                                                color = palette.textSecondary
                                            )
                                            ElasticMorphingSegmentedPill(
                                                segments = leadOptions.map { ElasticPillSegment(it.second) },
                                                selectedIndex = leadOptions.indexOfFirst { it.first == config.scheduleLeadMinutes }
                                                    .coerceAtLeast(1),
                                                onSelectIndex = { idx ->
                                                    DynamicIslandRepository.updateConfig(context) {
                                                        it.copy(scheduleLeadMinutes = leadOptions[idx].first)
                                                    }
                                                },
                                                height = 34.dp,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    CompactSingleLineSwitchRow(
                                        label = "Goal & Streak Milestones",
                                        checked = config.enableMilestones,
                                        onCheckedChange = { v ->
                                            haptics.tick()
                                            DynamicIslandRepository.updateConfig(context) {
                                                it.copy(enableMilestones = v)
                                            }
                                        },
                                        testTag = "switch_island_milestones"
                                    )
                                }
                            }

                            IslandStudioTab.BEHAVIOR -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val dismissOptions = listOf(4 to "4s", 8 to "8s", 14 to "14s", 0 to "Stay")
                                    IslandCompactPillControl(
                                        label = "AUTO-DISMISS",
                                        options = dismissOptions.map { it.second },
                                        selectedIndex = dismissOptions.indexOfFirst { it.first == config.autoDismissSeconds }
                                            .coerceAtLeast(0),
                                        onSelect = { idx ->
                                            DynamicIslandRepository.updateConfig(context) {
                                                it.copy(autoDismissSeconds = dismissOptions[idx].first)
                                            }
                                        }
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(palette.surfaceElevated.copy(alpha = 0.65f))
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        CompactSingleLineSwitchRow(
                                            label = "Keep Notifs Until Cleared",
                                            checked = config.notificationPersistUntilCleared,
                                            onCheckedChange = { v ->
                                                haptics.tick()
                                                DynamicIslandRepository.updateConfig(context) {
                                                    it.copy(notificationPersistUntilCleared = v)
                                                }
                                            },
                                            testTag = "switch_island_notif_persist"
                                        )
                                        CompactSingleLineSwitchRow(
                                            label = "Swipe Up to Dismiss",
                                            checked = config.swipeUpToDismiss,
                                            onCheckedChange = { v ->
                                                haptics.tick()
                                                DynamicIslandRepository.updateConfig(context) {
                                                    it.copy(swipeUpToDismiss = v)
                                                }
                                            },
                                            testTag = "switch_island_swipe_dismiss"
                                        )
                                        CompactSingleLineSwitchRow(
                                            label = "Haptic Pulse",
                                            checked = config.enableHaptics,
                                            onCheckedChange = { v ->
                                                if (v) haptics.tick()
                                                DynamicIslandRepository.updateConfig(context) {
                                                    it.copy(enableHaptics = v)
                                                }
                                            },
                                            testTag = "switch_island_haptics"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandCompactPillControl(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val palette = LocalFocusTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
            color = palette.textSecondary
        )
        ElasticMorphingSegmentedPill(
            segments = options.map { ElasticPillSegment(label = it) },
            selectedIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)),
            onSelectIndex = onSelect,
            height = 36.dp,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CompactSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    val palette = LocalFocusTheme.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = palette.textSecondary
            )
            Text(
                text = valueLabel,
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.primary,
                activeTrackColor = palette.primary,
                inactiveTrackColor = palette.background.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun CompactTogglePillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val shape = remember { RoundedCornerShape(50) }
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) palette.primaryContainer else palette.surfaceElevated.copy(alpha = 0.65f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (selected) palette.primary else palette.textSecondary.copy(alpha = 0.4f))
        )
        Text(
            text = "  $label",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) palette.primary else palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactSingleLineSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalFocusTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onPrimary,
                checkedTrackColor = palette.primary,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.background.copy(alpha = 0.65f)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun buildSampleIslandEvent(kind: IslandPreviewKind): DynamicIslandEvent {
    val now = System.currentTimeMillis()
    return when (kind) {
        IslandPreviewKind.NOTIFICATION_SUMMARY -> DynamicIslandEvent.NotificationSummary(
            id = "preview_notif",
            totalCount = 3,
            groups = listOf(
                BlockedAppGroup(
                    packageName = "com.whatsapp",
                    appLabel = "WhatsApp",
                    items = listOf(
                        BlockedNotificationItem(
                            id = "p1",
                            packageName = "com.whatsapp",
                            appLabel = "WhatsApp",
                            title = "Alex",
                            snippet = "Are we still on for the study call at 6?",
                            timestampMs = now - 120_000L
                        ),
                        BlockedNotificationItem(
                            id = "p2",
                            packageName = "com.whatsapp",
                            appLabel = "WhatsApp",
                            title = "Study Group",
                            snippet = "Uploaded today's chapter notes",
                            timestampMs = now - 420_000L
                        )
                    )
                ),
                BlockedAppGroup(
                    packageName = "com.instagram.android",
                    appLabel = "Instagram",
                    items = listOf(
                        BlockedNotificationItem(
                            id = "p3",
                            packageName = "com.instagram.android",
                            appLabel = "Instagram",
                            title = "maya.design",
                            snippet = "Sent you a reel",
                            timestampMs = now - 600_000L
                        )
                    )
                )
            )
        )
        IslandPreviewKind.UPCOMING_SCHEDULE -> DynamicIslandEvent.UpcomingSchedule(
            id = "preview_sched",
            scheduleId = "preview_id",
            scheduleName = "Deep Study",
            timeRangeLabel = "9:00 AM → 11:00 AM",
            startsAtMs = now + 4 * 60_000L + 12_000L
        )
        IslandPreviewKind.MILESTONE -> DynamicIslandEvent.Milestone(
            id = "preview_milestone",
            kind = MilestoneKind.GOAL_REACHED,
            title = "Daily Goal Complete",
            subtitle = "4h 0m focused today · +80 XP",
            badgeText = "100%"
        )
    }
}
