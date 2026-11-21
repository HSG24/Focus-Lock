package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusSchedule
import com.example.data.FocusScheduleRepository
import com.example.data.FocusSettingsState
import com.example.data.FocusXpState
import com.example.data.NothingWidgetKind
import com.example.data.SingleWidgetConfig
import com.example.ui.theme.DotGothic16
import com.example.ui.theme.JetBrainsMono
import com.example.widget.WidgetVisualPalette
import com.example.widget.resolveWidgetPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun NothingWidgetStudioLivePreview(
    kind: NothingWidgetKind,
    config: SingleWidgetConfig,
    settingsState: FocusSettingsState,
    xpState: FocusXpState,
    schedules: List<FocusSchedule>
) {
    val palette = remember(config, settingsState.appTheme) {
        resolveWidgetPalette(config, settingsState.appTheme)
    }

    when (kind) {
        NothingWidgetKind.GOAL_RING -> GoalRingWidgetPreview(config, palette, settingsState, xpState)
        NothingWidgetKind.DOT_CLOCK -> DotClockWidgetPreview(config, palette)
        NothingWidgetKind.HEATMAP_MATRIX -> HeatmapMatrixWidgetPreview(config, palette, xpState)
        NothingWidgetKind.NEXT_SCHEDULE -> NextScheduleWidgetPreview(config, palette, schedules)
        NothingWidgetKind.GOAL_BAR -> GoalBarWidgetPreview(config, palette, settingsState, xpState)
        NothingWidgetKind.STREAK_RANK -> StreakRankWidgetPreview(config, palette, xpState)
    }
}

@Composable
private fun GoalRingWidgetPreview(
    config: SingleWidgetConfig,
    palette: WidgetVisualPalette,
    settingsState: FocusSettingsState,
    xpState: FocusXpState
) {
    val goalMs = settingsState.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
    val sampleMs = if (xpState.todayFocusMs > 0L) xpState.todayFocusMs else (goalMs * 0.64f).toLong()
    val fraction = (sampleMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
    val pct = (fraction * 100f).toInt().coerceIn(0, 999)
    val hours = sampleMs / 3_600_000L
    val mins = (sampleMs % 3_600_000L) / 60_000L
    val timeLabel = String.format(Locale.US, "%02dH %02dM", hours, mins)
    val shape = RoundedCornerShape(config.cornerStyle.radiusDp.dp)

    Box(
        modifier = Modifier
            .size(188.dp)
            .clip(shape)
            .background(palette.cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 13.dp, start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (config.showHeaderDots) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(palette.accent)
                    )
                }
                Text(
                    text = "FOCUS LOCK",
                    fontFamily = DotGothic16,
                    fontSize = 9.5.sp,
                    color = palette.textPrimary,
                    letterSpacing = 0.8.sp
                )
            }
            if (settingsState.widgetShowStreakBadge) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.chipBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${xpState.currentStreak}D",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            if (settingsState.widgetShowDottedRing) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.minDimension * 0.44f
                    val dots = 40
                    val activeCount = if (fraction > 0f) (dots * fraction).roundToInt().coerceIn(1, dots) else 0
                    val headIndex = activeCount - 1
                    for (i in 0 until dots) {
                        val angle = Math.toRadians(-90.0 + (360.0 / dots) * i)
                        val dx = cx + (radius * cos(angle)).toFloat()
                        val dy = cy + (radius * sin(angle)).toFloat()
                        val isHead = activeCount > 0 && i == headIndex
                        val isDone = i < activeCount && !isHead
                        drawCircle(
                            color = when {
                                isHead -> palette.accent
                                isDone -> palette.activeDot
                                else -> palette.inactiveDot
                            },
                            radius = when {
                                isHead -> 2.3.dp.toPx()
                                isDone -> 1.7.dp.toPx()
                                else -> 1.5.dp.toPx()
                            },
                            center = Offset(dx, dy)
                        )
                    }
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
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                }
                if (settingsState.widgetShowGoalPercent) {
                    Text(
                        text = "$pct%",
                        fontFamily = DotGothic16,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                }
            }
        }

        if (settingsState.widgetShowRankXpTag) {
            Text(
                text = "${xpState.rank.title.uppercase(Locale.US)} · ${xpState.totalXp} XP",
                fontFamily = JetBrainsMono,
                fontSize = 9.5.sp,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 13.dp, start = 12.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun DotClockWidgetPreview(
    config: SingleWidgetConfig,
    palette: WidgetVisualPalette
) {
    val now = remember { Date() }
    val timeText = remember(config.use24HourClock) {
        SimpleDateFormat(if (config.use24HourClock) "HH:mm" else "hh:mm", Locale.US).format(now)
    }
    val amPm = remember { SimpleDateFormat("a", Locale.US).format(now).uppercase(Locale.US) }
    val dateText = remember { SimpleDateFormat("EEE · MMM d", Locale.US).format(now).uppercase(Locale.US) }
    val shape = RoundedCornerShape(config.cornerStyle.radiusDp.dp)

    Column(
        modifier = Modifier
            .size(188.dp)
            .clip(shape)
            .background(palette.cardBg)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (config.showHeaderDots) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(palette.accent))
                }
                Text(
                    text = "FOCUS TIME",
                    fontFamily = DotGothic16,
                    fontSize = 9.5.sp,
                    color = palette.textPrimary
                )
            }
            if (!config.use24HourClock) {
                Text(
                    text = amPm,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                fontFamily = DotGothic16,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                letterSpacing = 2.sp
            )
            if (config.showSecondaryMetric) {
                Text(
                    text = dateText,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.5.sp,
                    color = palette.textSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(palette.chipBg)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (config.enableQuickLockTap) "● TAP TO LOCK" else "OPEN FOCUS",
                fontFamily = JetBrainsMono,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }
    }
}

@Composable
private fun HeatmapMatrixWidgetPreview(
    config: SingleWidgetConfig,
    palette: WidgetVisualPalette,
    xpState: FocusXpState
) {
    val shape = RoundedCornerShape(config.cornerStyle.radiusDp.dp)
    val pattern = remember {
        listOf(
            0, 1, 2, 1, 0, 2, 3, 2, 1, 2, 3, 3,
            1, 2, 1, 0, 2, 3, 2, 1, 2, 3, 2, 3,
            0, 1, 3, 2, 1, 1, 2, 3, 2, 1, 2, 2,
            2, 2, 1, 1, 0, 2, 3, 2, 3, 3, 1, 3,
            1, 0, 2, 3, 2, 1, 1, 2, 2, 3, 2, 1,
            0, 1, 1, 2, 3, 2, 1, 0, 2, 2, 3, 2,
            1, 2, 0, 1, 2, 3, 2, 2, 1, 3, 2, 3
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .clip(shape)
            .background(palette.cardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (config.showHeaderDots) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(palette.accent))
                }
                Text(
                    text = "CONSISTENCY MATRIX",
                    fontFamily = DotGothic16,
                    fontSize = 10.sp,
                    color = palette.textPrimary
                )
            }
            if (config.showSecondaryMetric) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.chipBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${maxOf(xpState.currentStreak, 5)}D STREAK",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            val cols = 12
            val rows = 7
            val cellW = size.width / cols
            val cellH = size.height / rows
            val r = minOf(cellW, cellH) * 0.34f
            for (col in 0 until cols) {
                for (row in 0 until rows) {
                    val lvl = pattern[(row * cols + col) % pattern.size]
                    val isLast = col == cols - 1 && row == rows - 1
                    val color = when {
                        isLast || lvl == 3 -> palette.accent
                        lvl == 2 -> palette.activeDot.copy(alpha = 0.85f)
                        lvl == 1 -> palette.activeDot.copy(alpha = 0.4f)
                        else -> palette.inactiveDot
                    }
                    drawCircle(
                        color = color,
                        radius = if (isLast) r * 1.15f else r,
                        center = Offset(col * cellW + cellW / 2f, row * cellH + cellH / 2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NextScheduleWidgetPreview(
    config: SingleWidgetConfig,
    palette: WidgetVisualPalette,
    schedules: List<FocusSchedule>
) {
    val shape = RoundedCornerShape(config.cornerStyle.radiusDp.dp)
    val next = remember(schedules) { FocusScheduleRepository.findNextUpcomingWindow(schedules) }
    val name = next?.schedule?.name ?: "Morning Deep Work"
    val range = next?.schedule?.formattedTimeRange(config.use24HourClock)
        ?: if (config.use24HourClock) "09:00 → 10:30" else "9:00 AM → 10:30 AM"
    val repeat = next?.schedule?.repeatSummary()?.uppercase(Locale.US) ?: "DAILY"
    val countdown = next?.let {
        if (it.isCurrentlyActive) "ACTIVE"
        else "IN ${FocusScheduleRepository.formatRelativeCountdown(it.startTimeMs).uppercase(Locale.US)}"
    } ?: "IN 25M"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(shape)
            .background(palette.cardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (config.showHeaderDots) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(palette.accent))
                }
                Text(
                    text = "NEXT SCHEDULE",
                    fontFamily = DotGothic16,
                    fontSize = 10.sp,
                    color = palette.textPrimary
                )
            }
            if (config.showSecondaryMetric) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.chipBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = repeat,
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = range,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = palette.textSecondary,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = countdown,
                fontFamily = DotGothic16,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
        }

        PreviewDottedCapsuleBar(progress = 0.28f, palette = palette)
    }
}

@Composable
private fun GoalBarWidgetPreview(
    config: SingleWidgetConfig,
    palette: WidgetVisualPalette,
    settingsState: FocusSettingsState,
    xpState: FocusXpState
) {
    val shape = RoundedCornerShape(config.cornerStyle.radiusDp.dp)
    val goalMs = settingsState.dailyGoalMinutes.coerceAtLeast(1L) * 60_000L
    val sampleMs = if (xpState.todayFocusMs > 0L) xpState.todayFocusMs else (goalMs * 0.72f).toLong()
    val progress = (sampleMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
    val pct = (progress * 100f).toInt().coerceIn(0, 999)
    val hrs = sampleMs / 3_600_000L
    val mins = (sampleMs % 3_600_000L) / 60_000L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(shape)
            .background(palette.cardBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "%s%02dH %02dM", if (config.showHeaderDots) "● " else "", hrs, mins),
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                if (config.showSecondaryMetric) {
                    Text(
                        text = "$pct%",
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                }
            }
            PreviewDottedCapsuleBar(progress = progress, palette = palette)
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (config.enableQuickLockTap) "GO" else "IN",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun StreakRankWidgetPreview(
    config: SingleWidgetConfig,
    palette: WidgetVisualPalette,
    xpState: FocusXpState
) {
    val shape = RoundedCornerShape(config.cornerStyle.radiusDp.dp)
    val streak = maxOf(xpState.currentStreak, 7)

    Column(
        modifier = Modifier
            .size(188.dp)
            .clip(shape)
            .background(palette.cardBg)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (config.showHeaderDots) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(palette.accent))
                }
                Text(
                    text = "STREAK",
                    fontFamily = DotGothic16,
                    fontSize = 9.5.sp,
                    color = palette.textPrimary
                )
            }
            if (config.showSecondaryMetric) {
                Text(
                    text = xpState.rank.badgeCode,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary
                )
            }
        }

        Box(
            modifier = Modifier.size(104.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.minDimension * 0.42f
                val dots = 28
                val lit = streak.coerceAtMost(dots)
                for (i in 0 until dots) {
                    val angle = Math.toRadians(-90.0 + (360.0 / dots) * i)
                    val dx = cx + (radius * cos(angle)).toFloat()
                    val dy = cy + (radius * sin(angle)).toFloat()
                    val isHead = lit > 0 && i == lit - 1
                    val isLit = i < lit
                    drawCircle(
                        color = when {
                            isHead -> palette.accent
                            isLit -> palette.activeDot
                            else -> palette.inactiveDot
                        },
                        radius = when {
                            isHead -> 2.8.dp.toPx()
                            isLit -> 2.0.dp.toPx()
                            else -> 1.7.dp.toPx()
                        },
                        center = Offset(dx, dy)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = streak.toString(),
                    fontFamily = DotGothic16,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = if (streak == 1) "DAY" else "DAYS",
                    fontFamily = DotGothic16,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(palette.chipBg)
                .padding(vertical = 5.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${xpState.rank.title.uppercase(Locale.US)} · ${xpState.totalXp} XP",
                fontFamily = JetBrainsMono,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PreviewDottedCapsuleBar(
    progress: Float,
    palette: WidgetVisualPalette
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val count = 22
        val filled = if (progress > 0f) (count * progress.coerceIn(0f, 1f)).roundToInt().coerceIn(1, count) else 0
        val gap = 3.dp.toPx()
        val segW = (size.width - gap * (count - 1)) / count.toFloat()
        val r = minOf(segW * 0.48f, size.height * 0.42f)
        val cy = size.height / 2f
        for (i in 0 until count) {
            val cx = i * (segW + gap) + segW / 2f
            val color = when {
                filled > 0 && i == filled - 1 -> palette.accent
                i < filled -> palette.activeDot
                else -> palette.inactiveDot
            }
            val rad = if (filled > 0 && i == filled - 1) r * 1.18f else r
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - rad, cy - rad),
                size = Size(rad * 2f, rad * 2f),
                cornerRadius = CornerRadius(rad, rad)
            )
        }
    }
}
