package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    val cardShape = remember { RoundedCornerShape(18.dp) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = if (palette.useDottedDividers) DotGothic16 else palette.displayFont,
                letterSpacing = 1.2.sp,
                color = palette.textSecondary
            )
            trailingAction?.invoke()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(palette.surface, cardShape)
                .border(1.dp, palette.border, cardShape)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp,
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
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onPrimary,
                checkedTrackColor = palette.primary,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.surfaceElevated,
                uncheckedBorderColor = palette.border
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun AppearanceAndThemesSection(
    selectedTheme: AppVisualTheme,
    onSelectTheme: (AppVisualTheme) -> Unit
) {
    val palette = LocalFocusTheme.current
    val itemShape = remember { RoundedCornerShape(14.dp) }

    ThemedSettingsSectionCard(title = "APPEARANCE & THEMES") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Unified themes restyle both the app and the active lock screen.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = palette.textSecondary
            )

            AppVisualTheme.entries.forEach { themeOption ->
                val preview = remember(themeOption) { FocusThemePalette.from(themeOption) }
                val isSelected = selectedTheme == themeOption

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(itemShape)
                        .background(
                            color = if (isSelected) palette.primaryContainer else palette.surfaceElevated,
                            shape = itemShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) palette.primaryBorderTint else palette.border,
                            shape = itemShape
                        )
                        .clickable { onSelectTheme(themeOption) }
                        .padding(12.dp)
                        .testTag("theme_option_${themeOption.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Live miniature theme + lock preview swatch
                    ThemeMiniPreviewCard(preview = preview)

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
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 14.5.sp,
                                color = palette.textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = preview.primary.copy(alpha = 0.16f),
                                border = BorderStroke(0.75.dp, preview.primary.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = themeOption.badgeTag,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = preview.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = themeOption.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.5.sp,
                            color = palette.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectTheme(themeOption) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = palette.primary,
                            unselectedColor = palette.textSecondary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeMiniPreviewCard(preview: FocusThemePalette) {
    val swatchShape = remember { RoundedCornerShape(12.dp) }
    Box(
        modifier = Modifier
            .size(width = 62.dp, height = 58.dp)
            .clip(swatchShape)
            .background(preview.background)
            .border(1.dp, preview.borderHighlight, swatchShape)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "02:45",
                fontFamily = preview.numeralFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = preview.textPrimary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(preview.surface)
                    .border(0.75.dp, preview.primary.copy(alpha = 0.55f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(preview.primary)
                    )
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(preview.textSecondary.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

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

    ThemedSettingsSectionCard(title = "HOME SCREEN WIDGET (2X2)") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Live Nothing OS 2x2 widget preview. Toggle any element below and placed widgets refresh immediately.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = palette.textSecondary
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
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
                subtitle = "Display focused hours & minutes (02H 45M)",
                checked = settingsState.widgetShowStudyHours,
                onCheckedChange = onToggleStudyHours,
                testTag = "switch_widget_study_hours"
            )
            ThemedDivider(palette = palette)

            ThemedSettingsSwitchRow(
                title = "Daily Goal Progress %",
                subtitle = "Percentage toward daily study target",
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
                subtitle = "Current progression tier and XP counter",
                checked = settingsState.widgetShowRankXpTag,
                onCheckedChange = onToggleRankXpTag,
                testTag = "switch_widget_rank_xp"
            )
        }
    }
}

@Composable
private fun NothingWidgetLivePreview(
    settingsState: FocusSettingsState,
    xpState: FocusXpState
) {
    val widgetCardBg = Color(0xFF1B1B1D)
    val widgetChipBg = Color(0xFF232327)
    val widgetChipBorder = Color(0xFF3A3A40)
    val nothingRed = Color(0xFFD71921)
    val widgetBorder = Color(0xFF2D2D32)

    val goalMs = settingsState.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
    val sampleMs = xpState.todayFocusMs.takeIf { it > 0L } ?: (60_000L)
    val fraction = (sampleMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
    val pct = (fraction * 100f).toInt().coerceIn(0, 999)
    val hours = sampleMs / 3_600_000L
    val mins = (sampleMs % 3_600_000L) / 60_000L
    val timeLabel = String.format(Locale.US, "%02dH %02dM", hours, mins)

    val cardSize = 200.dp
    val ringSize = cardSize * 0.64f

    Box(
        modifier = Modifier
            .size(cardSize)
            .clip(RoundedCornerShape(26.dp))
            .background(widgetCardBg)
            .border(1.dp, widgetBorder, RoundedCornerShape(26.dp))
    ) {
        // Top Row anchored 14dp from top edge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 14.dp, start = 14.dp, end = 14.dp),
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
                    fontSize = 10.sp,
                    color = Color.White,
                    letterSpacing = 0.9.sp
                )
            }

            if (settingsState.widgetShowStreakBadge) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(widgetChipBg)
                        .border(1.dp, widgetChipBorder, RoundedCornerShape(9.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${xpState.currentStreak.coerceAtLeast(1)}D",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Center Ring (64% height) or Compact Readout
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
                        val headIndex = (activeCount - 1).coerceAtLeast(0)

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
                                    else -> Color(0xFF3A3A40)
                                },
                                radius = when {
                                    isHead -> 2.4.dp.toPx()
                                    isCompleted -> 1.8.dp.toPx()
                                    else -> 1.75.dp.toPx()
                                },
                                center = Offset(dx, dy)
                            )
                        }

                        // 4 subtle cardinal accent ticks inside the ring
                        val tickOuter = radius - 4.5.dp.toPx()
                        val tickInner = radius - 8.dp.toPx()
                        for (deg in listOf(-90.0, 0.0, 90.0, 180.0)) {
                            val rad = Math.toRadians(deg)
                            drawLine(
                                color = Color(0xFF5A5A64),
                                start = Offset(cx + (tickInner * cos(rad)).toFloat(), cy + (tickInner * sin(rad)).toFloat()),
                                end = Offset(cx + (tickOuter * cos(rad)).toFloat(), cy + (tickOuter * sin(rad)).toFloat()),
                                strokeWidth = 1.2.dp.toPx()
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (settingsState.widgetShowGoalPercent) {
                            Text(
                                text = "$pct%",
                                fontFamily = DotGothic16,
                                fontSize = 12.sp,
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (settingsState.widgetShowGoalPercent) {
                        Text(
                            text = "$pct%",
                            fontFamily = DotGothic16,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = nothingRed
                        )
                    }
                }
            }
        }

        // Footer centered 14dp from bottom edge
        if (settingsState.widgetShowRankXpTag) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = xpState.rank.title.uppercase(Locale.US),
                    fontFamily = JetBrainsMono,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = " • ${xpState.totalXp} XP",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.5.sp,
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
    val selectedHours = (dailyGoalMinutes / 60L).toInt()
    val chipShape = remember { RoundedCornerShape(10.dp) }

    ThemedSettingsSectionCard(title = "DAILY STUDY HOUR GOAL & XP PENALTY") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Daily Goal (${dailyGoalMinutes / 60}h ${if (dailyGoalMinutes % 60 > 0) "${dailyGoalMinutes % 60}m" else ""})".trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        color = palette.textPrimary
                    )
                }

                Text(
                    text = "Custom",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.5.sp,
                    color = palette.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenCustomGoalDialog)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("settings_daily_goal_row")
                )
            }

            Text(
                text = "Earn +10 XP/min +100 XP goal bonus. Falling short deducts -150 XP at midnight.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = palette.textSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..12).forEach { hours ->
                    val isSelected = selectedHours == hours && dailyGoalMinutes % 60L == 0L
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(chipShape)
                            .background(
                                color = if (isSelected) palette.primaryContainer else palette.surfaceElevated,
                                shape = chipShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) palette.primaryBorderTint else palette.border,
                                shape = chipShape
                            )
                            .clickable { onSelectGoalMinutes(hours * 60L) }
                            .padding(horizontal = 14.dp)
                            .testTag("goal_hour_${hours}h"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${hours}h",
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) palette.primary else palette.textSecondary
                        )
                    }
                }
            }
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
    val pillShape = remember { RoundedCornerShape(50) }

    ThemedSettingsSectionCard(title = "NOTIFICATION & POWER BLOCKERS") {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Notification Shield",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    color = palette.textPrimary
                )
            }

            ThemedSettingsSwitchRow(
                title = "Block Notifications During Lock",
                subtitle = "Dismisses distracting notifications into an unlock summary",
                checked = blockNotifications,
                onCheckedChange = onToggleBlockNotifications,
                testTag = "switch_block_notifications"
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneInTalk,
                        contentDescription = null,
                        tint = palette.success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Call-Safe: Incoming & ongoing phone calls and alarms are never blocked.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.5.sp,
                        color = palette.textSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenNotificationAccessSettings)
                    .padding(vertical = 4.dp)
                    .testTag("notification_access_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Notification Listener Access",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary
                )
                Text(
                    text = if (notificationAccessGranted) "Granted" else "Enable Access",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = if (notificationAccessGranted) palette.success else palette.primary,
                    modifier = Modifier
                        .clip(pillShape)
                        .background(
                            color = if (notificationAccessGranted) palette.success.copy(alpha = 0.14f) else palette.primaryContainer,
                            shape = pillShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (notificationAccessGranted) palette.success.copy(alpha = 0.35f) else palette.primaryBorderTint,
                            shape = pillShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (hasBlockedLog) {
                OutlinedButton(
                    onClick = onOpenLastUnlockSummary,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.primaryBorderTint),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("view_unlock_summary_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Last Unlock Summary", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            ThemedDivider(palette = palette)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Power Menu & Restart Guard",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                    color = palette.textPrimary
                )
            }

            ThemedSettingsSwitchRow(
                title = "Block Power Menu During Lock",
                subtitle = "Dismisses power/restart dialogs to prevent timer bypasses",
                checked = blockPowerMenu,
                onCheckedChange = onToggleBlockPowerMenu,
                testTag = "switch_block_power_menu"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenAccessibilitySettings)
                    .padding(vertical = 4.dp)
                    .testTag("accessibility_guard_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (accessibilityGranted) palette.success else palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Accessibility Power Guard",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary
                    )
                }
                Text(
                    text = if (accessibilityGranted) "Active" else "Enable Service",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = if (accessibilityGranted) palette.success else palette.primary,
                    modifier = Modifier
                        .clip(pillShape)
                        .background(
                            color = if (accessibilityGranted) palette.success.copy(alpha = 0.14f) else palette.primaryContainer,
                            shape = pillShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (accessibilityGranted) palette.success.copy(alpha = 0.35f) else palette.primaryBorderTint,
                            shape = pillShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
