package com.example.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonBorderTint
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonGlow
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateGray

fun View.performClockTickHaptic() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

fun View.performKeyTapHaptic() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

/**
 * Spring scale feedback helper for responsive 120Hz button and card interactions.
 */
@Composable
fun rememberSpringPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 650f
        ),
        label = "spring_press_scale"
    )
    return scale
}

/**
 * Custom geometric Focus Lock emblem matching the adaptive launcher icon:
 * precision segmented focus target ring, crimson cardinal crosshairs,
 * sleek white padlock silhouette, and sharp crimson focus core.
 */
@Composable
fun FocusLockEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    showAmbientGlow: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val unit = minOf(w, h) / 108f

        if (showAmbientGlow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CrimsonPrimary.copy(alpha = 0.24f),
                        CrimsonDeep.copy(alpha = 0.14f),
                        CrimsonGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = 54f * unit
                ),
                radius = 54f * unit,
                center = center
            )
        }

        val ringRadius = 31f * unit
        val ringTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
        val ringSize = Size(ringRadius * 2f, ringRadius * 2f)
        val ringStroke = Stroke(width = 2.4f * unit, cap = StrokeCap.Round)

        // 4 segmented target arcs leaving crisp openings at N/E/S/W
        val arcAngles = listOf(-78f, 12f, 102f, 192f)
        for (start in arcAngles) {
            drawArc(
                color = PureWhite,
                startAngle = start,
                sweepAngle = 66f,
                useCenter = false,
                topLeft = ringTopLeft,
                size = ringSize,
                style = ringStroke
            )
        }

        // Precision crimson cardinal crosshairs
        val tickStroke = 2.4f * unit
        val outerTick = 36f * unit
        val innerTick = 26.5f * unit
        drawLine(
            color = CrimsonPrimary,
            start = Offset(center.x, center.y - outerTick),
            end = Offset(center.x, center.y - innerTick),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = CrimsonPrimary,
            start = Offset(center.x, center.y + innerTick),
            end = Offset(center.x, center.y + outerTick),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = CrimsonPrimary,
            start = Offset(center.x - outerTick, center.y),
            end = Offset(center.x - innerTick, center.y),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = CrimsonPrimary,
            start = Offset(center.x + innerTick, center.y),
            end = Offset(center.x + outerTick, center.y),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )

        // Padlock shackle arch
        val shackleRadius = 9f * unit
        val shackleCenterY = center.y - 12.5f * unit
        drawArc(
            color = PureWhite,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - shackleRadius, shackleCenterY - shackleRadius),
            size = Size(shackleRadius * 2f, shackleRadius * 2f),
            style = Stroke(width = 3.1f * unit, cap = StrokeCap.Round)
        )
        drawLine(
            color = PureWhite,
            start = Offset(center.x - shackleRadius, shackleCenterY),
            end = Offset(center.x - shackleRadius, center.y - 5.5f * unit),
            strokeWidth = 3.1f * unit,
            cap = StrokeCap.Round
        )
        drawLine(
            color = PureWhite,
            start = Offset(center.x + shackleRadius, shackleCenterY),
            end = Offset(center.x + shackleRadius, center.y - 5.5f * unit),
            strokeWidth = 3.1f * unit,
            cap = StrokeCap.Round
        )

        // Padlock body (Obsidian fill + White outline)
        val bodyWidth = 33f * unit
        val bodyHeight = 25f * unit
        val bodyTopLeft = Offset(center.x - bodyWidth / 2f, center.y - 5.5f * unit)
        val corner = CornerRadius(4.2f * unit, 4.2f * unit)
        drawRoundRect(
            color = ObsidianSurface,
            topLeft = bodyTopLeft,
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = corner
        )
        drawRoundRect(
            color = PureWhite,
            topLeft = bodyTopLeft,
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = corner,
            style = Stroke(width = 2.7f * unit, join = StrokeJoin.Round)
        )

        // Sharp Crimson Focus Core inside padlock body
        val coreCenter = Offset(center.x, center.y + 4.8f * unit)
        drawCircle(
            color = CrimsonPrimary.copy(alpha = 0.30f),
            radius = 6.8f * unit,
            center = coreCenter
        )
        drawCircle(
            color = CrimsonPrimary,
            radius = 4.2f * unit,
            center = coreCenter
        )
        drawLine(
            color = CrimsonPrimary,
            start = Offset(center.x, coreCenter.y + 2f * unit),
            end = Offset(center.x, coreCenter.y + 8.6f * unit),
            strokeWidth = 2.8f * unit,
            cap = StrokeCap.Round
        )
    }
}

data class PillNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Floating translucent pill bottom bar with GPU-translated sliding crimson indicator.
 * Uses a lambda provider and graphicsLayer so horizontal pager swipes incur zero
 * recompositions and zero relayouts per frame for locked 120Hz responsiveness.
 */
@Composable
fun FloatingPillBottomBar(
    tabs: List<PillNavTab>,
    selectedIndex: Int,
    pagerPositionProvider: () -> Float,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = remember { RoundedCornerShape(32.dp) }
    val indicatorShape = remember { RoundedCornerShape(26.dp) }
    val backdropBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                ObsidianGlass,
                Color(0xE80D0D10)
            )
        )
    }
    val borderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                CrimsonBorderTint,
                ObsidianBorder,
                CrimsonBorderTint.copy(alpha = 0.25f)
            )
        )
    }
    val indicatorBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                CrimsonPrimary.copy(alpha = 0.94f),
                CrimsonDeep.copy(alpha = 0.94f)
            )
        )
    }

    val density = LocalDensity.current
    val tabCount = tabs.size.coerceAtLeast(1)
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val tabWidthDp = remember(trackWidthPx, tabCount, density) {
        with(density) { (trackWidthPx.toFloat() / tabCount).toDp() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .height(64.dp)
                .clip(pillShape)
                .background(backdropBrush)
                .border(
                    width = 1.dp,
                    brush = borderBrush,
                    shape = pillShape
                )
                .padding(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { trackWidthPx = it.width }
            ) {
                if (trackWidthPx > 0) {
                    // Sliding crimson pill indicator moved strictly in GPU graphicsLayer phase
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                val maxIdx = (tabCount - 1).coerceAtLeast(0).toFloat()
                                val clamped = pagerPositionProvider().coerceIn(0f, maxIdx)
                                translationX = (trackWidthPx.toFloat() / tabCount) * clamped
                            }
                            .width(tabWidthDp)
                            .fillMaxHeight()
                            .padding(1.dp)
                            .clip(indicatorShape)
                            .background(indicatorBrush)
                            .border(
                                width = 0.75.dp,
                                color = PureWhite.copy(alpha = 0.16f),
                                shape = indicatorShape
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedIndex == index
                        val interactionSource = remember { MutableInteractionSource() }
                        val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.94f)
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) PureWhite else SlateGray,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "pill_tab_color"
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(indicatorShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onSelectTab(index) }
                                )
                                .testTag(tab.testTag),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = contentColor,
                                modifier = Modifier.size(19.dp)
                            )
                            Box(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
