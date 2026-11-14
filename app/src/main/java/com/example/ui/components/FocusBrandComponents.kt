package com.example.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium
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
 * Floating translucent frosted-glass pill bottom bar with sliding crimson indicator
 * synchronized smoothly to the HorizontalPager position.
 */
@Composable
fun FloatingPillBottomBar(
    tabs: List<PillNavTab>,
    selectedIndex: Int,
    pagerPosition: Float,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(32.dp)
    val clampedPosition = pagerPosition.coerceIn(0f, (tabs.size - 1).coerceAtLeast(0).toFloat())

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
                .shadow(
                    elevation = 18.dp,
                    shape = pillShape,
                    ambientColor = CrimsonPrimary.copy(alpha = 0.28f),
                    spotColor = CrimsonDeep.copy(alpha = 0.35f)
                )
                .clip(pillShape)
        ) {
            // Frosted glass backdrop plate (blurred on Android 12+ without blurring foreground icons)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.graphicsLayer {
                                renderEffect = RenderEffect
                                    .createBlurEffect(28f, 28f, Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }
                        } else {
                            Modifier
                        }
                    )
                    .background(ObsidianGlass)
            )

            // Crisp translucent surface + subtle red-tinted hairline border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianGlass,
                                Color(0xDB0D0D10)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CrimsonBorderTint,
                                ObsidianBorder,
                                CrimsonBorderTint.copy(alpha = 0.25f)
                            )
                        ),
                        shape = pillShape
                    )
                    .padding(5.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val tabCount = tabs.size.coerceAtLeast(1)
                    val tabWidth = maxWidth / tabCount

                    // Sliding crimson pill indicator synced directly with pager scroll/spring
                    Box(
                        modifier = Modifier
                            .offset(x = tabWidth * clampedPosition)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .padding(1.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        CrimsonPrimary.copy(alpha = 0.92f),
                                        CrimsonDeep.copy(alpha = 0.92f)
                                    )
                                )
                            )
                            .border(
                                width = 0.75.dp,
                                color = PureWhite.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(26.dp)
                            )
                    )

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
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
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
                                    .clip(RoundedCornerShape(26.dp))
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
}
