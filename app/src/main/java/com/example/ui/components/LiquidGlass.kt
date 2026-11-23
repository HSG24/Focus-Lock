package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.LocalFocusTheme

/**
 * Unified bespoke surface styling modifier across the entire app.
 *
 * Each of the 3 visual themes renders its own authentic architectural surface craft:
 * - **Liquid Glass**: Deep smoked obsidian dark glass with pearl specular bevel rim,
 *   whisper-soft top reflection sweep, corner light-glint, and spring touch refraction.
 * - **Nothing Dot**: Matte hardware charcoal card with crisp industrial hairline,
 *   3x3 micro-dot corner matrices, and a signature Nothing Red telemetry LED on
 *   elevated/highlighted panels.
 * - **Solar Amber**: Machined warm-carbon instrument panel with brushed-amber top rim
 *   and crisp architectural corner registration brackets (`┌ ┐ └ ┘`).
 */
fun Modifier.focusGlassPanel(
    shape: Shape,
    palette: FocusThemePalette? = null,
    elevated: Boolean = false,
    isPressed: Boolean = false,
    @Suppress("UNUSED_PARAMETER") shimmer: Boolean = false,
    highlightAccent: Color? = null,
    @Suppress("UNUSED_PARAMETER") borderWidth: Dp = 0.dp,
    showOrnaments: Boolean = true
): Modifier = composed {
    val activePalette = palette ?: LocalFocusTheme.current
    val pressBloom by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "surface_press_bloom"
    )

    when {
        activePalette.isLiquidGlass -> {
            val smokeBase = if (elevated) Color(0xDC11151E) else Color(0xCC0A0D13)
            val topSpecAlpha = (if (elevated) 0.095f else 0.065f) + pressBloom * 0.06f
            val midTintAlpha = (if (elevated) 0.036f else 0.024f) + pressBloom * 0.025f
            val bottomCatchAlpha = (if (elevated) 0.045f else 0.028f) + pressBloom * 0.02f

            this
                .clip(shape)
                .background(smokeBase, shape)
                .drawWithContent {
                    val w = size.width
                    val h = size.height
                    val outline = shape.createOutline(size, layoutDirection, this)

                    // 1. Deep optical dark-glass body gradient
                    drawOutline(
                        outline = outline,
                        brush = Brush.verticalGradient(
                            0.0f to (highlightAccent ?: Color(0xFFDCE7FA)).copy(alpha = topSpecAlpha),
                            0.28f to Color(0xFF9CB0CF).copy(alpha = midTintAlpha),
                            0.68f to Color(0xFF030407).copy(alpha = 0.24f),
                            1.0f to Color(0xFFAAB8CE).copy(alpha = bottomCatchAlpha)
                        )
                    )

                    // 2. Curved upper specular reflection sweep
                    val sheenH = (h * 0.30f).coerceAtLeast(1f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f + pressBloom * 0.05f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = sheenH
                        ),
                        size = Size(w, sheenH)
                    )

                    // 3. Subtle optical corner glint on cards
                    if (showOrnaments && w >= 110.dp.toPx() && h >= 52.dp.toPx()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (elevated) 0.075f else 0.045f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.90f, h * 0.08f),
                                radius = (minOf(w, h) * 0.45f).coerceAtLeast(1f)
                            ),
                            radius = (minOf(w, h) * 0.45f).coerceAtLeast(1f),
                            center = Offset(w * 0.90f, h * 0.08f)
                        )
                    }

                    // 4. Highlight accent optical aura
                    if (highlightAccent != null && w > 0f && h > 0f) {
                        drawOutline(
                            outline = outline,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    highlightAccent.copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.18f, h * 0.18f),
                                radius = maxOf(w, h) * 0.85f
                            )
                        )
                    }

                    // 5. Interactive press refraction bloom
                    if (pressBloom > 0.01f) {
                        drawOutline(
                            outline = outline,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFDCE6F8).copy(alpha = 0.12f * pressBloom),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.5f, h * 0.38f),
                                radius = (maxOf(w, h) * 0.72f).coerceAtLeast(1f)
                            )
                        )
                    }

                    drawContent()

                    // 6. Refined pearl-white specular bevel rim (drawn cleanly over edges)
                    drawOutline(
                        outline = outline,
                        brush = Brush.verticalGradient(
                            0.0f to (highlightAccent ?: Color.White).copy(
                                alpha = (if (elevated) 0.24f else 0.15f) + pressBloom * 0.12f
                            ),
                            0.35f to Color.White.copy(alpha = 0.05f),
                            0.78f to Color.Transparent,
                            1.0f to Color.White.copy(alpha = 0.08f)
                        ),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }
        }

        activePalette.isNothingDot -> {
            val fill = when {
                highlightAccent != null -> Color(0xFF180C10)
                elevated -> activePalette.surfaceElevated
                else -> activePalette.surface
            }

            this
                .clip(shape)
                .background(fill, shape)
                .drawWithContent {
                    val w = size.width
                    val h = size.height
                    val outline = shape.createOutline(size, layoutDirection, this)

                    // Subtle matte hardware depth wash
                    if (highlightAccent != null && w > 0f && h > 0f) {
                        drawOutline(
                            outline = outline,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    highlightAccent.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.92f, h * 0.12f),
                                radius = maxOf(w, h) * 0.65f
                            )
                        )
                    }

                    // Micro-dot 3x3 industrial corner matrix on cards
                    if (showOrnaments && w >= 120.dp.toPx() && h >= 58.dp.toPx()) {
                        val dotStep = 5.5.dp.toPx()
                        val dotR = 0.85.dp.toPx()
                        val pad = 11.dp.toPx()
                        val dotColor = activePalette.textPrimary.copy(alpha = if (elevated) 0.14f else 0.09f)

                        // Top-right 3x3 triangular micro-dot cluster
                        for (row in 0..2) {
                            for (col in row..2) {
                                val dx = w - pad - (2 - col) * dotStep
                                val dy = pad + row * dotStep
                                drawCircle(color = dotColor, radius = dotR, center = Offset(dx, dy))
                            }
                        }

                        // Bottom-left whisper dot cluster for taller cards
                        if (h >= 116.dp.toPx()) {
                            val softDot = activePalette.textPrimary.copy(alpha = 0.065f)
                            for (row in 0..1) {
                                for (col in 0..row) {
                                    val dx = pad + col * dotStep
                                    val dy = h - pad - (1 - row) * dotStep
                                    drawCircle(color = softDot, radius = dotR, center = Offset(dx, dy))
                                }
                            }
                        }

                        // Signature Nothing Red hardware LED indicator on elevated/highlighted panels
                        if (elevated || highlightAccent != null) {
                            val ledColor = highlightAccent ?: activePalette.primary
                            val ledCenter = Offset(w - pad, pad)
                            drawCircle(
                                color = ledColor.copy(alpha = 0.24f),
                                radius = 4.2.dp.toPx(),
                                center = ledCenter
                            )
                            drawCircle(
                                color = ledColor.copy(alpha = 0.92f),
                                radius = 1.65.dp.toPx(),
                                center = ledCenter
                            )
                        }
                    }

                    if (pressBloom > 0.01f) {
                        drawOutline(
                            outline = outline,
                            color = activePalette.primary.copy(alpha = 0.08f * pressBloom)
                        )
                    }

                    drawContent()

                    // Crisp industrial matte hairline border
                    drawOutline(
                        outline = outline,
                        color = when {
                            highlightAccent != null -> highlightAccent.copy(alpha = 0.42f)
                            elevated -> activePalette.borderHighlight.copy(alpha = 0.72f)
                            else -> activePalette.border.copy(alpha = 0.82f)
                        },
                        style = Stroke(width = 0.85.dp.toPx())
                    )
                }
        }

        else -> {
            // Solar Amber: Machined warm-carbon instrument panel with corner registration brackets
            val fill = when {
                highlightAccent != null -> Color(0xFF241B0A)
                elevated -> activePalette.surfaceElevated
                else -> activePalette.surface
            }

            this
                .clip(shape)
                .background(fill, shape)
                .drawWithContent {
                    val w = size.width
                    val h = size.height
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val accent = highlightAccent ?: activePalette.primary

                    // Subtle warm metallic top sheen inside panel
                    val topGlowH = (h * 0.28f).coerceAtLeast(1f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accent.copy(alpha = if (elevated || highlightAccent != null) 0.075f else 0.035f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = topGlowH
                        ),
                        size = Size(w, topGlowH)
                    )

                    if (pressBloom > 0.01f) {
                        drawOutline(
                            outline = outline,
                            color = accent.copy(alpha = 0.09f * pressBloom)
                        )
                    }

                    drawContent()

                    // Recessed warm carbon hairline outline
                    drawOutline(
                        outline = outline,
                        brush = Brush.verticalGradient(
                            0.0f to accent.copy(alpha = if (elevated || highlightAccent != null) 0.36f else 0.20f),
                            0.25f to activePalette.borderHighlight.copy(alpha = 0.55f),
                            1.0f to activePalette.border.copy(alpha = 0.75f)
                        ),
                        style = Stroke(width = 0.85.dp.toPx())
                    )

                    // Crisp machined corner registration brackets (┌ ┐ └ ┘) on cards
                    if (showOrnaments && w >= 120.dp.toPx() && h >= 58.dp.toPx()) {
                        val inset = 7.dp.toPx()
                        val arm = if (h >= 100.dp.toPx()) 7.dp.toPx() else 5.dp.toPx()
                        val bracketStroke = 1.dp.toPx()
                        val bracketColor = accent.copy(
                            alpha = if (elevated || highlightAccent != null) 0.48f else 0.26f
                        )

                        // Top-left ┌
                        drawLine(bracketColor, Offset(inset, inset), Offset(inset + arm, inset), bracketStroke, StrokeCap.Square)
                        drawLine(bracketColor, Offset(inset, inset), Offset(inset, inset + arm), bracketStroke, StrokeCap.Square)
                        // Top-right ┐
                        drawLine(bracketColor, Offset(w - inset - arm, inset), Offset(w - inset, inset), bracketStroke, StrokeCap.Square)
                        drawLine(bracketColor, Offset(w - inset, inset), Offset(w - inset, inset + arm), bracketStroke, StrokeCap.Square)
                        // Bottom-left └
                        drawLine(bracketColor, Offset(inset, h - inset - arm), Offset(inset, h - inset), bracketStroke, StrokeCap.Square)
                        drawLine(bracketColor, Offset(inset, h - inset), Offset(inset + arm, h - inset), bracketStroke, StrokeCap.Square)
                        // Bottom-right ┘
                        drawLine(bracketColor, Offset(w - inset, h - inset - arm), Offset(w - inset, h - inset), bracketStroke, StrokeCap.Square)
                        drawLine(bracketColor, Offset(w - inset - arm, h - inset), Offset(w - inset, h - inset), bracketStroke, StrokeCap.Square)
                    }
                }
        }
    }
}

/**
 * Static highlight factor helper retained for API compatibility.
 */
@Composable
fun rememberLiquidGlassPulse(@Suppress("UNUSED_PARAMETER") enabled: Boolean): Float = 0f
