package com.example.ui.components

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalFocusTheme
import com.example.ui.theme.PureWhite

fun View.performClockTickHaptic() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

fun View.performKeyTapHaptic() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

private val PillTopHighlightColor = Color(0x33FFFFFF)

/**
 * Spring scale feedback helper tuned for crisp 120Hz button and card interactions.
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
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "spring_press_scale"
    )
    return scale
}

/**
 * Custom geometric Focus Lock emblem matching the adaptive launcher icon and
 * dynamically colored by the active theme in LocalFocusTheme.
 */
@Composable
fun FocusLockEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    showAmbientGlow: Boolean = true
) {
    val palette = LocalFocusTheme.current
    val primaryColor = palette.primary
    val deepColor = palette.primaryDeep
    val glowColor = palette.primaryGlow
    val surfaceColor = palette.surface
    val strokePrimary = palette.textPrimary

    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val w = this.size.width
                val h = this.size.height
                val center = Offset(w / 2f, h / 2f)
                val unit = minOf(w, h) / 108f

                val glowRadius = 54f * unit
                val glowBrush = if (showAmbientGlow) {
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.24f),
                            deepColor.copy(alpha = 0.14f),
                            glowColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius
                    )
                } else {
                    null
                }

                val ringRadius = 31f * unit
                val ringTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                val ringSize = Size(ringRadius * 2f, ringRadius * 2f)
                val ringStroke = Stroke(width = 2.4f * unit, cap = StrokeCap.Round)
                val arcAngles = floatArrayOf(-78f, 12f, 102f, 192f)

                val tickStroke = 2.4f * unit
                val outerTick = 36f * unit
                val innerTick = 26.5f * unit

                val shackleRadius = 9f * unit
                val shackleCenterY = center.y - 12.5f * unit
                val shackleTopLeft = Offset(center.x - shackleRadius, shackleCenterY - shackleRadius)
                val shackleSize = Size(shackleRadius * 2f, shackleRadius * 2f)
                val shackleStrokeWidth = 3.1f * unit
                val shackleStroke = Stroke(width = shackleStrokeWidth, cap = StrokeCap.Round)
                val shackleBottomY = center.y - 5.5f * unit

                val bodyWidth = 33f * unit
                val bodyHeight = 25f * unit
                val bodyTopLeft = Offset(center.x - bodyWidth / 2f, shackleBottomY)
                val bodySize = Size(bodyWidth, bodyHeight)
                val bodyCorner = CornerRadius(4.2f * unit, 4.2f * unit)
                val bodyStroke = Stroke(width = 2.7f * unit, join = StrokeJoin.Round)

                val coreCenter = Offset(center.x, center.y + 4.8f * unit)
                val coreOuterRadius = 6.8f * unit
                val coreInnerRadius = 4.2f * unit
                val coreHaloColor = primaryColor.copy(alpha = 0.30f)
                val coreKeyholeStart = Offset(center.x, coreCenter.y + 2f * unit)
                val coreKeyholeEnd = Offset(center.x, coreCenter.y + 8.6f * unit)
                val coreKeyholeWidth = 2.8f * unit

                onDrawBehind {
                    if (glowBrush != null) {
                        drawCircle(
                            brush = glowBrush,
                            radius = glowRadius,
                            center = center
                        )
                    }

                    for (start in arcAngles) {
                        drawArc(
                            color = strokePrimary,
                            startAngle = start,
                            sweepAngle = 66f,
                            useCenter = false,
                            topLeft = ringTopLeft,
                            size = ringSize,
                            style = ringStroke
                        )
                    }

                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x, center.y - outerTick),
                        end = Offset(center.x, center.y - innerTick),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x, center.y + innerTick),
                        end = Offset(center.x, center.y + outerTick),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x - outerTick, center.y),
                        end = Offset(center.x - innerTick, center.y),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x + innerTick, center.y),
                        end = Offset(center.x + outerTick, center.y),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )

                    drawArc(
                        color = strokePrimary,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = shackleTopLeft,
                        size = shackleSize,
                        style = shackleStroke
                    )
                    drawLine(
                        color = strokePrimary,
                        start = Offset(center.x - shackleRadius, shackleCenterY),
                        end = Offset(center.x - shackleRadius, shackleBottomY),
                        strokeWidth = shackleStrokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = strokePrimary,
                        start = Offset(center.x + shackleRadius, shackleCenterY),
                        end = Offset(center.x + shackleRadius, shackleBottomY),
                        strokeWidth = shackleStrokeWidth,
                        cap = StrokeCap.Round
                    )

                    drawRoundRect(
                        color = surfaceColor,
                        topLeft = bodyTopLeft,
                        size = bodySize,
                        cornerRadius = bodyCorner
                    )
                    drawRoundRect(
                        color = strokePrimary,
                        topLeft = bodyTopLeft,
                        size = bodySize,
                        cornerRadius = bodyCorner,
                        style = bodyStroke
                    )

                    drawCircle(
                        color = coreHaloColor,
                        radius = coreOuterRadius,
                        center = coreCenter
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = coreInnerRadius,
                        center = coreCenter
                    )
                    drawLine(
                        color = primaryColor,
                        start = coreKeyholeStart,
                        end = coreKeyholeEnd,
                        strokeWidth = coreKeyholeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
    )
}

@Immutable
data class PillNavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Floating simulated-glass pill bottom bar styled dynamically by the active theme.
 */
@Composable
fun FloatingPillBottomBar(
    tabs: List<PillNavTab>,
    selectedIndex: Int,
    pagerPositionProvider: () -> Float,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFocusTheme.current
    val pillShape = remember { RoundedCornerShape(32.dp) }
    val indicatorShape = remember { RoundedCornerShape(26.dp) }
    val indicatorBrush = remember(palette.primary, palette.primaryDeep) {
        Brush.linearGradient(
            colors = listOf(
                palette.primary.copy(alpha = 0.94f),
                palette.primaryDeep.copy(alpha = 0.94f)
            )
        )
    }
    val tabCount = tabs.size.coerceAtLeast(1)
    val maxIdx = (tabCount - 1).coerceAtLeast(0).toFloat()

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
                .background(palette.pillGlassFill)
                .drawWithCache {
                    val strokePx = 1.dp.toPx()
                    val cornerPx = (size.height / 2f)
                    onDrawWithContent {
                        drawContent()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                0f to PillTopHighlightColor,
                                0.14f to Color.Transparent,
                                startY = strokePx,
                                endY = size.height * 0.45f
                            ),
                            topLeft = Offset(strokePx, strokePx),
                            size = Size(size.width - strokePx * 2f, size.height - strokePx * 2f),
                            cornerRadius = CornerRadius(cornerPx, cornerPx),
                            style = Stroke(width = strokePx)
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    color = palette.primaryBorderTint,
                    shape = pillShape
                )
                .padding(5.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val clamped = pagerPositionProvider().coerceIn(0f, maxIdx)
                            translationX = size.width * clamped
                        }
                        .fillMaxWidth(1f / tabCount)
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

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        PillTabItem(
                            tab = tab,
                            isSelected = selectedIndex == index,
                            indicatorShape = indicatorShape,
                            selectedColor = palette.onPrimary,
                            unselectedColor = palette.textSecondary,
                            onClick = { onSelectTab(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillTabItem(
    tab: PillNavTab,
    isSelected: Boolean,
    indicatorShape: RoundedCornerShape,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.94f)
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pill_tab_color"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(indicatorShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
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
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
