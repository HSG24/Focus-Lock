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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusSettingsState
import com.example.data.FocusXpState
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.DotGothic16
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ThemedSettingsSectionCard(
    title: String,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalFocusTheme.current
    val cardShape = remember { RoundedCornerShape(28.dp) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                letterSpacing = 1.4.sp,
                color = palette.textSecondary
            )
            trailingAction?.invoke()
        }
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
 * Minimal 4-column theme swatch selector replacing tall vertical theme cards.
 */
@Composable
fun AppearanceAndThemesSection(
    selectedTheme: AppVisualTheme,
    onSelectTheme: (AppVisualTheme) -> Unit
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val tileShape = remember { RoundedCornerShape(24.dp) }

    ThemedSettingsSectionCard(title = "THEME") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppVisualTheme.entries.forEach { themeOption ->
                    val preview = remember(themeOption) { FocusThemePalette.from(themeOption) }
                    val isSelected = selectedTheme == themeOption
                    val interaction = remember { MutableInteractionSource() }
                    val isTilePressed by interaction.collectIsPressedAsState()
                    val scale = rememberSpringPressScale(interaction, pressedScale = 0.94f)
                    val shortName = remember(themeOption) {
                        when (themeOption) {
                            AppVisualTheme.NOTHING_CRIMSON -> "Crimson"
                            AppVisualTheme.LIQUID_GLASS -> "Glass"
                            AppVisualTheme.SOLAR_AMBER -> "Amber"
                            AppVisualTheme.NORDIC_SAGE -> "Sage"
                        }
                    }

                    val tileModifier = if (palette.isLiquidGlass) {
                        Modifier.focusGlassPanel(
                            shape = tileShape,
                            palette = palette,
                            elevated = isSelected,
                            isPressed = isTilePressed,
                            highlightAccent = if (isSelected) palette.primary else null
                        )
                    } else {
                        Modifier
                            .clip(tileShape)
                            .background(
                                color = if (isSelected) palette.primaryContainer else palette.surfaceElevated.copy(alpha = 0.7f),
                                shape = tileShape
                            )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .then(tileModifier)
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptics.segment()
                                    onSelectTheme(themeOption)
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("theme_option_${themeOption.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .then(
                                    if (themeOption == AppVisualTheme.LIQUID_GLASS) {
                                        Modifier.background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF2B3445),
                                                    Color(0xFF0A0D13),
                                                    Color(0xFF1C2330)
                                                )
                                            ),
                                            CircleShape
                                        )
                                    } else {
                                        Modifier.background(preview.background, CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = themeOption.displayName,
                                    tint = preview.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(preview.primary)
                                )
                            }
                        }

                        Text(
                            text = shortName,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) palette.textPrimary else palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Text(
                text = "${selectedTheme.displayName} · ${selectedTheme.subtitle}",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.5.sp,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

/**
 * Collapsible Home Widget studio so Settings stays uncluttered until customized.
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
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "widget_chevron_rotation"
    )

    ThemedSettingsSectionCard(title = "HOME WIDGET") {
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
                            text = "2×2 Widget Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (expanded) "Live preview & visibility toggles" else "Tap to customize ring, hours & rank",
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NothingWidgetLivePreview(
                            settingsState = settingsState,
                            xpState = xpState
                        )
                    }

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

@Composable
private fun NothingWidgetLivePreview(
    settingsState: FocusSettingsState,
    xpState: FocusXpState
) {
    val widgetCardBg = Color(0xFF16161A)
    val widgetChipBg = Color(0xFF202026)
    val widgetChipBorder = Color(0xFF33333C)
    val nothingRed = Color(0xFFFF242E)
    val widgetBorder = Color(0xFF282830)

    val goalMs = settingsState.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
    val sampleMs = xpState.todayFocusMs
    val fraction = (sampleMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
    val pct = (fraction * 100f).toInt().coerceIn(0, 999)
    val hours = sampleMs / 3_600_000L
    val mins = (sampleMs % 3_600_000L) / 60_000L
    val timeLabel = String.format(Locale.US, "%02dH %02dM", hours, mins)

    val cardSize = 186.dp
    val ringSize = cardSize * 0.64f

    Box(
        modifier = Modifier
            .size(cardSize)
            .clip(RoundedCornerShape(32.dp))
            .background(widgetCardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 13.dp, start = 13.dp, end = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(nothingRed)
                )
                Text(
                    text = "FOCUS LOCK",
                    fontFamily = DotGothic16,
                    fontSize = 9.5.sp,
                    color = Color.White,
                    letterSpacing = 0.9.sp
                )
            }

            if (settingsState.widgetShowStreakBadge) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(widgetChipBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${xpState.currentStreak}D",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            if (settingsState.widgetShowDottedRing) {
                Box(
                    modifier = Modifier.size(ringSize),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = size.minDimension * 0.44f
                        val dots = 40
                        val activeCount = if (fraction > 0f) {
                            (dots * fraction).toInt().coerceIn(1, dots)
                        } else {
                            0
                        }
                        val headIndex = if (activeCount > 0) activeCount - 1 else -1

                        for (i in 0 until dots) {
                            val angle = Math.toRadians(-90.0 + (360.0 / dots) * i)
                            val dx = cx + (radius * cos(angle)).toFloat()
                            val dy = cy + (radius * sin(angle)).toFloat()
                            val isHead = i == headIndex
                            val isCompleted = i < activeCount && !isHead
                            drawCircle(
                                color = when {
                                    isHead -> nothingRed
                                    isCompleted -> Color.White
                                    else -> Color(0xFF34343C)
                                },
                                radius = when {
                                    isHead -> 2.3.dp.toPx()
                                    isCompleted -> 1.7.dp.toPx()
                                    else -> 1.5.dp.toPx()
                                },
                                center = Offset(dx, dy)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (settingsState.widgetShowStudyHours) {
                            Text(
                                text = timeLabel,
                                fontFamily = DotGothic16,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (settingsState.widgetShowGoalPercent) {
                            Text(
                                text = "$pct%",
                                fontFamily = DotGothic16,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = nothingRed
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (settingsState.widgetShowStudyHours) {
                        Text(
                            text = timeLabel,
                            fontFamily = DotGothic16,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (settingsState.widgetShowGoalPercent) {
                        Text(
                            text = "$pct%",
                            fontFamily = DotGothic16,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = nothingRed
                        )
                    }
                }
            }
        }

        if (settingsState.widgetShowRankXpTag) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 13.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = xpState.rank.title.uppercase(Locale.US),
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = " · ${xpState.totalXp} XP",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = Color(0xFFA0A0A8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
