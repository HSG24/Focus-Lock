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

@Composable
fun ThemeBackdropLayer(
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.28f)

        drawRect(color = palette.background)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.primaryGlow.copy(alpha = 0.55f),
                    palette.primaryDeep.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = maxOf(w, h) * 0.65f
            ),
            radius = maxOf(w, h) * 0.65f,
            center = center
        )

        when (palette.theme) {
            AppVisualTheme.NOTHING_CRIMSON -> {
                // Subtle Nothing dot-matrix constellation in top corners
                val step = 18.dp.toPx()
                val dotR = 1.1.dp.toPx()
                val dotColor = palette.textPrimary.copy(alpha = 0.06f)
                var x = step
                while (x < w) {
                    var y = step
                    while (y < h * 0.35f) {
                        drawCircle(color = dotColor, radius = dotR, center = Offset(x, y))
                        y += step
                    }
                    x += step
                }
            }
            AppVisualTheme.CYBER_ICE -> {
                // Geometric HUD grid lines
                val gridStep = 36.dp.toPx()
                val lineColor = palette.primary.copy(alpha = 0.055f)
                var x = 0f
                while (x < w) {
                    drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    x += gridStep
                }
                var y = 0f
                while (y < h) {
                    drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    y += gridStep
                }
            }
            AppVisualTheme.SOLAR_AMBER -> {
                // Luxury chronograph radial hairline markers near top center
                val ringRadius = minOf(w, h) * 0.42f
                drawCircle(
                    color = palette.primary.copy(alpha = 0.06f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            AppVisualTheme.ULTRAVIOLET_PULSE -> {
                // Concentric neon aura rings
                val baseR = minOf(w, h) * 0.25f
                for (i in 1..3) {
                    drawCircle(
                        color = palette.primary.copy(alpha = (0.10f / i)),
                        radius = baseR * i,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun ThemedDivider(
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
    if (palette.useDottedDividers) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            val dotRadius = 1.2.dp.toPx()
            val spacing = 6.dp.toPx()
            val y = size.height / 2f
            val dotColor = palette.borderHighlight.copy(alpha = 0.85f)
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
    } else {
        HorizontalDivider(
            modifier = modifier,
            thickness = 0.5.dp,
            color = palette.border
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
                    val totalDots = 40
                    val activeDots = (totalDots * clamped).toInt()
                        .let { if (clamped > 0f && it == 0) 1 else it }
                    for (i in 0 until totalDots) {
                        val angleRad = Math.toRadians(-90.0 + (360.0 / totalDots) * i)
                        val dx = cx + (radius * cos(angleRad)).toFloat()
                        val dy = cy + (radius * sin(angleRad)).toFloat()
                        val isActive = i < activeDots
                        drawCircle(
                            color = if (isActive) palette.primary else palette.borderHighlight,
                            radius = if (isActive) 3.4.dp.toPx() else 2.2.dp.toPx(),
                            center = Offset(dx, dy)
                        )
                    }
                }
                AppVisualTheme.CYBER_ICE -> {
                    val strokeW = 3.dp.toPx()
                    drawCircle(
                        color = palette.border,
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
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    // Corner HUD ticks
                    val outer = radius + 6.dp.toPx()
                    val inner = radius - 4.dp.toPx()
                    for (deg in listOf(0.0, 90.0, 180.0, 270.0)) {
                        val rad = Math.toRadians(deg)
                        drawLine(
                            color = palette.primary.copy(alpha = 0.7f),
                            start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                            end = Offset(cx + (outer * cos(rad)).toFloat(), cy + (outer * sin(rad)).toFloat()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
                AppVisualTheme.SOLAR_AMBER -> {
                    val ticks = 60
                    val activeTicks = (ticks * clamped).toInt()
                    for (i in 0 until ticks) {
                        val rad = Math.toRadians(-90.0 + (360.0 / ticks) * i)
                        val isMajor = i % 5 == 0
                        val outer = radius + if (isMajor) 4.dp.toPx() else 2.dp.toPx()
                        val inner = radius - if (isMajor) 5.dp.toPx() else 2.dp.toPx()
                        drawLine(
                            color = if (i < activeTicks) palette.primary else palette.borderHighlight,
                            start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                            end = Offset(cx + (outer * cos(rad)).toFloat(), cy + (outer * sin(rad)).toFloat()),
                            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                AppVisualTheme.ULTRAVIOLET_PULSE -> {
                    drawCircle(
                        color = palette.primary.copy(alpha = 0.14f),
                        radius = radius + 6.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = palette.border,
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 5.dp.toPx())
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
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
        content()
    }
}
