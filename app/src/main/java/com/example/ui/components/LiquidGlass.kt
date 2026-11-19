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
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.LocalFocusTheme

/**
 * Unified borderless surface styling modifier across the entire app.
 *
 * When [FocusThemePalette.isLiquidGlass] is active, every panel, stat card, pill,
 * control container, and keypad key renders as deep smoked obsidian dark glass
 * with static specular sheen and interactive spring-animated press refraction
 * only when touched — zero idle/live animations and zero harsh borders.
 *
 * For all other themes (Nothing Crimson, Solar Amber, Nordic Sage), it renders
 * a clean, borderless tonal surface that floats softly over the canvas.
 */
fun Modifier.focusGlassPanel(
    shape: Shape,
    palette: FocusThemePalette? = null,
    elevated: Boolean = false,
    isPressed: Boolean = false,
    @Suppress("UNUSED_PARAMETER") shimmer: Boolean = false,
    highlightAccent: Color? = null,
    @Suppress("UNUSED_PARAMETER") borderWidth: Dp = 0.dp
): Modifier = composed {
    val activePalette = palette ?: LocalFocusTheme.current

    if (!activePalette.isLiquidGlass) {
        val fill = if (highlightAccent != null) {
            highlightAccent.copy(alpha = 0.14f)
        } else if (elevated) {
            activePalette.surfaceElevated
        } else {
            activePalette.surface
        }
        this
            .clip(shape)
            .background(fill, shape)
    } else {
        val pressBloom by animateFloatAsState(
            targetValue = if (isPressed) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "liquid_glass_press_bloom"
        )

        // Deep smoked obsidian glass base (static dark glass, zero live idle animation)
        val smokeBase = if (elevated) Color(0xDC11151E) else Color(0xCC0A0D13)
        val topSpecAlpha = (if (elevated) 0.085f else 0.06f) + pressBloom * 0.06f
        val midTintAlpha = (if (elevated) 0.032f else 0.022f) + pressBloom * 0.025f
        val bottomCatchAlpha = (if (elevated) 0.04f else 0.025f) + pressBloom * 0.02f

        this
            .clip(shape)
            .background(smokeBase, shape)
            .drawWithContent {
                val w = size.width
                val h = size.height
                val outline = shape.createOutline(size, layoutDirection, this)

                // 1. Deep dark-glass optical body gradient
                drawOutline(
                    outline = outline,
                    brush = Brush.verticalGradient(
                        0.0f to (highlightAccent ?: Color(0xFFD8E4F8)).copy(alpha = topSpecAlpha),
                        0.28f to Color(0xFF9CB0CF).copy(alpha = midTintAlpha),
                        0.68f to Color(0xFF030407).copy(alpha = 0.24f),
                        1.0f to Color(0xFFAAB8CE).copy(alpha = bottomCatchAlpha)
                    )
                )

                // 2. Whisper-soft curved upper specular sheen (no outer border stroke)
                val sheenH = (h * 0.30f).coerceAtLeast(1f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.055f + pressBloom * 0.05f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = sheenH
                    ),
                    size = Size(w, sheenH)
                )

                // 3. Subtle accent aura if highlighted (borderless)
                if (highlightAccent != null && w > 0f && h > 0f) {
                    drawOutline(
                        outline = outline,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                highlightAccent.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.18f, h * 0.18f),
                            radius = maxOf(w, h) * 0.85f
                        )
                    )
                }

                // 4. Interactive touch-press refraction bloom (event-driven only)
                if (pressBloom > 0.01f) {
                    drawOutline(
                        outline = outline,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFDCE6F8).copy(alpha = 0.11f * pressBloom),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.38f),
                            radius = (maxOf(w, h) * 0.72f).coerceAtLeast(1f)
                        )
                    )
                }

                drawContent()
            }
    }
}

/**
 * Static highlight factor (zero continuous live animations).
 */
@Composable
fun rememberLiquidGlassPulse(@Suppress("UNUSED_PARAMETER") enabled: Boolean): Float = 0f
