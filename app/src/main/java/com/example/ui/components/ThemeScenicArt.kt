package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppVisualTheme
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalFocusTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * Identifies each screen so scenic artwork shifts composition, celestial placement,
 * and nature/architectural silhouettes uniquely per page while maintaining visual harmony.
 */
enum class ScenicPageVariant {
    HOME,
    SCHEDULE,
    STATS,
    SETTINGS,
    SHARE,
    BACKUP,
    SETUP,
    LOCK
}

/**
 * Full-bleed artistic fine-art layer rendered quietly over [ThemeBackdropLayer] and
 * beneath foreground content.
 *
 * Each of the 3 themes renders a completely distinct artistic world:
 * - [AppVisualTheme.LIQUID_GLASS]: Nocturnal moonlit peaks, tranquil lake reflections,
 *   drifting aurora mist, and translucent botanical reed/lotus line-art.
 * - [AppVisualTheme.NOTHING_DOT]: Bold minimalist Bauhaus graphic art — sculpted
 *   halftone dot-matrix mountain ridges, acoustic wave horizons, and Nothing Red focal suns.
 * - [AppVisualTheme.SOLAR_AMBER]: Warm desert dusk — layered sunlit sand dunes,
 *   celestial eclipse astrolabe arcs, and delicate golden olive-frond line-art.
 */
@Composable
fun ThemeScenicBackdropOverlay(
    variant: ScenicPageVariant,
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
    val infinite = rememberInfiniteTransition(label = "theme_scenic_ambient")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scenic_drift"
    )

    // Keep lock screens extra calm and ultra-minimal so focus stays unbroken.
    val intensity = when (variant) {
        ScenicPageVariant.LOCK -> 0.52f
        ScenicPageVariant.HOME, ScenicPageVariant.SETUP -> 1.0f
        else -> 0.86f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        when (palette.theme) {
            AppVisualTheme.LIQUID_GLASS -> drawLiquidGlassScenic(variant, palette, drift, intensity)
            AppVisualTheme.NOTHING_DOT -> drawNothingDotScenic(variant, palette, drift, intensity)
            AppVisualTheme.SOLAR_AMBER -> drawSolarAmberScenic(variant, palette, drift, intensity)
        }
    }
}

/**
 * Sculpted minimalist fine-art landscape card designed to gracefully fill bottom
 * scroll breathing room across scrollable screens without adding clutter.
 */
@Composable
fun ThemeScenicVignetteFooter(
    caption: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    palette: FocusThemePalette = LocalFocusTheme.current
) {
    val shape = remember { RoundedCornerShape(26.dp) }
    val infinite = rememberInfiniteTransition(label = "vignette_ambient")
    val shimmer by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vignette_shimmer"
    )

    Box(
        modifier = modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .height(92.dp)
            .focusGlassPanel(shape = shape, palette = palette)
            .clip(shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (palette.theme) {
                AppVisualTheme.LIQUID_GLASS -> drawLiquidGlassVignette(palette, shimmer)
                AppVisualTheme.NOTHING_DOT -> drawNothingDotVignette(palette, shimmer)
                AppVisualTheme.SOLAR_AMBER -> drawSolarAmberVignette(palette, shimmer)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = caption,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.6.sp,
                    color = palette.primary.copy(alpha = 0.92f)
                )
                Text(
                    text = subtitle,
                    fontFamily = palette.displayFont,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary.copy(alpha = 0.82f)
                )
            }
            Text(
                text = palette.theme.badgeTag,
                fontFamily = JetBrainsMono,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                color = palette.textSecondary.copy(alpha = 0.72f)
            )
        }
    }
}

// =============================================================================
// 1. LIQUID GLASS: MOONLIT PEAKS, TRANQUIL LAKE RIPPLES & BOTANICAL GLASS ART
// =============================================================================

private fun DrawScope.drawLiquidGlassScenic(
    variant: ScenicPageVariant,
    palette: FocusThemePalette,
    drift: Float,
    intensity: Float
) {
    val w = size.width
    val h = size.height
    val iceBlue = Color(0xFF93C5FD)
    val softIndigo = Color(0xFF818CF8)

    // Flowing twin translucent aurora mist filaments + tiny 4-point starlight glints in upper space
    val ribbonY = when (variant) {
        ScenicPageVariant.HOME, ScenicPageVariant.SETUP -> h * 0.21f
        ScenicPageVariant.STATS, ScenicPageVariant.SHARE -> h * 0.27f
        else -> h * 0.19f
    }
    val driftShift = (drift - 0.5f) * 16.dp.toPx()
    val auroraPath = Path().apply {
        moveTo(-w * 0.04f, ribbonY + 10.dp.toPx())
        cubicTo(
            w * 0.28f, ribbonY - 24.dp.toPx() + driftShift,
            w * 0.68f, ribbonY + 28.dp.toPx() - driftShift,
            w * 1.04f, ribbonY - 6.dp.toPx()
        )
    }
    drawPath(
        path = auroraPath,
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                iceBlue.copy(alpha = 0.16f * intensity),
                Color.White.copy(alpha = 0.20f * intensity),
                softIndigo.copy(alpha = 0.12f * intensity),
                Color.Transparent
            )
        ),
        style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
    )

    val upperMistPath = Path().apply {
        moveTo(w * 0.04f, ribbonY - 22.dp.toPx())
        cubicTo(
            w * 0.36f, ribbonY - 40.dp.toPx() - driftShift * 0.6f,
            w * 0.70f, ribbonY - 6.dp.toPx() + driftShift * 0.6f,
            w * 0.96f, ribbonY - 26.dp.toPx()
        )
    }
    drawPath(
        path = upperMistPath,
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.11f * intensity),
                iceBlue.copy(alpha = 0.14f * intensity),
                Color.Transparent
            )
        ),
        style = Stroke(width = 0.85.dp.toPx(), cap = StrokeCap.Round)
    )

    // Minimal 4-point starlight glints in the upper sanctuary sky (non-circular, airy)
    val glintFlip = variant == ScenicPageVariant.SCHEDULE || variant == ScenicPageVariant.SHARE
    drawMinimalStarGlint(
        center = Offset(w * (if (glintFlip) 0.16f else 0.84f), h * 0.135f),
        radius = 5.2.dp.toPx(),
        color = Color.White.copy(alpha = (0.34f + 0.12f * drift) * intensity)
    )
    drawMinimalStarGlint(
        center = Offset(w * (if (glintFlip) 0.26f else 0.74f), h * 0.175f),
        radius = 3.0.dp.toPx(),
        color = iceBlue.copy(alpha = (0.28f + 0.10f * (1f - drift)) * intensity)
    )
    drawMinimalStarGlint(
        center = Offset(w * (if (glintFlip) 0.82f else 0.18f), h * 0.155f),
        radius = 3.4.dp.toPx(),
        color = Color.White.copy(alpha = (0.24f + 0.08f * drift) * intensity)
    )

    // Lower misty mountain silhouettes reflected in a calm midnight glass lake
    val horizonY = h * (if (variant == ScenicPageVariant.LOCK) 0.875f else 0.825f)
    val peakAmp = h * (if (variant == ScenicPageVariant.LOCK) 0.055f else 0.088f)
    val peakShift = when (variant) {
        ScenicPageVariant.SCHEDULE, ScenicPageVariant.SHARE -> 0.14f
        ScenicPageVariant.STATS, ScenicPageVariant.SETTINGS -> -0.10f
        else -> 0f
    }

    // Distant translucent mist ridge
    val farRidge = Path().apply {
        moveTo(0f, horizonY)
        cubicTo(
            w * (0.20f + peakShift), horizonY - peakAmp * 1.05f,
            w * (0.40f + peakShift), horizonY - peakAmp * 0.35f,
            w * 0.58f, horizonY - peakAmp * 0.82f
        )
        cubicTo(
            w * 0.76f, horizonY - peakAmp * 1.18f,
            w * 0.90f, horizonY - peakAmp * 0.45f,
            w, horizonY - peakAmp * 0.28f
        )
        lineTo(w, horizonY)
        close()
    }
    drawPath(
        path = farRidge,
        brush = Brush.verticalGradient(
            colors = listOf(
                iceBlue.copy(alpha = 0.13f * intensity),
                Color.White.copy(alpha = 0.025f * intensity)
            ),
            startY = horizonY - peakAmp * 1.2f,
            endY = horizonY
        )
    )

    // Near sculpted peak contour line (crisp fine-art silver edge)
    val nearLine = Path().apply {
        moveTo(0f, horizonY - peakAmp * 0.22f)
        cubicTo(
            w * 0.18f, horizonY - peakAmp * 0.72f,
            w * 0.36f, horizonY - peakAmp * 0.12f,
            w * 0.54f, horizonY - peakAmp * 0.48f
        )
        cubicTo(
            w * 0.74f, horizonY - peakAmp * 0.84f,
            w * 0.88f, horizonY - peakAmp * 0.25f,
            w, horizonY - peakAmp * 0.36f
        )
    }
    drawPath(
        path = nearLine,
        brush = Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.06f * intensity),
                Color.White.copy(alpha = 0.24f * intensity),
                iceBlue.copy(alpha = 0.28f * intensity),
                Color.White.copy(alpha = 0.08f * intensity)
            )
        ),
        style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
    )

    // Subtle mirror reflection below horizon + gentle breathing water ripples
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.20f * intensity),
                iceBlue.copy(alpha = 0.22f * intensity),
                Color.Transparent
            )
        ),
        start = Offset(w * 0.06f, horizonY),
        end = Offset(w * 0.94f, horizonY),
        strokeWidth = 0.9.dp.toPx()
    )
    for (r in 1..4) {
        val y = horizonY + r * 14.dp.toPx()
        val halfSpan = w * (0.38f - r * 0.065f)
        val cx = w * 0.5f + (if (r % 2 == 0) 1f else -1f) * driftShift * 0.65f
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = (0.16f - r * 0.025f) * intensity),
                    Color.Transparent
                ),
                startX = cx - halfSpan,
                endX = cx + halfSpan
            ),
            start = Offset(cx - halfSpan, y),
            end = Offset(cx + halfSpan, y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Minimalist botanical lotus/reed line-art curving gently from a corner margin
    if (variant != ScenicPageVariant.LOCK) {
        val onRight = variant == ScenicPageVariant.SCHEDULE || variant == ScenicPageVariant.SETTINGS
        drawMinimalBotanicalStem(
            base = Offset(if (onRight) w * 0.93f else w * 0.07f, h * 0.89f),
            height = h * 0.19f,
            leanRight = !onRight,
            strokeColor = Color.White.copy(alpha = 0.20f * intensity),
            accentColor = iceBlue.copy(alpha = 0.28f * intensity)
        )
    }
}

private fun DrawScope.drawLiquidGlassVignette(palette: FocusThemePalette, shimmer: Float) {
    val w = size.width
    val h = size.height
    val iceBlue = Color(0xFF93C5FD)

    // Soft moonlight reflection wash
    drawCircle(
        brush = Brush.radialGradient(
            listOf(iceBlue.copy(alpha = 0.20f + 0.06f * shimmer), Color.Transparent),
            center = Offset(w * 0.82f, h * 0.28f),
            radius = h * 0.95f
        ),
        radius = h * 0.95f,
        center = Offset(w * 0.82f, h * 0.28f)
    )
    // Crescent moon
    val mr = 10.dp.toPx()
    val mc = Offset(w * 0.84f, h * 0.30f)
    drawArc(
        color = Color.White.copy(alpha = 0.55f),
        startAngle = -55f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = Offset(mc.x - mr, mc.y - mr),
        size = Size(mr * 2f, mr * 2f),
        style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
    )
    // Twin mountain ridge silhouette + mirrored water sheen
    val ridge = Path().apply {
        moveTo(0f, h * 0.76f)
        cubicTo(w * 0.26f, h * 0.28f, w * 0.48f, h * 0.72f, w * 0.68f, h * 0.40f)
        cubicTo(w * 0.82f, h * 0.18f, w * 0.92f, h * 0.56f, w, h * 0.50f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = ridge,
        brush = Brush.verticalGradient(
            listOf(iceBlue.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f)),
            startY = h * 0.2f,
            endY = h
        )
    )
    drawLine(
        color = Color.White.copy(alpha = 0.22f),
        start = Offset(w * 0.52f, h * 0.78f),
        end = Offset(w * 0.92f, h * 0.78f),
        strokeWidth = 0.9.dp.toPx(),
        cap = StrokeCap.Round
    )
}

// =============================================================================
// 2. NOTHING DOT: HALFTONE MOUNTAIN WAVES, INDUSTRIAL MONOLITHS & RED SUN
// =============================================================================

private fun DrawScope.drawNothingDotScenic(
    variant: ScenicPageVariant,
    palette: FocusThemePalette,
    drift: Float,
    intensity: Float
) {
    val w = size.width
    val h = size.height
    val dotRed = palette.primary
    val dotWhite = Color.White

    // Minimal industrial dotted wave & crisp Nothing Red calibration dash in upper margin
    val upperY = when (variant) {
        ScenicPageVariant.HOME, ScenicPageVariant.SETUP -> h * 0.165f
        ScenicPageVariant.LOCK -> h * 0.125f
        else -> h * 0.155f
    }
    val anchorRight = variant == ScenicPageVariant.HOME ||
        variant == ScenicPageVariant.SCHEDULE ||
        variant == ScenicPageVariant.SETTINGS ||
        variant == ScenicPageVariant.SHARE
    val waveCount = 15
    val waveStep = 7.5.dp.toPx()
    val waveSpan = (waveCount - 1) * waveStep
    val waveStartX = if (anchorRight) w - 26.dp.toPx() - waveSpan else 26.dp.toPx()
    val waveAmp = 5.5.dp.toPx()

    for (i in 0 until waveCount) {
        val frac = i.toFloat() / (waveCount - 1).toFloat()
        val x = waveStartX + i * waveStep
        val envelope = sin(frac * PI).toFloat()
        val y = upperY - sin(frac * PI * 2.0 + drift * 0.8).toFloat() * waveAmp * envelope
        val isAccent = i == waveCount / 2
        drawCircle(
            color = if (isAccent) {
                dotRed.copy(alpha = 0.85f * intensity)
            } else {
                dotWhite.copy(alpha = (0.08f + 0.18f * envelope) * intensity)
            },
            radius = if (isAccent) 1.65.dp.toPx() else 1.05.dp.toPx(),
            center = Offset(x, y)
        )
    }

    // Crisp subtle horizontal red telemetry dash beside the opposite upper shoulder
    val dashStartX = if (anchorRight) 26.dp.toPx() else w - 42.dp.toPx()
    val dashY = upperY + 4.dp.toPx()
    drawLine(
        color = dotRed.copy(alpha = 0.62f * intensity),
        start = Offset(dashStartX, dashY),
        end = Offset(dashStartX + 12.dp.toPx(), dashY),
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = dotWhite.copy(alpha = 0.18f * intensity),
        start = Offset(dashStartX + 16.dp.toPx(), dashY),
        end = Offset(dashStartX + 24.dp.toPx(), dashY),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Unified, strictly grounded Halftone Dot-Matrix Horizon along the bottom
    // Every dot lives on one uniform orthogonal grid (no overlapping grids, no floating dots).
    val cell = 10.dp.toPx()
    val cols = ((w - cell) / cell).toInt().coerceIn(20, 52)
    val gridWidth = cols * cell
    val startX = (w - gridWidth) * 0.5f
    val baseY = h - cell * 0.7f
    val maxRows = ((h * (if (variant == ScenicPageVariant.LOCK) 0.095f else 0.145f)) / cell)
        .toInt()
        .coerceIn(4, 14)

    val phaseOffset = when (variant) {
        ScenicPageVariant.HOME -> 0.15
        ScenicPageVariant.SCHEDULE -> 1.15
        ScenicPageVariant.STATS -> 2.10
        ScenicPageVariant.SETTINGS -> 3.05
        ScenicPageVariant.SHARE -> 0.65
        ScenicPageVariant.BACKUP -> 1.70
        ScenicPageVariant.SETUP -> 0.35
        ScenicPageVariant.LOCK -> 2.45
    }

    val leftBeaconCol = (cols * 0.25f).toInt()
    val rightBeaconCol = (cols * 0.75f).toInt()

    for (c in 0..cols) {
        val u = c.toFloat() / cols.toFloat()
        val x = startX + c * cell
        // Smooth, strictly continuous dual-wave ridge silhouette
        val waveA = ((sin(u * PI * 2.0 + phaseOffset) + 1.0) * 0.5).toFloat()
        val waveB = ((sin(u * PI * 4.0 - phaseOffset * 0.65 + drift * 0.35) + 1.0) * 0.5).toFloat()
        val heightFactor = (0.20f + 0.58f * waveA + 0.22f * waveB).coerceIn(0.18f, 1f)
        val rowsInCol = (heightFactor * maxRows).toInt().coerceIn(2, maxRows)

        for (r in 0 until rowsInCol) {
            val y = baseY - r * cell
            val elevationFrac = if (rowsInCol <= 1) 1f else r.toFloat() / (rowsInCol - 1).toFloat()
            val isCrest = r == rowsInCol - 1
            val isRedBeacon = isCrest && (c == leftBeaconCol || c == rightBeaconCol)
            val alpha = ((0.06f + 0.21f * elevationFrac) * intensity).coerceAtMost(0.32f)
            drawCircle(
                color = if (isRedBeacon) dotRed.copy(alpha = 0.88f * intensity) else dotWhite.copy(alpha = alpha),
                radius = when {
                    isRedBeacon -> 2.1.dp.toPx()
                    isCrest -> 1.55.dp.toPx()
                    else -> (0.85f + 0.45f * elevationFrac).dp.toPx()
                },
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawNothingDotVignette(palette: FocusThemePalette, shimmer: Float) {
    val w = size.width
    val h = size.height
    val dotRed = palette.primary
    val spacing = 8.dp.toPx()
    val cols = (w / spacing).toInt().coerceIn(18, 44)

    // Bold Nothing Red solar circle on the right
    val sc = Offset(w * 0.86f, h * 0.30f)
    drawCircle(
        brush = Brush.radialGradient(listOf(dotRed.copy(alpha = 0.28f), Color.Transparent), center = sc, radius = 34.dp.toPx()),
        radius = 34.dp.toPx(),
        center = sc
    )
    drawCircle(color = dotRed.copy(alpha = 0.90f), radius = 6.5.dp.toPx(), center = sc)

    // Halftone rolling mountain waveform
    for (c in 0..cols) {
        val u = c.toFloat() / cols.toFloat()
        val x = u * w
        val ridgeH = h * (0.20f + 0.52f * ((sin(u * PI * 2.4 + shimmer * 0.5) + 1.0) * 0.5).toFloat())
        val rows = (ridgeH / spacing).toInt().coerceAtLeast(1)
        for (r in 0 until rows) {
            val y = h - 4.dp.toPx() - r * spacing
            val crest = r == rows - 1
            drawCircle(
                color = if (crest && c == (cols * 0.62f).toInt()) dotRed else Color.White.copy(alpha = if (crest) 0.34f else 0.12f),
                radius = if (crest) 1.45.dp.toPx() else 0.95.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

// =============================================================================
// 3. SOLAR AMBER: GOLDEN DESERT DUNES, ECLIPSE ASTROLABE & OLIVE BOTANICAL ART
// =============================================================================

private fun DrawScope.drawSolarAmberScenic(
    variant: ScenicPageVariant,
    palette: FocusThemePalette,
    drift: Float,
    intensity: Float
) {
    val w = size.width
    val h = size.height
    val gold = palette.primary
    val warmGold = Color(0xFFFCD34D)
    val terracotta = Color(0xFF9A3412)

    // Minimal sweeping golden desert-wind silk arcs + quiet twilight star glints above
    val windY = when (variant) {
        ScenicPageVariant.HOME, ScenicPageVariant.SETUP -> h * 0.185f
        ScenicPageVariant.LOCK -> h * 0.135f
        else -> h * 0.17f
    }
    val driftShift = (drift - 0.5f) * 14.dp.toPx()
    val windFlip = variant == ScenicPageVariant.SCHEDULE || variant == ScenicPageVariant.SETTINGS

    val upperWind = Path().apply {
        moveTo(w * 0.04f, windY + (if (windFlip) -8.dp.toPx() else 10.dp.toPx()))
        cubicTo(
            w * 0.34f, windY - 22.dp.toPx() + driftShift,
            w * 0.66f, windY + 18.dp.toPx() - driftShift,
            w * 0.96f, windY - (if (windFlip) -6.dp.toPx() else 10.dp.toPx())
        )
    }
    drawPath(
        path = upperWind,
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                warmGold.copy(alpha = 0.22f * intensity),
                gold.copy(alpha = 0.16f * intensity),
                Color.Transparent
            )
        ),
        style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
    )

    val secondaryWind = Path().apply {
        moveTo(w * 0.14f, windY + 18.dp.toPx())
        cubicTo(
            w * 0.44f, windY + 4.dp.toPx() - driftShift * 0.55f,
            w * 0.72f, windY + 26.dp.toPx() + driftShift * 0.55f,
            w * 0.92f, windY + 12.dp.toPx()
        )
    }
    drawPath(
        path = secondaryWind,
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                gold.copy(alpha = 0.14f * intensity),
                warmGold.copy(alpha = 0.12f * intensity),
                Color.Transparent
            )
        ),
        style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round)
    )

    // Quiet warm-gold 4-point twilight stars in the upper dusk sky
    drawMinimalStarGlint(
        center = Offset(w * (if (windFlip) 0.83f else 0.17f), h * 0.135f),
        radius = 5.4.dp.toPx(),
        color = warmGold.copy(alpha = (0.40f + 0.14f * drift) * intensity)
    )
    drawMinimalStarGlint(
        center = Offset(w * (if (windFlip) 0.73f else 0.27f), h * 0.168f),
        radius = 3.0.dp.toPx(),
        color = gold.copy(alpha = (0.30f + 0.10f * (1f - drift)) * intensity)
    )
    drawMinimalStarGlint(
        center = Offset(w * (if (windFlip) 0.20f else 0.80f), h * 0.150f),
        radius = 3.5.dp.toPx(),
        color = warmGold.copy(alpha = (0.26f + 0.10f * drift) * intensity)
    )

    // Sweeping layered desert dusk sand dunes along the lower horizon
    val duneBaseY = h * (if (variant == ScenicPageVariant.LOCK) 0.90f else 0.845f)
    val duneAmp = h * (if (variant == ScenicPageVariant.LOCK) 0.045f else 0.075f)
    val flip = variant == ScenicPageVariant.SCHEDULE || variant == ScenicPageVariant.SETTINGS

    val backDune = Path().apply {
        moveTo(0f, duneBaseY - (if (flip) duneAmp * 0.25f else duneAmp * 0.75f))
        cubicTo(
            w * 0.30f, duneBaseY - (if (flip) duneAmp * 1.10f else duneAmp * 0.10f),
            w * 0.68f, duneBaseY - (if (flip) duneAmp * 0.15f else duneAmp * 1.15f),
            w, duneBaseY - (if (flip) duneAmp * 0.80f else duneAmp * 0.30f)
        )
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = backDune,
        brush = Brush.verticalGradient(
            listOf(
                gold.copy(alpha = 0.15f * intensity),
                terracotta.copy(alpha = 0.08f * intensity),
                Color.Transparent
            ),
            startY = duneBaseY - duneAmp * 1.2f,
            endY = h
        )
    )

    val frontDuneLine = Path().apply {
        moveTo(0f, duneBaseY + 10.dp.toPx())
        cubicTo(
            w * 0.34f, duneBaseY - duneAmp * 0.55f,
            w * 0.66f, duneBaseY + duneAmp * 0.45f,
            w, duneBaseY - duneAmp * 0.32f
        )
    }
    drawPath(
        path = frontDuneLine,
        brush = Brush.horizontalGradient(
            listOf(
                gold.copy(alpha = 0.08f * intensity),
                warmGold.copy(alpha = 0.34f * intensity),
                gold.copy(alpha = 0.22f * intensity),
                Color.Transparent
            )
        ),
        style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
    )

    // Graceful minimalist golden botanical olive-frond rising from bottom corner
    if (variant != ScenicPageVariant.LOCK) {
        val onRight = !flip
        drawMinimalBotanicalStem(
            base = Offset(if (onRight) w * 0.92f else w * 0.08f, h * 0.90f),
            height = h * 0.20f,
            leanRight = !onRight,
            strokeColor = warmGold.copy(alpha = 0.24f * intensity),
            accentColor = gold.copy(alpha = 0.32f * intensity)
        )
    }
}

private fun DrawScope.drawSolarAmberVignette(palette: FocusThemePalette, shimmer: Float) {
    val w = size.width
    val h = size.height
    val gold = palette.primary
    val warmGold = Color(0xFFFCD34D)

    // Rising golden sun behind dusk dunes
    val sc = Offset(w * 0.78f, h * 0.56f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(warmGold.copy(alpha = 0.26f + 0.06f * shimmer), gold.copy(alpha = 0.10f), Color.Transparent),
            center = sc,
            radius = h * 0.95f
        ),
        radius = h * 0.95f,
        center = sc
    )
    drawCircle(
        color = warmGold.copy(alpha = 0.55f),
        radius = 14.dp.toPx(),
        center = sc,
        style = Stroke(width = 1.4.dp.toPx())
    )

    val dune = Path().apply {
        moveTo(0f, h * 0.74f)
        cubicTo(w * 0.32f, h * 0.36f, w * 0.62f, h * 0.86f, w, h * 0.48f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = dune,
        brush = Brush.verticalGradient(
            listOf(gold.copy(alpha = 0.22f), Color(0xFF9A3412).copy(alpha = 0.08f)),
            startY = h * 0.36f,
            endY = h
        )
    )
    val duneCrest = Path().apply {
        moveTo(0f, h * 0.74f)
        cubicTo(w * 0.32f, h * 0.36f, w * 0.62f, h * 0.86f, w, h * 0.48f)
    }
    drawPath(
        path = duneCrest,
        color = warmGold.copy(alpha = 0.34f),
        style = Stroke(width = 1.15.dp.toPx(), cap = StrokeCap.Round)
    )
}

/**
 * Minimal 4-point diamond star glint drawn with crisp curved concave facets
 * so upper sky accents stay luminous, sharp, and completely non-circular.
 */
private fun DrawScope.drawMinimalStarGlint(
    center: Offset,
    radius: Float,
    color: Color
) {
    val waist = radius * 0.18f
    val star = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + waist, center.y - waist, center.x + radius, center.y)
        quadraticTo(center.x + waist, center.y + waist, center.x, center.y + radius)
        quadraticTo(center.x - waist, center.y + waist, center.x - radius, center.y)
        quadraticTo(center.x - waist, center.y - waist, center.x, center.y - radius)
        close()
    }
    drawPath(path = star, color = color)
}

/**
 * Reusable ultra-minimal fine-art botanical stem with leaves anchored at the EXACT
 * cubic Bezier curve points B(t) and angled outward along the stem tangent/normal,
 * ensuring every leaf grows cleanly out of the stem and never floats or sits on top of it.
 */
private fun DrawScope.drawMinimalBotanicalStem(
    base: Offset,
    height: Float,
    leanRight: Boolean,
    strokeColor: Color,
    accentColor: Color
) {
    val dir = if (leanRight) 1f else -1f
    val p0 = base
    val p1 = Offset(base.x + dir * height * 0.05f, base.y - height * 0.36f)
    val p2 = Offset(base.x + dir * height * 0.15f, base.y - height * 0.72f)
    val p3 = Offset(base.x + dir * height * 0.27f, base.y - height)

    fun bezierPoint(t: Float): Offset {
        val u = 1f - t
        val u2 = u * u
        val u3 = u2 * u
        val t2 = t * t
        val t3 = t2 * t
        return Offset(
            x = u3 * p0.x + 3f * u2 * t * p1.x + 3f * u * t2 * p2.x + t3 * p3.x,
            y = u3 * p0.y + 3f * u2 * t * p1.y + 3f * u * t2 * p2.y + t3 * p3.y
        )
    }

    fun bezierUnitTangent(t: Float): Offset {
        val u = 1f - t
        val dx = 3f * u * u * (p1.x - p0.x) +
            6f * u * t * (p2.x - p1.x) +
            3f * t * t * (p3.x - p2.x)
        val dy = 3f * u * u * (p1.y - p0.y) +
            6f * u * t * (p2.y - p1.y) +
            3f * t * t * (p3.y - p2.y)
        val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1e-4f)
        return Offset(dx / len, dy / len)
    }

    fun drawAnchoredLeaf(
        origin: Offset,
        axisDir: Offset,
        length: Float,
        halfWidth: Float
    ) {
        val normLen = kotlin.math.hypot(axisDir.x.toDouble(), axisDir.y.toDouble()).toFloat().coerceAtLeast(1e-4f)
        val ax = axisDir.x / normLen
        val ay = axisDir.y / normLen
        // Perpendicular vector for symmetrical petal lobes
        val px = -ay
        val py = ax

        val tip = Offset(origin.x + ax * length, origin.y + ay * length)
        val ctrlA = Offset(
            origin.x + ax * (length * 0.44f) + px * halfWidth,
            origin.y + ay * (length * 0.44f) + py * halfWidth
        )
        val ctrlB = Offset(
            origin.x + ax * (length * 0.56f) - px * halfWidth,
            origin.y + ay * (length * 0.56f) - py * halfWidth
        )

        val leafPath = Path().apply {
            moveTo(origin.x, origin.y)
            quadraticTo(ctrlA.x, ctrlA.y, tip.x, tip.y)
            quadraticTo(ctrlB.x, ctrlB.y, origin.x, origin.y)
            close()
        }
        drawPath(
            path = leafPath,
            color = accentColor.copy(alpha = accentColor.alpha * 0.34f)
        )
        drawPath(
            path = leafPath,
            color = strokeColor,
            style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Draw the curved stem itself
    val stem = Path().apply {
        moveTo(p0.x, p0.y)
        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
    }
    drawPath(
        path = stem,
        color = strokeColor,
        style = Stroke(width = 1.15.dp.toPx(), cap = StrokeCap.Round)
    )

    // 4 alternating leaves sprouting strictly outward from exact curve points B(t)
    val leafFractions = listOf(0.28f, 0.48f, 0.67f, 0.85f)
    leafFractions.forEachIndexed { idx, t ->
        val anchor = bezierPoint(t)
        val tan = bezierUnitTangent(t)
        // Right-hand perpendicular normal to upward tangent (pointing roughly +x)
        val normalRight = Offset(-tan.y, tan.x)
        val side = if (idx % 2 == 0) dir else -dir
        // Leaf axis blends upward stem tangent + outward normal so it grows smoothly out of stem
        val leafAxis = Offset(
            x = tan.x * 0.52f + normalRight.x * side * 0.86f,
            y = tan.y * 0.52f + normalRight.y * side * 0.86f
        )
        val leafLen = height * (0.21f - idx * 0.022f)
        drawAnchoredLeaf(
            origin = anchor,
            axisDir = leafAxis,
            length = leafLen,
            halfWidth = leafLen * 0.28f
        )
    }

    // Delicate terminal leaf continuing straight from the exact stem tip B(1.0)
    val tipTan = bezierUnitTangent(1f)
    val tipLeafLen = height * 0.135f
    drawAnchoredLeaf(
        origin = p3,
        axisDir = tipTan,
        length = tipLeafLen,
        halfWidth = tipLeafLen * 0.26f
    )
}
