package com.example.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Serene, GPU-friendly ambient backdrop layer tailored to each of the 3 themes:
 * - **Liquid Glass**: Gently drifting smoked-obsidian optical caustic pools and delicate prism arcs.
 * - **Nothing Dot**: Pitch-black void with authentic Nothing dot-matrix fades, breathing dot constellation, and a pulsing Nothing Red telemetry LED.
 * - **Solar Amber**: Warm matte carbon with an ultra-slow rotating celestial chronograph azimuth halo and cardinal ticks.
 */
@Composable
fun ThemeBackdropLayer(
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
    val transition = rememberInfiniteTransition(label = "theme_backdrop_ambient")

    when (palette.theme) {
        AppVisualTheme.LIQUID_GLASS -> {
            val driftPhase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 9500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glass_caustic_drift"
            )

            Canvas(modifier = modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(color = palette.background)

                val shiftX = (driftPhase - 0.5f) * w * 0.08f
                val shiftY = (driftPhase - 0.5f) * h * 0.045f

                // Drifting optical caustic pool 1 (upper left-center)
                val orb1Center = Offset(w * 0.25f + shiftX, h * 0.14f + shiftY)
                val orb1Radius = (maxOf(w, h) * 0.56f).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1D7E91B0),
                            Color(0x0B44526A),
                            Color.Transparent
                        ),
                        center = orb1Center,
                        radius = orb1Radius
                    ),
                    radius = orb1Radius,
                    center = orb1Center
                )

                // Drifting optical caustic pool 2 (mid-right)
                val orb2Center = Offset(w * 0.82f - shiftX * 0.8f, h * 0.54f - shiftY)
                val orb2Radius = (maxOf(w, h) * 0.48f).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x16647794),
                            Color(0x072E3747),
                            Color.Transparent
                        ),
                        center = orb2Center,
                        radius = orb2Radius
                    ),
                    radius = orb2Radius,
                    center = orb2Center
                )

                // Soft lower anchor caustic
                val orb3Center = Offset(w * 0.22f + shiftX * 0.5f, h * 0.88f)
                val orb3Radius = (maxOf(w, h) * 0.40f).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x1261718A),
                            Color.Transparent
                        ),
                        center = orb3Center,
                        radius = orb3Radius
                    ),
                    radius = orb3Radius,
                    center = orb3Center
                )

                // Twin whisper-thin translucent prism horizon arcs framing upper viewport
                val prismCenter = Offset(w * 0.5f, h * 0.22f)
                val r1 = minOf(w, h) * 0.46f
                val r2 = minOf(w, h) * 0.58f
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.15f to Color.White.copy(alpha = 0.045f + driftPhase * 0.02f),
                        0.38f to Color.Transparent,
                        0.72f to Color(0xFFD5E2F5).copy(alpha = 0.055f),
                        1.0f to Color.Transparent
                    ),
                    startAngle = -140f + driftPhase * 12f,
                    sweepAngle = 280f,
                    useCenter = false,
                    topLeft = Offset(prismCenter.x - r1, prismCenter.y - r1),
                    size = Size(r1 * 2f, r1 * 2f),
                    style = Stroke(width = 0.85.dp.toPx())
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.45f to Color.White.copy(alpha = 0.032f),
                        0.82f to Color.Transparent,
                        1.0f to Color.Transparent
                    ),
                    startAngle = 20f - driftPhase * 10f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(prismCenter.x - r2, prismCenter.y - r2),
                    size = Size(r2 * 2f, r2 * 2f),
                    style = Stroke(width = 0.7.dp.toPx())
                )
            }
        }

        AppVisualTheme.NOTHING_DOT -> {
            val ledPulse by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2800, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "nothing_led_pulse"
            )

            Canvas(modifier = modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(color = palette.background)

                // Deep crimson radial breath behind upper focal area
                val glowCenter = Offset(w * 0.5f, h * 0.21f)
                val glowRadius = maxOf(w, h) * 0.56f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primaryGlow.copy(alpha = 0.44f + ledPulse * 0.12f),
                            palette.primaryDeep.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = glowCenter,
                        radius = glowRadius.coerceAtLeast(1f)
                    ),
                    radius = glowRadius,
                    center = glowCenter
                )

                // Top industrial dot-matrix field with vertical falloff
                val step = 18.dp.toPx()
                val dotRadius = 0.95.dp.toPx()
                val topLimit = h * 0.34f
                var y = step * 0.8f
                var rowIdx = 0
                while (y < topLimit) {
                    val fade = (1f - (y / topLimit)).coerceIn(0f, 1f)
                    var x = step * 0.8f
                    var colIdx = 0
                    while (x < w - step * 0.4f) {
                        val wave = ((rowIdx + colIdx) % 7 == 0)
                        val alpha = fade * (if (wave) 0.075f + ledPulse * 0.035f else 0.042f)
                        drawCircle(
                            color = palette.textPrimary.copy(alpha = alpha),
                            radius = if (wave) 1.15.dp.toPx() else dotRadius,
                            center = Offset(x, y)
                        )
                        x += step
                        colIdx++
                    }
                    y += step
                    rowIdx++
                }

                // Delicate Nothing Glyph curved dotted arc framing upper right
                val glyphCenter = Offset(w * 0.5f, h * 0.24f)
                val glyphRadius = minOf(w, h) * 0.46f
                val glyphDots = 28
                for (i in 0 until glyphDots) {
                    val deg = -55.0 + (110.0 / (glyphDots - 1)) * i
                    val rad = Math.toRadians(deg)
                    val gx = glyphCenter.x + (glyphRadius * cos(rad)).toFloat()
                    val gy = glyphCenter.y + (glyphRadius * sin(rad)).toFloat()
                    val edgeFade = sin((i.toFloat() / (glyphDots - 1)) * PI).toFloat()
                    drawCircle(
                        color = palette.textPrimary.copy(alpha = edgeFade * (0.08f + ledPulse * 0.04f)),
                        radius = 1.2.dp.toPx(),
                        center = Offset(gx, gy)
                    )
                }

                // Pulsing Nothing Red hardware telemetry beacon at top-right margin
                val beacon = Offset(w - 22.dp.toPx(), h * 0.09f)
                drawCircle(
                    color = palette.primary.copy(alpha = 0.20f * ledPulse),
                    radius = 7.dp.toPx(),
                    center = beacon
                )
                drawCircle(
                    color = palette.primary.copy(alpha = 0.65f + 0.35f * ledPulse),
                    radius = 2.dp.toPx(),
                    center = beacon
                )
            }
        }

        AppVisualTheme.SOLAR_AMBER -> {
            val haloRotation by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 90000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "solar_azimuth_rotation"
            )
            val solarBreath by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 6400, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "solar_horizon_breath"
            )

            Canvas(modifier = modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(color = palette.background)

                val center = Offset(w * 0.5f, h * 0.22f)
                val glowRadius = maxOf(w, h) * 0.60f

                // Warm breathing amber horizon radiance
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.primaryGlow.copy(alpha = 0.52f + solarBreath * 0.14f),
                            palette.primaryDeep.copy(alpha = 0.09f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius.coerceAtLeast(1f)
                    ),
                    radius = glowRadius,
                    center = center
                )

                val outerR = minOf(w, h) * 0.45f
                val innerR = minOf(w, h) * 0.35f

                // Ultra-slow rotating celestial chronograph azimuth halo
                rotate(degrees = haloRotation, pivot = center) {
                    drawCircle(
                        color = palette.primary.copy(alpha = 0.055f),
                        radius = outerR,
                        center = center,
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                    // 24 subtle degree hairline ticks around azimuth ring
                    for (i in 0 until 24) {
                        val rad = Math.toRadians(i * 15.0)
                        val isCardinal = i % 6 == 0
                        val len = if (isCardinal) 8.dp.toPx() else 3.5.dp.toPx()
                        drawLine(
                            color = palette.primary.copy(alpha = if (isCardinal) 0.16f else 0.06f),
                            start = Offset(
                                center.x + ((outerR - len) * cos(rad)).toFloat(),
                                center.y + ((outerR - len) * sin(rad)).toFloat()
                            ),
                            end = Offset(
                                center.x + ((outerR + len * 0.4f) * cos(rad)).toFloat(),
                                center.y + ((outerR + len * 0.4f) * sin(rad)).toFloat()
                            ),
                            strokeWidth = if (isCardinal) 1.2.dp.toPx() else 0.7.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    // Twin opposing orbital golden arcs
                    drawArc(
                        color = palette.primary.copy(alpha = 0.11f),
                        startAngle = -35f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - innerR, center.y - innerR),
                        size = Size(innerR * 2f, innerR * 2f),
                        style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = palette.primary.copy(alpha = 0.08f),
                        startAngle = 145f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - innerR, center.y - innerR),
                        size = Size(innerR * 2f, innerR * 2f),
                        style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Subtle bottom warm architectural horizon hairline
                val horizonY = h * 0.91f
                drawLine(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.3f to palette.primary.copy(alpha = 0.06f),
                        0.5f to palette.primary.copy(alpha = 0.12f),
                        0.7f to palette.primary.copy(alpha = 0.06f),
                        1f to Color.Transparent
                    ),
                    start = Offset(0f, horizonY),
                    end = Offset(w, horizonY),
                    strokeWidth = 0.75.dp.toPx()
                )
            }
        }
    }
}

/**
 * Bespoke theme-crafted horizontal divider:
 * - **Liquid Glass**: Fading pearl specular light beam with tiny center refraction droplet.
 * - **Nothing Dot**: Industrial dotted rhythm line anchored by a leading Nothing Red dot.
 * - **Solar Amber**: Fine warm hairline centered on a crisp golden diamond ornament (`── ◇ ──`).
 */
@Composable
fun ThemedDivider(
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
    when {
        palette.isNothingDot -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .drawWithCache {
                        val dotRadius = 0.95.dp.toPx()
                        val spacing = 6.8.dp.toPx()
                        val y = size.height / 2f
                        val dotColor = palette.borderHighlight.copy(alpha = 0.58f)
                        val redDotRadius = 1.8.dp.toPx()
                        onDrawBehind {
                            // Leading Nothing Red anchor dot
                            drawCircle(
                                color = palette.primary.copy(alpha = 0.92f),
                                radius = redDotRadius,
                                center = Offset(redDotRadius, y)
                            )
                            var x = redDotRadius * 2f + spacing
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
        }

        palette.isLiquidGlass -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .drawWithCache {
                        val y = size.height / 2f
                        val lineH = 0.75.dp.toPx()
                        val brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.2f to Color.White.copy(alpha = 0.14f),
                            0.5f to Color.White.copy(alpha = 0.28f),
                            0.8f to Color.White.copy(alpha = 0.14f),
                            1f to Color.Transparent
                        )
                        onDrawBehind {
                            drawRect(
                                brush = brush,
                                topLeft = Offset(0f, y - lineH / 2f),
                                size = Size(size.width, lineH)
                            )
                            // Whisper-soft center glass glint
                            drawCircle(
                                color = Color.White.copy(alpha = 0.35f),
                                radius = 1.4.dp.toPx(),
                                center = Offset(size.width / 2f, y)
                            )
                        }
                    }
            )
        }

        else -> {
            // Solar Amber: Fine warm hairline with centered golden diamond ornament
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .drawWithCache {
                        val y = size.height / 2f
                        val cx = size.width / 2f
                        val diamondR = 2.6.dp.toPx()
                        val gap = diamondR + 5.dp.toPx()
                        val stroke = 0.75.dp.toPx()
                        val leftBrush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            1f to palette.primary.copy(alpha = 0.34f),
                            startX = 0f,
                            endX = (cx - gap).coerceAtLeast(1f)
                        )
                        val rightBrush = Brush.horizontalGradient(
                            0f to palette.primary.copy(alpha = 0.34f),
                            1f to Color.Transparent,
                            startX = (cx + gap).coerceAtLeast(1f),
                            endX = size.width.coerceAtLeast(cx + gap + 1f)
                        )
                        val diamondPath = Path().apply {
                            moveTo(cx, y - diamondR)
                            lineTo(cx + diamondR, y)
                            lineTo(cx, y + diamondR)
                            lineTo(cx - diamondR, y)
                            close()
                        }
                        onDrawBehind {
                            if (cx - gap > 0f) {
                                drawLine(
                                    brush = leftBrush,
                                    start = Offset(0f, y),
                                    end = Offset(cx - gap, y),
                                    strokeWidth = stroke
                                )
                            }
                            if (cx + gap < size.width) {
                                drawLine(
                                    brush = rightBrush,
                                    start = Offset(cx + gap, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = stroke
                                )
                            }
                            drawPath(
                                path = diamondPath,
                                color = palette.primary.copy(alpha = 0.62f),
                                style = Stroke(width = 0.9.dp.toPx())
                            )
                        }
                    }
            )
        }
    }
}

/**
 * Theme-specific micro-ornamented section header for Settings, Schedule, Stats, and Backup screens.
 */
@Composable
fun ThemedSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                when (palette.theme) {
                    AppVisualTheme.LIQUID_GLASS -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.72f), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = size.minDimension * 0.5f
                            ),
                            radius = size.minDimension * 0.5f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = palette.primary,
                            radius = 2.1.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                    AppVisualTheme.NOTHING_DOT -> {
                        drawCircle(
                            color = palette.primary.copy(alpha = 0.28f),
                            radius = 4.6.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = palette.primary,
                            radius = 2.3.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                    AppVisualTheme.SOLAR_AMBER -> {
                        val r = 3.6.dp.toPx()
                        val path = Path().apply {
                            moveTo(cx, cy - r)
                            lineTo(cx + r, cy)
                            lineTo(cx, cy + r)
                            lineTo(cx - r, cy)
                            close()
                        }
                        drawPath(path = path, color = palette.primary)
                    }
                }
            }

            Text(
                text = title.uppercase(),
                fontFamily = if (palette.isNothingDot) palette.displayFont else if (palette.isSolarAmber) palette.displayFont else JetBrainsMono,
                fontSize = if (palette.isNothingDot) 12.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = if (palette.isNothingDot) 1.8.sp else 1.5.sp,
                color = palette.textSecondary
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * Delicate sculpted outer orbital frame wrapping the central hero timer dial.
 */
@Composable
fun ThemedHeroOrbitFrame(
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "hero_orbit_ambient")
    val slowPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_orbit_phase"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = (minOf(size.width, size.height) / 2f) - 2.dp.toPx()

            when (palette.theme) {
                AppVisualTheme.LIQUID_GLASS -> {
                    // Twin delicate frosted prism orbit rings & 4 subtle specular starlight motes
                    drawCircle(
                        brush = Brush.sweepGradient(
                            0.0f to Color.White.copy(alpha = 0.04f),
                            0.25f to Color.White.copy(alpha = 0.16f + slowPhase * 0.05f),
                            0.55f to Color.White.copy(alpha = 0.03f),
                            0.80f to Color(0xFFD5E2F5).copy(alpha = 0.14f),
                            1.0f to Color.White.copy(alpha = 0.04f)
                        ),
                        radius = outerR,
                        center = Offset(cx, cy),
                        style = Stroke(width = 0.75.dp.toPx())
                    )
                    val moteAngles = floatArrayOf(-42f, 32f, 140f, 218f)
                    moteAngles.forEachIndexed { idx, deg ->
                        val rad = Math.toRadians(deg + (if (idx % 2 == 0) slowPhase * 6f else -slowPhase * 6f).toDouble())
                        val mx = cx + (outerR * cos(rad)).toFloat()
                        val my = cy + (outerR * sin(rad)).toFloat()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f + slowPhase * 0.14f),
                            radius = 3.2.dp.toPx(),
                            center = Offset(mx, my)
                        )
                        drawCircle(
                            color = Color(0xFFEAF1FC).copy(alpha = 0.78f),
                            radius = 1.25.dp.toPx(),
                            center = Offset(mx, my)
                        )
                    }
                }

                AppVisualTheme.NOTHING_DOT -> {
                    // Authentic Nothing Glyph segmented outer dot arcs (left & right brackets)
                    val arcDots = 11
                    for (side in listOf(-1, 1)) {
                        val baseDeg = if (side == 1) 0.0 else 180.0
                        for (i in 0 until arcDots) {
                            val t = i.toFloat() / (arcDots - 1).toFloat()
                            val deg = baseDeg - 38.0 + 76.0 * t
                            val rad = Math.toRadians(deg)
                            val dx = cx + (outerR * cos(rad)).toFloat()
                            val dy = cy + (outerR * sin(rad)).toFloat()
                            val isCenterDot = i == arcDots / 2
                            drawCircle(
                                color = if (isCenterDot) {
                                    palette.primary.copy(alpha = 0.65f + slowPhase * 0.35f)
                                } else {
                                    palette.textPrimary.copy(alpha = 0.18f)
                                },
                                radius = if (isCenterDot) 2.2.dp.toPx() else 1.05.dp.toPx(),
                                center = Offset(dx, dy)
                            )
                        }
                    }
                }

                AppVisualTheme.SOLAR_AMBER -> {
                    // Precision chronograph outer bezel brackets & 4 cardinal diamond markers
                    val bracketR = outerR
                    val bracketStroke = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                    val starts = floatArrayOf(-26f, 64f, 154f, 244f)
                    starts.forEach { start ->
                        drawArc(
                            color = palette.primary.copy(alpha = 0.26f + slowPhase * 0.10f),
                            startAngle = start,
                            sweepAngle = 52f,
                            useCenter = false,
                            topLeft = Offset(cx - bracketR, cy - bracketR),
                            size = Size(bracketR * 2f, bracketR * 2f),
                            style = bracketStroke
                        )
                    }
                    for (i in 0 until 4) {
                        val rad = Math.toRadians(-90.0 + i * 90.0)
                        val px = cx + (bracketR * cos(rad)).toFloat()
                        val py = cy + (bracketR * sin(rad)).toFloat()
                        val r = 2.6.dp.toPx()
                        val diamond = Path().apply {
                            moveTo(px, py - r)
                            lineTo(px + r, py)
                            lineTo(px, py + r)
                            lineTo(px - r, py)
                            close()
                        }
                        drawPath(
                            path = diamond,
                            color = palette.primary.copy(alpha = 0.62f + slowPhase * 0.22f)
                        )
                    }
                }
            }
        }
        content()
    }
}

/**
 * Bespoke sculpted timer ring for each of the 3 visual themes with gentle ambient motion.
 */
@Composable
fun ThemedTimerRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 188.dp,
    palette: FocusThemePalette = LocalFocusTheme.current,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "timer_ring_ambient")
    val ambientPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timer_ring_pulse"
    )

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
                AppVisualTheme.NOTHING_DOT -> {
                    val totalDots = 48
                    val activeDots = (totalDots * clamped).toInt()
                        .let { if (clamped > 0f && it == 0) 1 else it }

                    // Inner 12-dot cardinal compass ring
                    val innerR = radius - 11.dp.toPx()
                    for (j in 0 until 12) {
                        val rad = Math.toRadians(-90.0 + 30.0 * j)
                        val ix = cx + (innerR * cos(rad)).toFloat()
                        val iy = cy + (innerR * sin(rad)).toFloat()
                        val isCardinal = j % 3 == 0
                        drawCircle(
                            color = if (isCardinal) {
                                palette.textPrimary.copy(alpha = 0.26f)
                            } else {
                                palette.borderHighlight.copy(alpha = 0.34f)
                            },
                            radius = if (isCardinal) 1.5.dp.toPx() else 0.9.dp.toPx(),
                            center = Offset(ix, iy)
                        )
                    }

                    // Outer 48-LED Nothing dot-matrix ring
                    for (i in 0 until totalDots) {
                        val angleRad = Math.toRadians(-90.0 + (360.0 / totalDots) * i)
                        val dx = cx + (radius * cos(angleRad)).toFloat()
                        val dy = cy + (radius * sin(angleRad)).toFloat()
                        val isActive = i < activeDots
                        val isLeadDot = isActive && i == activeDots - 1

                        if (isActive) {
                            drawCircle(
                                color = palette.primary.copy(
                                    alpha = if (isLeadDot) 0.34f + ambientPulse * 0.20f else 0.14f
                                ),
                                radius = if (isLeadDot) 7.5.dp.toPx() else 4.6.dp.toPx(),
                                center = Offset(dx, dy)
                            )
                        }
                        drawCircle(
                            color = when {
                                isLeadDot -> Color(0xFFFF6B73)
                                isActive -> palette.primary
                                i % 4 == 0 -> palette.borderHighlight.copy(alpha = 0.85f)
                                else -> palette.borderHighlight.copy(alpha = 0.52f)
                            },
                            radius = when {
                                isLeadDot -> (3.1f + ambientPulse * 0.5f).dp.toPx()
                                isActive -> 2.55.dp.toPx()
                                else -> 1.45.dp.toPx()
                            },
                            center = Offset(dx, dy)
                        )
                    }
                }

                AppVisualTheme.LIQUID_GLASS -> {
                    val innerLensRadius = radius - 6.dp.toPx()
                    val sheenOffsetY = (ambientPulse - 0.5f) * innerLensRadius * 0.16f

                    // Deep smoked-obsidian optical glass lens interior
                    drawCircle(
                        brush = Brush.verticalGradient(
                            0.0f to Color(0xFFDCE8FC).copy(alpha = 0.11f + ambientPulse * 0.03f),
                            0.36f to Color(0xFF0A0D14).copy(alpha = 0.78f),
                            1.0f to Color(0xFF93A5C2).copy(alpha = 0.065f)
                        ),
                        radius = innerLensRadius,
                        center = Offset(cx, cy)
                    )

                    // Gently breathing inner liquid-mercury crescent sheen
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.075f),
                                Color.Transparent
                            ),
                            center = Offset(cx - innerLensRadius * 0.25f, cy - innerLensRadius * 0.38f + sheenOffsetY),
                            radius = innerLensRadius * 0.72f
                        ),
                        radius = innerLensRadius,
                        center = Offset(cx, cy)
                    )

                    // Whisper-thin frosted concentric inner bezel hairline
                    drawCircle(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.22f),
                            0.5f to Color.White.copy(alpha = 0.05f),
                            1.0f to Color.White.copy(alpha = 0.12f)
                        ),
                        radius = innerLensRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 0.75.dp.toPx())
                    )

                    // Outer dark smoked track
                    val strokeW = 3.6.dp.toPx()
                    drawCircle(
                        color = Color(0xFFC5D2E8).copy(alpha = 0.11f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = strokeW)
                    )

                    // Sleek titanium-silver mercury progress arc + pulsing glass droplet
                    if (clamped > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to Color(0xFF6B7890),
                                0.45f to Color(0xFFE3ECF9),
                                0.85f to Color(0xFF9FB0CA),
                                1.0f to Color(0xFF6B7890)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * clamped,
                            useCenter = false,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )

                        val tipRad = Math.toRadians(-90.0 + 360.0 * clamped)
                        val tipX = cx + (radius * cos(tipRad)).toFloat()
                        val tipY = cy + (radius * sin(tipRad)).toFloat()
                        drawCircle(
                            color = Color(0xFFDDE8FA).copy(alpha = 0.22f + ambientPulse * 0.14f),
                            radius = (5.8f + ambientPulse * 1.4f).dp.toPx(),
                            center = Offset(tipX, tipY)
                        )
                        drawCircle(
                            color = Color(0xFFF4F8FF),
                            radius = 2.6.dp.toPx(),
                            center = Offset(tipX, tipY)
                        )
                    }
                }

                AppVisualTheme.SOLAR_AMBER -> {
                    val innerBezelR = radius - 9.dp.toPx()

                    // Subtle recessed instrument dial face
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1D180F).copy(alpha = 0.55f),
                                Color(0xFF0A0906).copy(alpha = 0.35f)
                            ),
                            center = Offset(cx, cy),
                            radius = innerBezelR
                        ),
                        radius = innerBezelR,
                        center = Offset(cx, cy)
                    )

                    // Fine inner chronograph bezel hairline
                    drawCircle(
                        color = palette.primary.copy(alpha = 0.16f),
                        radius = innerBezelR,
                        center = Offset(cx, cy),
                        style = Stroke(width = 0.8.dp.toPx())
                    )

                    // 60 precision graduated ticks with bold 5-minute baton indices
                    val ticks = 60
                    val activeTicks = (ticks * clamped).toInt()
                    for (i in 0 until ticks) {
                        val rad = Math.toRadians(-90.0 + (360.0 / ticks) * i)
                        val isCardinal = i % 15 == 0
                        val isMajor = i % 5 == 0
                        val outer = radius + when {
                            isCardinal -> 4.5.dp.toPx()
                            isMajor -> 3.4.dp.toPx()
                            else -> 1.7.dp.toPx()
                        }
                        val inner = radius - when {
                            isCardinal -> 5.0.dp.toPx()
                            isMajor -> 3.8.dp.toPx()
                            else -> 1.7.dp.toPx()
                        }
                        val isActive = i < activeTicks
                        drawLine(
                            color = when {
                                isActive -> palette.primary
                                isCardinal -> palette.primary.copy(alpha = 0.42f)
                                else -> palette.borderHighlight.copy(alpha = 0.62f)
                            },
                            start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                            end = Offset(cx + (outer * cos(rad)).toFloat(), cy + (outer * sin(rad)).toFloat()),
                            strokeWidth = when {
                                isCardinal -> 2.1.dp.toPx()
                                isMajor -> 1.6.dp.toPx()
                                else -> 0.85.dp.toPx()
                            },
                            cap = StrokeCap.Round
                        )
                    }

                    // Molten-amber precision sweep arc along inner bezel + glowing tip
                    if (clamped > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to palette.primaryDeep,
                                0.5f to palette.primary,
                                1.0f to Color(0xFFFFD86B)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * clamped,
                            useCenter = false,
                            topLeft = Offset(cx - innerBezelR, cy - innerBezelR),
                            size = Size(innerBezelR * 2f, innerBezelR * 2f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        val tipRad = Math.toRadians(-90.0 + 360.0 * clamped)
                        val tipX = cx + (innerBezelR * cos(tipRad)).toFloat()
                        val tipY = cy + (innerBezelR * sin(tipRad)).toFloat()
                        drawCircle(
                            color = palette.primary.copy(alpha = 0.28f + ambientPulse * 0.16f),
                            radius = 5.5.dp.toPx(),
                            center = Offset(tipX, tipY)
                        )
                        drawCircle(
                            color = Color(0xFFFFE599),
                            radius = 2.2.dp.toPx(),
                            center = Offset(tipX, tipY)
                        )
                    }
                }
            }
        }
        content()
    }
}

/**
 * Multi-step / quiz progress indicator styled uniquely per theme:
 * - **Liquid Glass**: Sleek frosted specular capsules.
 * - **Nothing Dot**: Glowing Nothing Red LED dot matrix row.
 * - **Solar Amber**: Precision machined amber chronograph bars.
 */
@Composable
fun ThemedProgressSegments(
    total: Int,
    completed: Int,
    modifier: Modifier = Modifier,
    activeIndex: Int = completed,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
    val safeTotal = total.coerceAtLeast(1)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (palette.isNothingDot) 6.dp else 5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until safeTotal) {
            val isDone = i < completed
            val isCurrent = i == activeIndex && !isDone
            when (palette.theme) {
                AppVisualTheme.NOTHING_DOT -> {
                    Canvas(modifier = Modifier.size(if (isCurrent) 10.dp else 8.dp)) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        if (isDone || isCurrent) {
                            drawCircle(
                                color = palette.primary.copy(alpha = if (isCurrent) 0.34f else 0.22f),
                                radius = size.minDimension * 0.5f,
                                center = c
                            )
                        }
                        drawCircle(
                            color = when {
                                isDone -> palette.primary
                                isCurrent -> Color(0xFFFF6B73)
                                else -> palette.borderHighlight.copy(alpha = 0.65f)
                            },
                            radius = when {
                                isDone -> 2.9.dp.toPx()
                                isCurrent -> 3.2.dp.toPx()
                                else -> 1.8.dp.toPx()
                            },
                            center = c
                        )
                    }
                }

                AppVisualTheme.LIQUID_GLASS -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .drawWithCache {
                                val corner = CornerRadius(size.height / 2f, size.height / 2f)
                                onDrawBehind {
                                    drawRoundRect(
                                        brush = when {
                                            isDone -> Brush.horizontalGradient(
                                                listOf(Color(0xFF9EB1CE), Color(0xFFE7EFFC))
                                            )
                                            isCurrent -> Brush.horizontalGradient(
                                                listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.25f))
                                            )
                                            else -> Brush.horizontalGradient(
                                                listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.08f))
                                            )
                                        },
                                        cornerRadius = corner
                                    )
                                }
                            }
                    )
                }

                AppVisualTheme.SOLAR_AMBER -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isCurrent) 4.5.dp else 3.5.dp)
                            .drawWithCache {
                                val corner = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                                onDrawBehind {
                                    drawRoundRect(
                                        color = when {
                                            isDone -> palette.primary
                                            isCurrent -> palette.primary.copy(alpha = 0.55f)
                                            else -> palette.borderHighlight.copy(alpha = 0.58f)
                                        },
                                        cornerRadius = corner
                                    )
                                }
                            }
                    )
                }
            }
        }
    }
}

/**
 * Rich miniature sculpted theme artwork for Settings & First-Install Setup theme pickers.
 * Renders each theme's real visual identity (backdrop + focal ring + micro-details).
 */
@Composable
fun ThemeMiniArtPreview(
    theme: AppVisualTheme,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val previewPalette = FocusThemePalette.from(theme)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(14.dp.toPx(), 14.dp.toPx())

        drawRoundRect(
            color = previewPalette.background,
            cornerRadius = corner
        )

        when (theme) {
            AppVisualTheme.LIQUID_GLASS -> {
                // Soft smoked caustic glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x36869BB8), Color.Transparent),
                        center = Offset(w * 0.26f, h * 0.34f),
                        radius = maxOf(w, h) * 0.65f
                    ),
                    radius = maxOf(w, h) * 0.65f,
                    center = Offset(w * 0.26f, h * 0.34f)
                )
                // Mini frosted glass lens orb on left
                val cx = w * 0.26f
                val cy = h * 0.5f
                val r = minOf(w, h) * 0.28f
                drawCircle(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color(0xFF0A0D14))
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color(0xFF7B8CA6), Color(0xFFEAF2FF), Color(0xFF7B8CA6))),
                    startAngle = -90f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                // Mini floating frosted glass card on right
                val cardX = w * 0.48f
                val cardW = w * 0.44f
                val cardH = h * 0.54f
                val cardY = (h - cardH) / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color(0xCC0D1119))
                    ),
                    topLeft = Offset(cardX, cardY),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                    ),
                    topLeft = Offset(cardX, cardY),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
                // Two sleek titanium line bars inside mini card
                drawRoundRect(
                    color = Color(0xFFD5DFEE).copy(alpha = 0.72f),
                    topLeft = Offset(cardX + 8.dp.toPx(), cardY + cardH * 0.34f),
                    size = Size(cardW * 0.56f, 3.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF8C9BB4).copy(alpha = 0.38f),
                    topLeft = Offset(cardX + 8.dp.toPx(), cardY + cardH * 0.62f),
                    size = Size(cardW * 0.36f, 2.5.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            AppVisualTheme.NOTHING_DOT -> {
                // Background dot-matrix grid
                val step = 8.dp.toPx()
                var gx = step
                while (gx < w - step * 0.5f) {
                    var gy = step
                    while (gy < h - step * 0.5f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = 0.75.dp.toPx(),
                            center = Offset(gx, gy)
                        )
                        gy += step
                    }
                    gx += step
                }
                // Mini Nothing Red LED dot dial on left
                val cx = w * 0.26f
                val cy = h * 0.5f
                val r = minOf(w, h) * 0.28f
                val dots = 16
                for (i in 0 until dots) {
                    val rad = Math.toRadians(-90.0 + (360.0 / dots) * i)
                    val px = cx + (r * cos(rad)).toFloat()
                    val py = cy + (r * sin(rad)).toFloat()
                    val active = i < 11
                    drawCircle(
                        color = if (active) previewPalette.primary else Color(0xFF3A3A48),
                        radius = if (active) 1.7.dp.toPx() else 1.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
                // Mini hardware card with red LED dot
                val cardX = w * 0.48f
                val cardW = w * 0.44f
                val cardH = h * 0.54f
                val cardY = (h - cardH) / 2f
                drawRoundRect(
                    color = previewPalette.surfaceElevated,
                    topLeft = Offset(cardX, cardY),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawRoundRect(
                    color = previewPalette.borderHighlight,
                    topLeft = Offset(cardX, cardY),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
                // Red LED dot in top-right of mini card
                drawCircle(
                    color = previewPalette.primary.copy(alpha = 0.30f),
                    radius = 4.dp.toPx(),
                    center = Offset(cardX + cardW - 7.dp.toPx(), cardY + 7.dp.toPx())
                )
                drawCircle(
                    color = previewPalette.primary,
                    radius = 1.7.dp.toPx(),
                    center = Offset(cardX + cardW - 7.dp.toPx(), cardY + 7.dp.toPx())
                )
                // Dotted progress row inside mini card
                for (d in 0 until 6) {
                    drawCircle(
                        color = if (d < 4) Color.White.copy(alpha = 0.85f) else Color(0xFF3E3E4E),
                        radius = 1.4.dp.toPx(),
                        center = Offset(cardX + 9.dp.toPx() + d * 6.5.dp.toPx(), cardY + cardH * 0.62f)
                    )
                }
            }

            AppVisualTheme.SOLAR_AMBER -> {
                // Warm solar radiance
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(previewPalette.primary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(w * 0.26f, h * 0.5f),
                        radius = maxOf(w, h) * 0.55f
                    ),
                    radius = maxOf(w, h) * 0.55f,
                    center = Offset(w * 0.26f, h * 0.5f)
                )
                // Mini chronograph tick dial
                val cx = w * 0.26f
                val cy = h * 0.5f
                val r = minOf(w, h) * 0.28f
                val ticks = 24
                for (i in 0 until ticks) {
                    val rad = Math.toRadians(-90.0 + (360.0 / ticks) * i)
                    val isMajor = i % 6 == 0
                    val inner = r - if (isMajor) 3.2.dp.toPx() else 1.5.dp.toPx()
                    val outer = r + if (isMajor) 2.2.dp.toPx() else 1.dp.toPx()
                    drawLine(
                        color = if (i < 16) previewPalette.primary else previewPalette.borderHighlight,
                        start = Offset(cx + (inner * cos(rad)).toFloat(), cy + (inner * sin(rad)).toFloat()),
                        end = Offset(cx + (outer * cos(rad)).toFloat(), cy + (outer * sin(rad)).toFloat()),
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 0.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                // Mini machined instrument panel with corner brackets
                val cardX = w * 0.48f
                val cardW = w * 0.44f
                val cardH = h * 0.54f
                val cardY = (h - cardH) / 2f
                drawRoundRect(
                    color = previewPalette.surfaceElevated,
                    topLeft = Offset(cardX, cardY),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    color = previewPalette.primary.copy(alpha = 0.32f),
                    topLeft = Offset(cardX, cardY),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
                // Tiny corner registration ticks
                val b = 4.dp.toPx()
                val ins = 4.dp.toPx()
                val bc = previewPalette.primary.copy(alpha = 0.72f)
                drawLine(bc, Offset(cardX + ins, cardY + ins), Offset(cardX + ins + b, cardY + ins), 1.dp.toPx())
                drawLine(bc, Offset(cardX + ins, cardY + ins), Offset(cardX + ins, cardY + ins + b), 1.dp.toPx())
                drawLine(bc, Offset(cardX + cardW - ins - b, cardY + cardH - ins), Offset(cardX + cardW - ins, cardY + cardH - ins), 1.dp.toPx())
                drawLine(bc, Offset(cardX + cardW - ins, cardY + cardH - ins - b), Offset(cardX + cardW - ins, cardY + cardH - ins), 1.dp.toPx())

                // Golden telemetry bar inside mini card
                drawRoundRect(
                    color = previewPalette.primary.copy(alpha = 0.88f),
                    topLeft = Offset(cardX + 9.dp.toPx(), cardY + cardH * 0.48f),
                    size = Size(cardW * 0.58f, 3.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
            }
        }

        // Crisp outer framing stroke
        drawRoundRect(
            color = if (isSelected) previewPalette.primary.copy(alpha = 0.72f) else previewPalette.borderHighlight.copy(alpha = 0.45f),
            cornerRadius = corner,
            style = Stroke(width = if (isSelected) 1.4.dp.toPx() else 0.8.dp.toPx())
        )
    }
}
