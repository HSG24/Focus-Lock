package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FocusHeatmapData
import com.example.data.FocusStatsRepository
import com.example.data.HeatmapDayCell
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme

/**
 * Minimal, borderless Focus Consistency Heatmap & Streak Calendar card.
 * Uses rounded squircle cells, theme-matched tonal ramps, and zero-data pass-through
 * (only active focus days respond to taps).
 */
@Composable
fun FocusHeatmapCard(
    heatmap: FocusHeatmapData,
    onJumpToDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val cardShape = remember { RoundedCornerShape(30.dp) }
    val pillShape = remember { RoundedCornerShape(50) }

    var selectedDayMillis by remember(heatmap) { mutableStateOf<Long?>(null) }
    val selectedCell: HeatmapDayCell? = remember(heatmap, selectedDayMillis) {
        val target = selectedDayMillis ?: return@remember null
        heatmap.weeks.asSequence()
            .flatMap { it.days.asSequence() }
            .firstOrNull { it.calendarMillis == target && it.durationMs > 0L }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .focusGlassPanel(shape = cardShape, palette = palette)
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .testTag("focus_heatmap_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Minimal header row
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "Consistency Heatmap",
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Consistency",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val activeLabel = if (heatmap.activeDaysCount == 1) "1 active day" else "${heatmap.activeDaysCount} active days"
                        Text(
                            text = "$activeLabel · ${heatmap.windowWeeks}w",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.5.sp,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Subtle streak / goal-days summary pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(pillShape)
                        .background(
                            if (heatmap.currentStreak > 0) palette.primaryContainer
                            else palette.surfaceElevated,
                            pillShape
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("heatmap_streak_pill")
                ) {
                    Icon(
                        imageVector = if (heatmap.currentStreak > 0) {
                            Icons.Outlined.LocalFireDepartment
                        } else {
                            Icons.Outlined.CheckCircle
                        },
                        contentDescription = null,
                        tint = if (heatmap.currentStreak > 0) palette.primary else palette.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (heatmap.currentStreak > 0) {
                            "${heatmap.currentStreak}d streak"
                        } else {
                            "${heatmap.goalMetDaysCount} goals"
                        },
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (heatmap.currentStreak > 0) palette.primary else palette.textSecondary,
                        maxLines = 1
                    )
                }
            }

            // Responsive 14-week rounded squircle matrix
            val weekCount = heatmap.weeks.size.coerceAtLeast(1)
            val dayMarkers = remember { listOf("M", "", "W", "", "F", "", "S") }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val axisWidth = 16.dp
                val axisGap = 6.dp
                val cellGap = 4.dp
                val gridWidth = (maxWidth - axisWidth - axisGap).coerceAtLeast(120.dp)
                val rawCellSize = (gridWidth - cellGap * (weekCount - 1)) / weekCount
                val cellSize = rawCellSize.coerceIn(10.dp, 22.dp)
                val cellCorner = (cellSize * 0.34f).coerceIn(3.5.dp, 7.dp)
                val cellShape = RoundedCornerShape(cellCorner)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Whisper-quiet month header row aligned above week columns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = axisWidth + axisGap),
                        horizontalArrangement = Arrangement.spacedBy(cellGap)
                    ) {
                        heatmap.weeks.forEachIndexed { colIndex, week ->
                            val prevHasLabel = colIndex > 0 && heatmap.weeks[colIndex - 1].monthLabel != null
                            val displayLabel = if (!prevHasLabel) week.monthLabel else null
                            Box(
                                modifier = Modifier.width(cellSize),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (displayLabel != null) {
                                    Text(
                                        text = displayLabel,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.textSecondary.copy(alpha = 0.78f),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible
                                    )
                                }
                            }
                        }
                    }

                    // Matrix rows (weekday label column + 14 rounded squircle columns)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.width(axisWidth),
                            verticalArrangement = Arrangement.spacedBy(cellGap),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            dayMarkers.forEach { marker ->
                                Box(
                                    modifier = Modifier.height(cellSize),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (marker.isNotEmpty()) {
                                        Text(
                                            text = marker,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.textSecondary.copy(alpha = 0.62f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(axisGap))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(cellGap),
                            verticalAlignment = Alignment.Top
                        ) {
                            heatmap.weeks.forEach { week ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(cellGap),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    week.days.forEach { cell ->
                                        val isSelected = selectedCell?.calendarMillis == cell.calendarMillis
                                        val cellColor = heatmapCellColor(
                                            level = cell.intensityLevel,
                                            isFuture = cell.isFuture,
                                            palette = palette
                                        )
                                        val isInteractive = !cell.isFuture && cell.durationMs > 0L
                                        val cellScale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.22f else 1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.68f,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                            label = "heatmap_cell_scale"
                                        )

                                        val clickModifier = if (isInteractive) {
                                            Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                haptics.tick()
                                                selectedDayMillis = if (isSelected) null else cell.calendarMillis
                                            }
                                        } else {
                                            Modifier
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(cellSize)
                                                .graphicsLayer {
                                                    scaleX = cellScale
                                                    scaleY = cellScale
                                                }
                                                .clip(cellShape)
                                                .background(cellColor, cellShape)
                                                .then(clickModifier)
                                                .testTag(
                                                    if (cell.isToday) "heatmap_cell_today"
                                                    else "heatmap_cell_${cell.calendarMillis}"
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size((cellSize * 0.34f).coerceAtLeast(3.5.dp))
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (cell.intensityLevel >= 3) palette.onPrimary
                                                            else palette.textPrimary
                                                        )
                                                )
                                            } else if (cell.isToday) {
                                                Box(
                                                    modifier = Modifier
                                                        .size((cellSize * 0.28f).coerceAtLeast(3.dp))
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (cell.intensityLevel >= 3) palette.onPrimary.copy(alpha = 0.85f)
                                                            else palette.primary
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
            }

            // Expandable detail capsule when an active day cell is tapped
            AnimatedVisibility(
                visible = selectedCell != null,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut()
            ) {
                val cell = selectedCell
                if (cell != null) {
                    val detailShape = remember { RoundedCornerShape(22.dp) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(detailShape)
                            .background(palette.surfaceElevated, detailShape)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("heatmap_selected_day_pill"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = cell.dateLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (cell.isGoalMet) {
                                    Text(
                                        text = "GOAL",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.success,
                                        modifier = Modifier
                                            .clip(pillShape)
                                            .background(palette.success.copy(alpha = 0.14f), pillShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            val sessWord = if (cell.sessionCount == 1) "session" else "sessions"
                            Text(
                                text = "${FocusStatsRepository.formatDurationHero(cell.durationMs)} · ${cell.sessionCount} $sessWord",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.5.sp,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(pillShape)
                                .background(palette.primary, pillShape)
                                .clickable {
                                    haptics.tap()
                                    onJumpToDay(cell.calendarMillis)
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                                .testTag("heatmap_view_day_button")
                        ) {
                            Text(
                                text = "View",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.onPrimary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View day details",
                                tint = palette.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Quiet footer legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (heatmap.maxStreak > 0) {
                        "Best ${heatmap.maxStreak}d · ${heatmap.goalMetDaysCount} goals met"
                    } else {
                        "Tap any active day to inspect"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = palette.textSecondary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Less",
                        fontSize = 10.sp,
                        color = palette.textSecondary.copy(alpha = 0.7f)
                    )
                    val legendShape = remember { RoundedCornerShape(3.dp) }
                    for (level in 0..4) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(legendShape)
                                .background(
                                    heatmapCellColor(
                                        level = level,
                                        isFuture = false,
                                        palette = palette
                                    ),
                                    legendShape
                                )
                        )
                    }
                    Text(
                        text = "More",
                        fontSize = 10.sp,
                        color = palette.textSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun heatmapCellColor(
    level: Int,
    isFuture: Boolean,
    palette: FocusThemePalette
): Color {
    if (isFuture) {
        return palette.surfaceElevated.copy(alpha = 0.24f)
    }
    if (palette.isLiquidGlass) {
        return when (level) {
            0 -> Color(0xFF151A24)
            1 -> Color(0xFF2B3547)
            2 -> Color(0xFF4F6280)
            3 -> Color(0xFF93AACB)
            else -> Color(0xFFE9F0FB)
        }
    }
    return when (level) {
        0 -> palette.surfaceElevated.copy(alpha = 0.76f)
        1 -> palette.primary.copy(alpha = 0.24f)
        2 -> palette.primary.copy(alpha = 0.48f)
        3 -> palette.primary.copy(alpha = 0.75f)
        else -> palette.primary
    }
}
