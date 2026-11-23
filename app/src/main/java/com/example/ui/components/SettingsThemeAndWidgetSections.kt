package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FocusScheduleRepository
import com.example.data.FocusSettingsState
import com.example.data.FocusXpState
import com.example.data.NothingWidgetKind
import com.example.data.WidgetCornerStyle
import com.example.data.WidgetStudioRepository
import com.example.data.WidgetSurfaceStyle
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.DotGothic16
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import java.util.Locale

@Composable
fun ThemedSettingsSectionCard(
    title: String,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalFocusTheme.current
    val cardShape = remember { RoundedCornerShape(28.dp) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemedSectionHeader(
            title = title,
            palette = palette,
            trailing = trailingAction,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusGlassPanel(
                    shape = cardShape,
                    palette = palette
                )
        ) {
            content()
        }
    }
}

@Composable
fun ThemedSettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = palette.textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { next ->
                haptics.toggle(next)
                onCheckedChange(next)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onPrimary,
                checkedTrackColor = palette.primary,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.surfaceElevated,
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

/**
 * Bespoke 3-theme visual gallery selector showcasing Liquid Glass, Nothing Dot, and Solar Amber
 * with live miniature canvas previews of each theme's unique aesthetic language.
 */
@Composable
fun AppearanceAndThemesSection(
    selectedTheme: AppVisualTheme,
    onSelectTheme: (AppVisualTheme) -> Unit
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val tileShape = remember { RoundedCornerShape(22.dp) }

    ThemedSettingsSectionCard(title = "THEME") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppVisualTheme.entries.forEach { themeOption ->
                val preview = remember(themeOption) { FocusThemePalette.from(themeOption) }
                val isSelected = selectedTheme == themeOption
                val interaction = remember { MutableInteractionSource() }
                val isTilePressed by interaction.collectIsPressedAsState()
                val scale = rememberSpringPressScale(interaction, pressedScale = 0.97f)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .focusGlassPanel(
                            shape = tileShape,
                            palette = palette,
                            elevated = isSelected,
                            isPressed = isTilePressed,
                            highlightAccent = if (isSelected) preview.primary else null,
                            showOrnaments = false
                        )
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptics.segment()
                                onSelectTheme(themeOption)
                            }
                        }
                        .padding(10.dp)
                        .testTag("theme_option_${themeOption.id}")
                ) {
                    ThemeMiniArtPreview(
                        theme = themeOption,
                        isSelected = isSelected,
                        modifier = Modifier
                            .width(96.dp)
                            .height(58.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = themeOption.displayName,
                                fontFamily = preview.displayFont,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = themeOption.badgeTag,
                                fontFamily = JetBrainsMono,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = preview.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(preview.primary.copy(alpha = 0.14f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = themeOption.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.5.sp,
                            color = palette.textSecondary,
                            maxLines = 2,
                            lineHeight = 15.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) preview.primary else palette.surfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected ${themeOption.displayName}",
                                tint = preview.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(preview.primary.copy(alpha = 0.65f))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Collapsible 6-widget Nothing Home Widget Studio with live previews and per-widget theming.
 */
@Composable
fun HomeScreenWidgetSection(
    settingsState: FocusSettingsState,
    xpState: FocusXpState,
    onToggleDottedRing: (Boolean) -> Unit,
    onToggleStudyHours: (Boolean) -> Unit,
    onToggleGoalPercent: (Boolean) -> Unit,
    onToggleStreakBadge: (Boolean) -> Unit,
    onToggleRankXpTag: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val studioState by WidgetStudioRepository.observe(context).collectAsStateWithLifecycle()
    val scheduleState by FocusScheduleRepository.observe(context).collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }
    var selectedKind by remember { mutableStateOf(NothingWidgetKind.GOAL_RING) }
    val activeConfig = studioState.configFor(selectedKind)

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "widget_chevron_rotation"
    )

    ThemedSettingsSectionCard(title = "HOME WIDGETS (6)") {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.tap()
                        expanded = !expanded
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("widget_studio_toggle"),
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
                            imageVector = Icons.Outlined.Widgets,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Nothing Widget Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (expanded) {
                                "Per-widget App Theme or Nothing Mono styling"
                            } else {
                                "6 Nothing-inspired widgets · live previews & options"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemedDivider(palette = palette)

                    // Horizontal widget selector pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NothingWidgetKind.entries.forEach { kind ->
                            val isSelected = kind == selectedKind
                            val chipBg by animateColorAsState(
                                targetValue = when {
                                    isSelected && palette.isLiquidGlass -> Color(0xFF262E3E)
                                    isSelected -> palette.primary
                                    else -> palette.surfaceElevated
                                },
                                label = "widget_kind_bg_${kind.id}"
                            )
                            val chipFg by animateColorAsState(
                                targetValue = when {
                                    isSelected && palette.isLiquidGlass -> palette.textPrimary
                                    isSelected -> palette.onPrimary
                                    else -> palette.textSecondary
                                },
                                label = "widget_kind_fg_${kind.id}"
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(chipBg)
                                    .clickable {
                                        haptics.segment()
                                        selectedKind = kind
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("widget_kind_chip_${kind.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = kind.shortTitle,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = chipFg
                                )
                                Text(
                                    text = kind.sizeBadge,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.5.sp,
                                    color = chipFg.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }

                    // Active widget title + subtitle
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${selectedKind.title} (${selectedKind.sizeBadge})",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = selectedKind.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.5.sp,
                            color = palette.textSecondary
                        )
                    }

                    // Live preview stage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(palette.background.copy(alpha = 0.7f))
                            .padding(vertical = 18.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NothingWidgetStudioLivePreview(
                            kind = selectedKind,
                            config = activeConfig,
                            settingsState = settingsState,
                            xpState = xpState,
                            schedules = scheduleState.schedules
                        )
                    }

                    // Per-widget theme mode: Nothing Mono vs App Theme
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "WIDGET THEME",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            letterSpacing = 1.1.sp,
                            color = palette.textSecondary
                        )
                        ElasticMorphingSegmentedPill(
                            segments = remember {
                                listOf(
                                    ElasticPillSegment("Nothing Mono"),
                                    ElasticPillSegment("App Theme")
                                )
                            },
                            selectedIndex = if (activeConfig.useNothingMono) 0 else 1,
                            onSelectIndex = { idx ->
                                haptics.segment()
                                WidgetStudioRepository.updateWidgetConfig(context, selectedKind) {
                                    it.copy(useNothingMono = idx == 0)
                                }
                            },
                            height = 40.dp,
                            fontSize = 12.sp,
                            useElevatedContainer = true
                        )
                    }

                    // Surface & Corner style row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SURFACE STYLE",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            letterSpacing = 1.1.sp,
                            color = palette.textSecondary
                        )
                        ElasticMorphingSegmentedPill(
                            segments = remember {
                                WidgetSurfaceStyle.entries.map { ElasticPillSegment(it.label) }
                            },
                            selectedIndex = WidgetSurfaceStyle.entries.indexOf(activeConfig.surfaceStyle).coerceAtLeast(0),
                            onSelectIndex = { idx ->
                                haptics.tick()
                                val next = WidgetSurfaceStyle.entries.getOrNull(idx) ?: WidgetSurfaceStyle.SOLID
                                WidgetStudioRepository.updateWidgetConfig(context, selectedKind) {
                                    it.copy(surfaceStyle = next)
                                }
                            },
                            height = 38.dp,
                            fontSize = 11.5.sp,
                            useElevatedContainer = true
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CORNER CURVE",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            letterSpacing = 1.1.sp,
                            color = palette.textSecondary
                        )
                        ElasticMorphingSegmentedPill(
                            segments = remember {
                                WidgetCornerStyle.entries.map { ElasticPillSegment(it.label) }
                            },
                            selectedIndex = WidgetCornerStyle.entries.indexOf(activeConfig.cornerStyle).coerceAtLeast(0),
                            onSelectIndex = { idx ->
                                haptics.tick()
                                val next = WidgetCornerStyle.entries.getOrNull(idx) ?: WidgetCornerStyle.PILL
                                WidgetStudioRepository.updateWidgetConfig(context, selectedKind) {
                                    it.copy(cornerStyle = next)
                                }
                            },
                            height = 38.dp,
                            fontSize = 11.5.sp,
                            useElevatedContainer = true
                        )
                    }

                    ThemedDivider(palette = palette)

                    ThemedSettingsSwitchRow(
                        title = "Nothing Status Dot",
                        subtitle = "Accent indicator dot beside widget label",
                        checked = activeConfig.showHeaderDots,
                        onCheckedChange = { checked ->
                            WidgetStudioRepository.updateWidgetConfig(context, selectedKind) {
                                it.copy(showHeaderDots = checked)
                            }
                        },
                        testTag = "switch_widget_header_dots"
                    )

                    ThemedSettingsSwitchRow(
                        title = "Secondary Metric",
                        subtitle = "Show extra pill (date, streak, best, repeat or %)",
                        checked = activeConfig.showSecondaryMetric,
                        onCheckedChange = { checked ->
                            WidgetStudioRepository.updateWidgetConfig(context, selectedKind) {
                                it.copy(showSecondaryMetric = checked)
                            }
                        },
                        testTag = "switch_widget_secondary_metric"
                    )

                    if (selectedKind == NothingWidgetKind.GOAL_RING ||
                        selectedKind == NothingWidgetKind.DOT_CLOCK ||
                        selectedKind == NothingWidgetKind.GOAL_BAR
                    ) {
                        ThemedSettingsSwitchRow(
                            title = "One-Tap Quick Lock",
                            subtitle = "Tapping the widget starts Focus Lock immediately",
                            checked = activeConfig.enableQuickLockTap,
                            onCheckedChange = { checked ->
                                WidgetStudioRepository.updateWidgetConfig(context, selectedKind) {
                                    it.copy(enableQuickLockTap = checked)
                                }
                            },
                            testTag = "switch_widget_quick_lock"
                        )
                    }

                    if (selectedKind == NothingWidgetKind.DOT_CLOCK ||
                        selectedKind == NothingWidgetKind.NEXT_SCHEDULE
                    ) {
                        ThemedSettingsSwitchRow(
                            title = "24-Hour Clock",
                            subtitle = "Display times in 24-hour format",
                            checked = activeConfig.use24HourClock,
                            onCheckedChange = { checked ->
                                WidgetStudioRepository.updateWidgetConfig(context, selectedKind) {
                                    it.copy(use24HourClock = checked)
                                }
                            },
                            testTag = "switch_widget_24h_clock"
                        )
                    }

                    if (selectedKind == NothingWidgetKind.GOAL_RING) {
                        ThemedDivider(palette = palette)

                        ThemedSettingsSwitchRow(
                            title = "Dotted Progress Ring",
                            subtitle = "Circular dot-matrix daily goal ring",
                            checked = settingsState.widgetShowDottedRing,
                            onCheckedChange = onToggleDottedRing,
                            testTag = "switch_widget_dotted_ring"
                        )
                        ThemedDivider(palette = palette)

                        ThemedSettingsSwitchRow(
                            title = "Today's Study Hours",
                            subtitle = "Display focused hours & minutes",
                            checked = settingsState.widgetShowStudyHours,
                            onCheckedChange = onToggleStudyHours,
                            testTag = "switch_widget_study_hours"
                        )
                        ThemedDivider(palette = palette)

                        ThemedSettingsSwitchRow(
                            title = "Daily Goal Progress %",
                            subtitle = "Percentage toward daily target",
                            checked = settingsState.widgetShowGoalPercent,
                            onCheckedChange = onToggleGoalPercent,
                            testTag = "switch_widget_goal_percent"
                        )
                        ThemedDivider(palette = palette)

                        ThemedSettingsSwitchRow(
                            title = "Current Streak Badge",
                            subtitle = "Consecutive study days pill",
                            checked = settingsState.widgetShowStreakBadge,
                            onCheckedChange = onToggleStreakBadge,
                            testTag = "switch_widget_streak_badge"
                        )
                        ThemedDivider(palette = palette)

                        ThemedSettingsSwitchRow(
                            title = "Rank & Focus XP Tag",
                            subtitle = "Progression tier and XP counter",
                            checked = settingsState.widgetShowRankXpTag,
                            onCheckedChange = onToggleRankXpTag,
                            testTag = "switch_widget_rank_xp"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyStudyGoalSection(
    dailyGoalMinutes: Long,
    onSelectGoalMinutes: (Long) -> Unit,
    onOpenCustomGoalDialog: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val selectedHours = (dailyGoalMinutes / 60L).toInt()
    val chipShape = remember { RoundedCornerShape(50) }
    val goalFormatted = remember(dailyGoalMinutes) {
        val h = dailyGoalMinutes / 60L
        val m = dailyGoalMinutes % 60L
        when {
            h > 0L && m > 0L -> "${h}h ${m}m"
            h > 0L -> "${h}h"
            else -> "${m}m"
        }
    }

    ThemedSettingsSectionCard(title = "DAILY GOAL") {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Target · $goalFormatted",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary
                    )
                }

                Text(
                    text = "Custom",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.5.sp,
                    color = palette.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.primaryContainer)
                        .clickable {
                            haptics.tap()
                            onOpenCustomGoalDialog()
                        }
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                        .testTag("settings_daily_goal_row")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..12).forEach { hours ->
                    val isSelected = selectedHours == hours && dailyGoalMinutes % 60L == 0L
                    val chipInteraction = remember { MutableInteractionSource() }
                    val pressScale = rememberSpringPressScale(chipInteraction, pressedScale = 0.92f)
                    val selectScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
                        label = "goal_hour_scale_$hours"
                    )
                    val chipBg by animateColorAsState(
                        targetValue = when {
                            isSelected && palette.isLiquidGlass -> Color(0xFF262E3E)
                            isSelected -> palette.primary
                            else -> palette.surfaceElevated
                        },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                        label = "goal_hour_bg_$hours"
                    )
                    val chipFg by animateColorAsState(
                        targetValue = when {
                            isSelected && palette.isLiquidGlass -> palette.textPrimary
                            isSelected -> palette.onPrimary
                            else -> palette.textSecondary
                        },
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                        label = "goal_hour_fg_$hours"
                    )
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .graphicsLayer {
                                val s = pressScale * selectScale
                                scaleX = s
                                scaleY = s
                            }
                            .clip(chipShape)
                            .background(color = chipBg, shape = chipShape)
                            .clickable(
                                interactionSource = chipInteraction,
                                indication = null
                            ) {
                                haptics.tick()
                                onSelectGoalMinutes(hours * 60L)
                            }
                            .padding(horizontal = 14.dp)
                            .testTag("goal_hour_${hours}h"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${hours}h",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = chipFg
                        )
                    }
                }
            }

            Text(
                text = "+10 XP/min · +100 XP goal bonus · -150 XP missed target",
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                color = palette.textSecondary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun ProtectionAndBlockersSection(
    blockNotifications: Boolean,
    notificationAccessGranted: Boolean,
    hasBlockedLog: Boolean,
    blockPowerMenu: Boolean,
    accessibilityGranted: Boolean,
    onToggleBlockNotifications: (Boolean) -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onOpenLastUnlockSummary: () -> Unit,
    onToggleBlockPowerMenu: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val pillShape = remember { RoundedCornerShape(50) }

    ThemedSettingsSectionCard(title = "SHIELD & GUARDS") {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemedSettingsSwitchRow(
                title = "Notification Shield",
                subtitle = "Silences distractions into an unlock digest (calls & alarms exempt)",
                checked = blockNotifications,
                onCheckedChange = onToggleBlockNotifications,
                testTag = "switch_block_notifications"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pillShape)
                    .clickable {
                        haptics.tap()
                        onOpenNotificationAccessSettings()
                    }
                    .padding(vertical = 4.dp)
                    .testTag("notification_access_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneInTalk,
                        contentDescription = null,
                        tint = if (notificationAccessGranted) palette.success else palette.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Notification Access",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Text(
                    text = if (notificationAccessGranted) "Active" else "Enable",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = if (notificationAccessGranted) palette.success else palette.primary,
                    modifier = Modifier
                        .clip(pillShape)
                        .background(
                            color = if (notificationAccessGranted) palette.success.copy(alpha = 0.12f) else palette.primaryContainer,
                            shape = pillShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (hasBlockedLog) {
                Surface(
                    onClick = {
                        haptics.tap()
                        onOpenLastUnlockSummary()
                    },
                    shape = RoundedCornerShape(50),
                    color = palette.surfaceElevated,
                    contentColor = palette.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("view_unlock_summary_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Last Unlock Summary", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            ThemedDivider(palette = palette)

            ThemedSettingsSwitchRow(
                title = "Power Menu Guard",
                subtitle = "Dismisses power & restart dialogs while locked",
                checked = blockPowerMenu,
                onCheckedChange = onToggleBlockPowerMenu,
                testTag = "switch_block_power_menu"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pillShape)
                    .clickable {
                        haptics.tap()
                        onOpenAccessibilitySettings()
                    }
                    .padding(vertical = 4.dp)
                    .testTag("accessibility_guard_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (accessibilityGranted) palette.success else palette.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Accessibility Service",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Text(
                    text = if (accessibilityGranted) "Active" else "Enable",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = if (accessibilityGranted) palette.success else palette.primary,
                    modifier = Modifier
                        .clip(pillShape)
                        .background(
                            color = if (accessibilityGranted) palette.success.copy(alpha = 0.12f) else palette.primaryContainer,
                            shape = pillShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
