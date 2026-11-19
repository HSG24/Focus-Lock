package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

@Immutable
data class ElasticPillSegment(
    val label: String,
    val testTag: String? = null,
    val weight: Float = 1f
)

/**
 * iOS-inspired fluid elastic segmented pill used across ALL themes.
 *
 * When switching between options, the leading edge of the active pill shoots forward
 * with a faster spring while the trailing edge stretches smoothly behind it (creating
 * a tactile rubber-band capsule morph with subtle volume-preserving vertical squish).
 * Simultaneously, both the outgoing and incoming labels glide subtly toward the
 * direction of travel so the entire button cluster feels alive on interaction —
 * with zero continuous/idle animations.
 */
@Composable
fun ElasticMorphingSegmentedPill(
    segments: List<ElasticPillSegment>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    fontSize: TextUnit = 12.sp,
    fontFamily: FontFamily = JetBrainsMono,
    useElevatedContainer: Boolean = false,
    accentFill: Boolean = false
) {
    if (segments.isEmpty()) return
    val palette = LocalFocusTheme.current
    val haptics = rememberFocusHaptics()
    val containerShape = remember { RoundedCornerShape(50) }

    val totalWeight = remember(segments) {
        segments.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
    }
    val segmentBounds = remember(segments, totalWeight) {
        var cursor = 0f
        segments.map { seg ->
            val start = cursor / totalWeight
            cursor += seg.weight
            val end = cursor / totalWeight
            start to end
        }
    }

    val safeIndex = selectedIndex.coerceIn(-1, segments.lastIndex)
    val initialBounds = if (safeIndex >= 0) segmentBounds[safeIndex] else (0f to 0f)
    val leftEdge = remember { Animatable(initialBounds.first) }
    val rightEdge = remember { Animatable(initialBounds.second) }
    var previousIndex by remember { mutableIntStateOf(safeIndex) }

    LaunchedEffect(safeIndex, segmentBounds) {
        if (safeIndex < 0) {
            previousIndex = -1
            return@LaunchedEffect
        }
        val target = segmentBounds[safeIndex]
        if (previousIndex < 0) {
            leftEdge.snapTo(target.first)
            rightEdge.snapTo(target.second)
            previousIndex = safeIndex
            return@LaunchedEffect
        }
        val movingRight = safeIndex > previousIndex
        val movingLeft = safeIndex < previousIndex
        previousIndex = safeIndex

        val leadSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 620f)
        val trailSpring = spring<Float>(dampingRatio = 0.80f, stiffness = 270f)

        if (movingRight) {
            // Right side surges ahead toward the next button; left tail stretches smoothly behind
            launch { rightEdge.animateTo(target.second, leadSpring) }
            launch { leftEdge.animateTo(target.first, trailSpring) }
        } else if (movingLeft) {
            // Left side surges ahead toward the previous button; right tail stretches smoothly behind
            launch { leftEdge.animateTo(target.first, leadSpring) }
            launch { rightEdge.animateTo(target.second, trailSpring) }
        } else {
            launch { leftEdge.animateTo(target.first, leadSpring) }
            launch { rightEdge.animateTo(target.second, leadSpring) }
        }
    }

    val indicatorFillColor = when {
        palette.isLiquidGlass -> Color(0xFF283142)
        accentFill -> palette.primary
        else -> palette.primary
    }
    val indicatorFillBottom = when {
        palette.isLiquidGlass -> Color(0xFF191F2C)
        else -> palette.primary
    }
    val selectedTextColor = if (palette.isLiquidGlass) palette.textPrimary else palette.onPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .focusGlassPanel(
                shape = containerShape,
                palette = palette,
                elevated = useElevatedContainer
            )
            .padding(4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Elastic morphing pill indicator drawn on GPU canvas with zero layout thrash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (safeIndex < 0) return@drawBehind
                        val l = leftEdge.value.coerceIn(0f, 1f)
                        val r = rightEdge.value.coerceIn(l, 1f)
                        val targetBounds = segmentBounds[safeIndex]
                        val targetWidth = (targetBounds.second - targetBounds.first).coerceAtLeast(0.01f)
                        val currentWidth = (r - l).coerceAtLeast(0.01f)
                        val stretchExcess = ((currentWidth / targetWidth) - 1f).coerceAtLeast(0f)

                        // Volume-preserving subtle vertical squish while stretching horizontally
                        val vInset = (size.height * (stretchExcess * 0.085f)).coerceIn(0f, size.height * 0.12f)
                        val pillX = size.width * l
                        val pillW = (size.width * currentWidth).coerceAtLeast(1f)
                        val pillH = (size.height - vInset * 2f).coerceAtLeast(1f)
                        val corner = CornerRadius(pillH / 2f, pillH / 2f)

                        if (palette.isLiquidGlass) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(indicatorFillColor, indicatorFillBottom),
                                    startY = vInset,
                                    endY = vInset + pillH
                                ),
                                topLeft = Offset(pillX, vInset),
                                size = Size(pillW, pillH),
                                cornerRadius = corner
                            )
                        } else {
                            drawRoundRect(
                                color = indicatorFillColor,
                                topLeft = Offset(pillX, vInset),
                                size = Size(pillW, pillH),
                                cornerRadius = corner
                            )
                        }
                    }
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                segments.forEachIndexed { index, segment ->
                    val isSelected = index == safeIndex
                    val bounds = segmentBounds[index]
                    val segCenter = (bounds.first + bounds.second) * 0.5f

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) selectedTextColor else palette.textSecondary,
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                        label = "elastic_seg_color_$index"
                    )
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressScale = rememberSpringPressScale(interactionSource, pressedScale = 0.94f)

                    Box(
                        modifier = Modifier
                            .weight(segment.weight)
                            .fillMaxHeight()
                            .clip(containerShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (index != safeIndex) {
                                    haptics.segment()
                                    onSelectIndex(index)
                                }
                            }
                            .then(
                                if (segment.testTag != null) Modifier.testTag(segment.testTag) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = segment.label,
                            fontFamily = fontFamily,
                            fontSize = fontSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .graphicsLayer {
                                    // Directional elastic text nudge as the pill stretches into/out of this slot
                                    if (safeIndex >= 0) {
                                        val pillLeft = leftEdge.value
                                        val pillRight = rightEdge.value
                                        val pillCenter = (pillLeft + pillRight) * 0.5f
                                        val targetBounds = segmentBounds[safeIndex]
                                        val targetW = (targetBounds.second - targetBounds.first).coerceAtLeast(0.01f)
                                        val stretch = (((pillRight - pillLeft) / targetW) - 1f).coerceIn(0f, 1.2f)
                                        val dist = pillCenter - segCenter
                                        val proximity = (1f - abs(dist) * 2.2f).coerceIn(0f, 1f)
                                        val directionSign = when {
                                            dist > 0.005f -> 1f
                                            dist < -0.005f -> -1f
                                            else -> 0f
                                        }
                                        // Nudge text subtly toward the sweeping pill edge during transition
                                        translationX = directionSign * stretch * proximity * 6.5.dp.toPx()
                                        val popScale = pressScale * (1f + if (isSelected) stretch * 0.03f else 0f)
                                        scaleX = popScale
                                        scaleY = popScale
                                    } else {
                                        scaleX = pressScale
                                        scaleY = pressScale
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Smooth vertical rolling text transition used for hero metrics & date readouts
 * whenever the value changes (purely event-driven, zero idle animation).
 */
@Composable
fun RollingMetricText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            ) { h -> (h * 0.35f).toInt() } + fadeIn(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )) togetherWith (slideOutVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
            ) { h -> -(h * 0.30f).toInt() } + fadeOut(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ))
        },
        label = "rolling_metric_text",
        modifier = modifier
    ) { value ->
        Text(
            text = value,
            style = style,
            color = color,
            maxLines = maxLines,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
