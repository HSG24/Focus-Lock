package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.LocalFocusTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Static, GPU-cached ambient backdrop layer tuned per theme (zero live/idle animations):
 * - Nothing Crimson: subtle dot matrix and crimson radial horizon.
 * - Liquid Glass: deep obsidian static specular light pools behind dark glass panels.
 * - Solar Amber: warm subtle chronograph halo.
 * - Nordic Sage: calm basalt horizon rings with organic sage mist.
 */
@Composable
fun ThemeBackdropLayer(
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
    if (palette.isLiquidGlass) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val orb1Center = Offset(w * 0.26f, h * 0.15f)
                    val orb1Radius = (maxOf(w, h) * 0.54f).coerceAtLeast(1f)
                    val orb1Brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x166C7C96),
                            Color(0x093D495C),
                            Color.Transparent
                        ),
                        center = orb1Center,
                        radius = orb1Radius
                    )

                    val orb2Center = Offset(w * 0.80f, h * 0.58f)
                    val orb2Radius = (maxOf(w, h) * 0.46f).coerceAtLeast(1f)
                    val orb2Brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1253627A),
                            Color(0x062E3747),
                            Color.Transparent
                        ),
                        center = orb2Center,
                        radius = orb2Radius
                    )

                    val orb3Center = Offset(w * 0.20f, h * 0.86f)
                    val orb3Radius = (maxOf(w, h) * 0.38f).coerceAtLeast(1f)
                    val orb3Brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1061718A),
                            Color.Transparent
                        ),
                        center = orb3Center,
                        radius = orb3Radius
                    )

                    onDrawBehind {
                        drawRect(color = palette.background)
                        drawCircle(brush = orb1Brush, radius = orb1Radius, center = orb1Center)
                        drawCircle(brush = orb2Brush, radius = orb2Radius, center = orb2Center)
                        drawCircle(brush = orb3Brush, radius = orb3Radius, center = orb3Center)
                    }
                }
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h * 0.22f)
                val glowRadius = maxOf(w, h) * 0.58f
                val glowBrush = Brush.radialGradient(
                    colors = listOf(
                        palette.primaryGlow.copy(alpha = 0.48f),
                        palette.primaryDeep.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius.coerceAtLeast(1f)
                )

                val dotPositions: List<Offset> = if (palette.theme == AppVisualTheme.NOTHING_CRIMSON) {
                    val step = 22.dp.toPx()
                    val pts = ArrayList<Offset>(180)
                    var x = step
                    while (x < w) {
                        var y = step
                        while (y < h * 0.28f) {
                            pts.add(Offset(x, y))
                            y += step
                        }
                        x += step
                    }
                    pts
                } else {
                    emptyList()
                }

                val dotRadius = 0.9.dp.toPx()
                val dotColor = palette.textPrimary.copy(alpha = 0.038f)
                val stroke1Dp = Stroke(width = 0.9.dp.toPx())
                val chronoRadius = minOf(w, h) * 0.38f
                val sageOuterRadius = minOf(w, h) * 0.44f
                val sageInnerRadius = minOf(w, h) * 0.26f

                onDrawBehind {
                    drawRect(color = palette.background)
                    drawCircle(
                        brush = glowBrush,
                        radius = glowRadius,
                        center = center
                    )

                    when (palette.theme) {
                        AppVisualTheme.NOTHING_CRIMSON -> {
                            for (pt in dotPositions) {
                                drawCircle(color = dotColor, radius = dotRadius, center = pt)
                            }
                        }
                        AppVisualTheme.SOLAR_AMBER -> {
                            drawCircle(
                                color = palette.primary.copy(alpha = 0.045f),
                                radius = chronoRadius,
                                center = center,
                                style = stroke1Dp
                            )
                        }
                        AppVisualTheme.NORDIC_SAGE -> {
                            drawCircle(
                                color = palette.primary.copy(alpha = 0.045f),
                                radius = sageInnerRadius,
                                center = center,
                                style = stroke1Dp
                            )
                            drawCircle(
                                color = palette.primary.copy(alpha = 0.025f),
                                radius = sageOuterRadius,
                                center = center,
                                style = stroke1Dp
                            )
                        }
                        AppVisualTheme.LIQUID_GLASS -> Unit
                    }
                }
            }
    )
}

@Composable
fun ThemedDivider(
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
    if (palette.useDottedDividers) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(3.dp)
                .drawWithCache {
                    val dotRadius = 0.9.dp.toPx()
                    val spacing = 7.dp.toPx()
                    val y = size.height / 2f
                    val dotColor = palette.borderHighlight.copy(alpha = 0.45f)
                    onDrawBehind {
                        var x = dotRadius
                        while (x < size.width - dotRadius) {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                            x += spacing
                        }
                    }
                }
        )
    } else if (palette.isLiquidGlass) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(0.75.dp)
                .drawWithCache {
                    val brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.2f to Color.White.copy(alpha = 0.16f),
                        0.5f to Color.White.copy(alpha = 0.24f),
                        0.8f to Color.White.copy(alpha = 0.16f),
                        1f to Color.Transparent
                    )
                    onDrawBehind {
                        drawRect(brush = brush)
                    }
                }
        )
    } else {
        HorizontalDivider(
            modifier = modifier,
            thickness = 0.5.dp,
            color = palette.border.copy(alpha = 0.75f)
        )
    }
}

@Composable
fun ThemedTimerRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 188.dp,
    palette: FocusThemePalette = LocalFocusTheme.current,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val clamped = progress.coerceIn(0f, 1f)
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = (minOf(this.size.width, this.size.height) / 2f) - 10.dp.toPx()

            when (palette.theme) {
                AppVisualTheme.NOTHING_CRIMSON -> {
                    val totalDots = 44
                    val activeDots = (totalDots * clamped).toInt()
                        .let { if (clamped > 0f && it == 0) 1 else it }
                    for (i in 0 until totalDots) {
                        val angleRad = Math.toRadians(-90.0 + (360.0 / totalDots) * i)
                        val dx = cx + (radius * cos(angleRad)).toFloat()
                        val dy = cy + (radius * sin(angleRad)).toFloat()
                        val isActive = i < activeDots
                        drawCircle(
                            color = if (isActive) palette.primary else palette.borderHighlight.copy(alpha = 0.65f),
                            radius = if (isActive) 2.8.dp.toPx() else 1.6.dp.toPx(),
                            center = Offset(dx, dy)
                        )
                    }
                }
                AppVisualTheme.LIQUID_GLASS -> {
                    val innerLensRadius = radius - 6.dp.toPx()
                    // Static deep smoked obsidian glass lens interior (borderless, zero live animation)
                    drawCircle(
                        brush = Brush.verticalGradient(
                            0.0f to Color(0xFFD6E2F5).copy(alpha = 0.085f),
                            0.40f to Color(0xFF0A0D14).copy(alpha = 0.74f),
                            1.0f to Color(0xFF8C9BB5).copy(alpha = 0.05f)
                        ),
                        radius = innerLensRadius,
                        center = Offset(cx, cy)
                    )
                    // Outer dark smoked track
                    val strokeW = 3.5.dp.toPx()
                    drawCircle(
                        color = Color(0xFFC5D2E8).copy(alpha = 0.10f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = strokeW)
                    )
                    // Sleek dark-titanium mercury progress arc
                    if (clamped > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to Color(0xFF6B7890),
                                0.5f to Color(0xFFD5DFEE),
                                1.0f to Color(0xFF6B7890)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * clamped,
                            useCenter = false,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        // Subtle static specular droplet at the tip of the arc
                        val tipRad = Math.toRadians(-90.0 + 360.0 * clamped)
                        val tipX = cx + (radius * cos(tipRad)).toFloat()
                        val tipY = cy + (radius * sin(tipRad)).toFloat()
                        drawCircle(
                            color = Color(0xFFD5DFEE).copy(alpha = 0.24f),
                            radius = 5.5.dp.toPx(),
                            center = Offset(tipX, tipY)
                        )
                        drawCircle(
                            color = Color(0xFFEAF0FA),
                            radius = 2.4.dp.toPx(),
                            center = Offset(tipX, tipY)
                        )
                    }
                }
                AppVisualTheme.SOLAR_AMBER -> {
                    val ticks = 60
                    val activeTicks = (ticks * clamped).toInt()
                    for (i in 0 until ticks) {
                        val rad = Math.toRadians(-90.0 + (360.0 / ticks) * i)
                        val isMajor = i % 5 == 0
                        val outer = radius + if (isMajor) 3.5.dp.toPx() else 1.8.dp.toPx()
                        val inner = radius - if (isMajor) 4.dp.toPx() else 1.8.dp.toPx()
                        drawLine(
                            color = if (i < activeTicks) palette.primary else palette.borderHighlight.copy(alpha = 0.65f),
                            start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                            end = Offset(cx + (outer * cos(rad)).toFloat(), cy + (outer * sin(rad)).toFloat()),
                            strokeWidth = if (isMajor) 1.8.dp.toPx() else 0.9.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                AppVisualTheme.NORDIC_SAGE -> {
                    val strokeW = 3.2.dp.toPx()
                    drawCircle(
                        color = palette.border.copy(alpha = 0.7f),
                        radius = radius - 6.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 0.75.dp.toPx())
                    )
                    drawCircle(
                        color = palette.borderHighlight.copy(alpha = 0.5f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = strokeW)
                    )
                    for (i in 0 until 12) {
                        val rad = Math.toRadians(-90.0 + 30.0 * i)
                        val dotX = cx + ((radius - 6.dp.toPx()) * cos(rad)).toFloat()
                        val dotY = cy + ((radius - 6.dp.toPx()) * sin(rad)).toFloat()
                        drawCircle(
                            color = palette.primary.copy(alpha = if (i % 3 == 0) 0.42f else 0.18f),
                            radius = if (i % 3 == 0) 1.6.dp.toPx() else 1.dp.toPx(),
                            center = Offset(dotX, dotY)
                        )
                    }
                    if (clamped > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(palette.primarySecondary, palette.primary, palette.primarySecondary)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * clamped,
                            useCenter = false,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
        content()
    }
}
