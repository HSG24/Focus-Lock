package com.example.ui.components

import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalFocusTheme
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun View.performClockTickHaptic() {
    performFocusTick()
}

fun View.performKeyTapHaptic() {
    performFocusTap()
}

private val PillTopSheenColor = Color(0x26FFFFFF)

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
            dampingRatio = 0.82f,
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
                            primaryColor.copy(alpha = 0.20f),
                            deepColor.copy(alpha = 0.10f),
                            glowColor.copy(alpha = 0.05f),
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
                val ringStroke = Stroke(width = 2.2f * unit, cap = StrokeCap.Round)
                val arcAngles = floatArrayOf(-78f, 12f, 102f, 192f)

                val tickStroke = 2.2f * unit
                val outerTick = 36f * unit
                val innerTick = 26.5f * unit

                val shackleRadius = 9f * unit
                val shackleCenterY = center.y - 12.5f * unit
                val shackleTopLeft = Offset(center.x - shackleRadius, shackleCenterY - shackleRadius)
                val shackleSize = Size(shackleRadius * 2f, shackleRadius * 2f)
                val shackleStrokeWidth = 3.0f * unit
                val shackleStroke = Stroke(width = shackleStrokeWidth, cap = StrokeCap.Round)
                val shackleBottomY = center.y - 5.5f * unit

                val bodyWidth = 33f * unit
                val bodyHeight = 25f * unit
                val bodyTopLeft = Offset(center.x - bodyWidth / 2f, shackleBottomY)
                val bodySize = Size(bodyWidth, bodyHeight)
                val bodyCorner = CornerRadius(4.2f * unit, 4.2f * unit)
                val bodyStroke = Stroke(width = 2.5f * unit, join = StrokeJoin.Round)

                val coreCenter = Offset(center.x, center.y + 4.8f * unit)
                val coreOuterRadius = 6.5f * unit
                val coreInnerRadius = 4.0f * unit
                val coreHaloColor = primaryColor.copy(alpha = 0.25f)
                val coreKeyholeStart = Offset(center.x, coreCenter.y + 2f * unit)
                val coreKeyholeEnd = Offset(center.x, coreCenter.y + 8.6f * unit)
                val coreKeyholeWidth = 2.6f * unit

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
 * Minimal floating navigation pill with iOS-style elastic edge-stretching indicator
 * and directional icon/label glide on both tab taps and horizontal swipes/scrubbing.
 */
@Composable
fun FloatingPillBottomBar(
    tabs: List<PillNavTab>,
    selectedIndex: Int,
    pagerPositionProvider: () -> Float,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isPagerDragged: Boolean = false
) {
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val pillShape = remember { RoundedCornerShape(50) }
    val indicatorShape = remember { RoundedCornerShape(50) }
    val tabCount = tabs.size.coerceAtLeast(1)
    val maxIdx = (tabCount - 1).coerceAtLeast(0).toFloat()
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelectTab)
    var lastScrubbedIndex by remember { mutableIntStateOf(selectedIndex) }

    // Asymmetric leading/trailing edge springs for crisp iOS-style rubber-band pill morph
    val leftEdge = remember { Animatable(selectedIndex.toFloat()) }
    val rightEdge = remember { Animatable((selectedIndex + 1).toFloat()) }

    fun computeLiveDragSpan(pagerPos: Float): Pair<Float, Float> {
        val clamped = pagerPos.coerceIn(0f, maxIdx)
        val frac = clamped - kotlin.math.floor(clamped)
        val swipeStretch = kotlin.math.sin(frac * Math.PI).toFloat() * 0.30f
        val halfStretch = swipeStretch * 0.5f
        val l = (clamped - halfStretch).coerceIn(0f, maxIdx)
        val r = (clamped + 1f + halfStretch).coerceIn(l + 0.85f, tabCount.toFloat())
        return l to r
    }

    LaunchedEffect(selectedIndex, isPagerDragged) {
        if (isPagerDragged) {
            // While the user's finger is actively dragging the pager, keep leftEdge/rightEdge
            // snapped to the live drag position so releasing smoothly morphs forward with zero jump-back.
            snapshotFlow { pagerPositionProvider().coerceIn(0f, maxIdx) }.collect { pos ->
                val (l, r) = computeLiveDragSpan(pos)
                leftEdge.snapTo(l)
                rightEdge.snapTo(r)
            }
        } else {
            val targetLeft = selectedIndex.toFloat()
            val targetRight = (selectedIndex + 1).toFloat()
            val leadSpring = spring<Float>(dampingRatio = 0.80f, stiffness = 540f)
            val trailSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 250f)

            val movingRight = targetRight > rightEdge.value + 0.01f ||
                (targetLeft > leftEdge.value + 0.01f && targetRight >= rightEdge.value - 0.01f)
            val movingLeft = targetLeft < leftEdge.value - 0.01f ||
                (targetRight < rightEdge.value - 0.01f && targetLeft <= leftEdge.value + 0.01f)

            when {
                movingRight && !movingLeft -> {
                    // Moving right: right edge surges ahead first, left edge stretches smoothly behind
                    coroutineScope {
                        launch { rightEdge.animateTo(targetRight, leadSpring) }
                        launch { leftEdge.animateTo(targetLeft, trailSpring) }
                    }
                }
                movingLeft && !movingRight -> {
                    // Moving left: left edge surges ahead first, right edge stretches smoothly behind
                    coroutineScope {
                        launch { leftEdge.animateTo(targetLeft, leadSpring) }
                        launch { rightEdge.animateTo(targetRight, trailSpring) }
                    }
                }
                else -> {
                    // Settling cleanly into the target slot (e.g., after a cancelled drag)
                    coroutineScope {
                        launch { leftEdge.animateTo(targetLeft, leadSpring) }
                        launch { rightEdge.animateTo(targetRight, leadSpring) }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val pillBgModifier = if (palette.isLiquidGlass) {
            Modifier.focusGlassPanel(
                shape = pillShape,
                palette = palette,
                elevated = true,
                shimmer = false
            )
        } else {
            Modifier
                .clip(pillShape)
                .background(palette.pillGlassFill, pillShape)
        }

        Box(
            modifier = Modifier
                .widthIn(max = 396.dp)
                .fillMaxWidth()
                .height(52.dp)
                .then(pillBgModifier)
                .pointerInput(tabCount) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            lastScrubbedIndex = currentSelected
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val idx = ((offset.x / width) * tabCount).toInt().coerceIn(0, tabCount - 1)
                            if (idx != lastScrubbedIndex) {
                                lastScrubbedIndex = idx
                                haptics.tick()
                                currentOnSelect(idx)
                            }
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val idx = ((change.position.x / width) * tabCount).toInt().coerceIn(0, tabCount - 1)
                            if (idx != lastScrubbedIndex) {
                                lastScrubbedIndex = idx
                                haptics.tick()
                                currentOnSelect(idx)
                            }
                        }
                    )
                }
                .padding(4.dp)
        ) {
            fun computePillSpan(): Pair<Float, Float> {
                return if (isPagerDragged) {
                    computeLiveDragSpan(pagerPositionProvider())
                } else {
                    val l = leftEdge.value.coerceIn(0f, maxIdx)
                    val r = rightEdge.value.coerceIn(l + 0.85f, tabCount.toFloat())
                    l to r
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // Elastic morphing pill indicator drawn on GPU canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val liquidBrushColors = listOf(Color(0xFF283040), Color(0xFF191E2A))
                            onDrawBehind {
                                val (spanL, spanR) = computePillSpan()
                                val slotW = size.width / tabCount.toFloat()
                                val stretchExcess = ((spanR - spanL) - 1f).coerceAtLeast(0f)
                                val vInset = (size.height * stretchExcess * 0.09f).coerceIn(0f, size.height * 0.12f)
                                val pillX = spanL * slotW
                                val pillW = ((spanR - spanL) * slotW).coerceAtLeast(1f)
                                val pillH = (size.height - vInset * 2f).coerceAtLeast(1f)
                                val corner = CornerRadius(pillH / 2f, pillH / 2f)

                                if (palette.isLiquidGlass) {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = liquidBrushColors,
                                            startY = vInset,
                                            endY = vInset + pillH
                                        ),
                                        topLeft = Offset(pillX, vInset),
                                        size = Size(pillW, pillH),
                                        cornerRadius = corner
                                    )
                                } else {
                                    drawRoundRect(
                                        color = palette.primary,
                                        topLeft = Offset(pillX, vInset),
                                        size = Size(pillW, pillH),
                                        cornerRadius = corner
                                    )
                                }
                            }
                        }
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        PillTabItem(
                            tab = tab,
                            index = index,
                            isSelected = selectedIndex == index,
                            spanProvider = { computePillSpan() },
                            indicatorShape = indicatorShape,
                            selectedColor = if (palette.isLiquidGlass) palette.textPrimary else palette.onPrimary,
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
    index: Int,
    isSelected: Boolean,
    spanProvider: () -> Pair<Float, Float>,
    indicatorShape: RoundedCornerShape,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberSpringPressScale(interactionSource, pressedScale = 0.92f)
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
            .clip(indicatorShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .graphicsLayer {
                val (spanL, spanR) = spanProvider()
                val pillCenter = (spanL + spanR) * 0.5f
                val slotCenter = index + 0.5f
                val stretch = ((spanR - spanL) - 1f).coerceIn(0f, 1.4f)
                val delta = pillCenter - slotCenter
                val proximity = (1f - kotlin.math.abs(delta) * 0.85f).coerceIn(0f, 1f)
                val dir = when {
                    delta > 0.02f -> 1f
                    delta < -0.02f -> -1f
                    else -> 0f
                }
                // Outgoing & incoming button icon+text glide subtly toward the elastic pill edge
                translationX = dir * stretch * proximity * 8.dp.toPx()
                val bounceScale = pressScale * (1f + if (isSelected) stretch * 0.045f else -stretch * proximity * 0.02f)
                scaleX = bounceScale
                scaleY = bounceScale
            }
            .testTag(tab.testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Box(modifier = Modifier.width(4.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
