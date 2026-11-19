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
 * Whisper-quiet ambient atmosphere layer cached via drawWithCache for smooth 120Hz scrolling.
 */
@Composable
fun ThemeBackdropLayer(
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
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
                val gridStep = 44.dp.toPx()
                val gridColor = palette.primary.copy(alpha = 0.035f)
                val stroke1Dp = Stroke(width = 0.9.dp.toPx())
                val chronoRadius = minOf(w, h) * 0.38f
                val auraBaseR = minOf(w, h) * 0.22f

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
                        AppVisualTheme.CYBER_ICE -> {
                            var x = 0f
                            while (x < w) {
                                drawLine(gridColor, Offset(x, 0f), Offset(x, h * 0.45f), strokeWidth = 0.75f)
                                x += gridStep
                            }
                            var y = 0f
                            while (y < h * 0.45f) {
                                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.75f)
                                y += gridStep
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
                        AppVisualTheme.ULTRAVIOLET_PULSE -> {
                            for (i in 1..2) {
                                drawCircle(
                                    color = palette.primary.copy(alpha = 0.055f / i),
                                    radius = auraBaseR * i,
                                    center = center,
                                    style = stroke1Dp
                                )
                            }
                        }
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
                AppVisualTheme.CYBER_ICE -> {
                    val strokeW = 2.5.dp.toPx()
                    drawCircle(
                        color = palette.border.copy(alpha = 0.8f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = strokeW)
                    )
                    drawArc(
                        color = palette.primary,
                        startAngle = -90f,
                        sweepAngle = 360f * clamped,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    val outer = radius + 5.dp.toPx()
                    val inner = radius - 3.dp.toPx()
                    for (deg in listOf(0.0, 90.0, 180.0, 270.0)) {
                        val rad = Math.toRadians(deg)
                        drawLine(
                            color = palette.primary.copy(alpha = 0.5f),
                            start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                            end = Offset(cx + (outer * cos(rad)).toFloat(), cy + (outer * sin(rad)).toFloat()),
                            strokeWidth = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
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
                AppVisualTheme.ULTRAVIOLET_PULSE -> {
                    drawCircle(
                        color = palette.border.copy(alpha = 0.7f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(palette.primarySecondary, palette.primary, palette.primarySecondary)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * clamped,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
        content()
    }
}
