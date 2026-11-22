package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuizInputMode
import com.example.ui.theme.FocusThemePalette
import com.example.ui.theme.JetBrainsMono

/**
 * Tactile adaptive built-in keypad for the 10-question unlock challenge.
 *
 * Automatically adapts between a precision 3x4 numeric pad (for math/reflex
 * challenges) and a minimalist QWERTY A-Z keypad (for word/letter challenges)
 * directly inside the overlay so the system keyboard never disrupts the UI.
 */
@Composable
internal fun QuizAdaptiveKeypad(
    inputMode: QuizInputMode,
    canSubmit: Boolean,
    hapticEnabled: Boolean,
    palette: FocusThemePalette,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberFocusHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quiz_adaptive_keypad"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedContent(
            targetState = inputMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
            },
            label = "quiz_keypad_mode"
        ) { mode ->
            when (mode) {
                QuizInputMode.NUMERIC -> {
                    NumericQuizPad(
                        palette = palette,
                        hapticEnabled = hapticEnabled,
                        onDigit = { digit ->
                            if (hapticEnabled) haptics.tap()
                            onChar(digit)
                        },
                        onClear = {
                            if (hapticEnabled) haptics.tick()
                            onClear()
                        },
                        onBackspace = {
                            if (hapticEnabled) haptics.tick()
                            onBackspace()
                        }
                    )
                }

                QuizInputMode.ALPHA -> {
                    AlphaQuizPad(
                        palette = palette,
                        hapticEnabled = hapticEnabled,
                        onLetter = { letter ->
                            if (hapticEnabled) haptics.tap()
                            onChar(letter)
                        },
                        onClear = {
                            if (hapticEnabled) haptics.tick()
                            onClear()
                        },
                        onBackspace = {
                            if (hapticEnabled) haptics.tick()
                            onBackspace()
                        }
                    )
                }
            }
        }

        // Primary Submit / Verify pill button
        val submitInteraction = remember { MutableInteractionSource() }
        val submitScale = rememberSpringPressScale(submitInteraction)
        Button(
            onClick = {
                if (canSubmit) {
                    if (hapticEnabled) haptics.confirm()
                    onSubmit()
                }
            },
            enabled = canSubmit,
            interactionSource = submitInteraction,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.primary,
                contentColor = palette.onPrimary,
                disabledContainerColor = palette.surfaceElevated.copy(alpha = 0.65f),
                disabledContentColor = palette.textSecondary.copy(alpha = 0.45f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer {
                    scaleX = submitScale
                    scaleY = submitScale
                }
                .testTag("quiz_submit_button")
        ) {
            Text(
                text = "Submit Answer",
                fontFamily = palette.bodyFont,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun NumericQuizPad(
    palette: FocusThemePalette,
    hapticEnabled: Boolean,
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit
) {
    val rows = remember {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_numeric_pad"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { digit ->
                    KeypadTile(
                        label = digit,
                        palette = palette,
                        height = 52.dp,
                        cornerRadius = 18.dp,
                        fontSizeSp = 21f,
                        useNumeralFont = true,
                        onClick = { onDigit(digit) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quiz_key_$digit")
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadActionTile(
                icon = Icons.Outlined.Clear,
                contentDescription = "Clear answer",
                label = "CLR",
                palette = palette,
                height = 52.dp,
                cornerRadius = 18.dp,
                onClick = onClear,
                modifier = Modifier
                    .weight(1f)
                    .testTag("quiz_key_clear")
            )
            KeypadTile(
                label = "0",
                palette = palette,
                height = 52.dp,
                cornerRadius = 18.dp,
                fontSizeSp = 21f,
                useNumeralFont = true,
                onClick = { onDigit("0") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quiz_key_0")
            )
            KeypadActionTile(
                icon = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = "Backspace",
                label = null,
                palette = palette,
                height = 52.dp,
                cornerRadius = 18.dp,
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1f)
                    .testTag("quiz_key_backspace")
            )
        }
    }
}

@Composable
private fun AlphaQuizPad(
    palette: FocusThemePalette,
    hapticEnabled: Boolean,
    onLetter: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit
) {
    val row1 = remember { listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P") }
    val row2 = remember { listOf("A", "S", "D", "F", "G", "H", "J", "K", "L") }
    val row3 = remember { listOf("Z", "X", "C", "V", "B", "N", "M") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quiz_alpha_pad"),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Row 1: 10 keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            row1.forEach { key ->
                KeypadTile(
                    label = key,
                    palette = palette,
                    height = 48.dp,
                    cornerRadius = 12.dp,
                    fontSizeSp = 15.5f,
                    useNumeralFont = false,
                    onClick = { onLetter(key) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quiz_key_$key")
                )
            }
        }

        // Row 2: 9 keys centered with half-key margins
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            row2.forEach { key ->
                KeypadTile(
                    label = key,
                    palette = palette,
                    height = 48.dp,
                    cornerRadius = 12.dp,
                    fontSizeSp = 15.5f,
                    useNumeralFont = false,
                    onClick = { onLetter(key) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quiz_key_$key")
                )
            }
        }

        // Row 3: Clear + 7 keys + Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            KeypadActionTile(
                icon = Icons.Outlined.Clear,
                contentDescription = "Clear answer",
                label = null,
                palette = palette,
                height = 48.dp,
                cornerRadius = 12.dp,
                onClick = onClear,
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("quiz_key_clear")
            )
            row3.forEach { key ->
                KeypadTile(
                    label = key,
                    palette = palette,
                    height = 48.dp,
                    cornerRadius = 12.dp,
                    fontSizeSp = 15.5f,
                    useNumeralFont = false,
                    onClick = { onLetter(key) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quiz_key_$key")
                )
            }
            KeypadActionTile(
                icon = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = "Backspace",
                label = null,
                palette = palette,
                height = 48.dp,
                cornerRadius = 12.dp,
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("quiz_key_backspace")
            )
        }
    }
}

@Composable
private fun KeypadTile(
    label: String,
    palette: FocusThemePalette,
    height: Dp,
    cornerRadius: Dp,
    fontSizeSp: Float,
    useNumeralFont: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.92f)
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val fill = if (palette.isLiquidGlass) {
        Color(0x88131722)
    } else {
        palette.surfaceElevated
    }
    val stroke = palette.borderHighlight.copy(alpha = if (palette.isLiquidGlass) 0.28f else 0.60f)

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(fill, shape)
            .border(width = 0.75.dp, color = stroke, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = if (useNumeralFont) palette.numeralFont else palette.bodyFont,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun KeypadActionTile(
    icon: ImageVector,
    contentDescription: String,
    label: String?,
    palette: FocusThemePalette,
    height: Dp,
    cornerRadius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberSpringPressScale(interactionSource, pressedScale = 0.92f)
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val fill = if (palette.isLiquidGlass) {
        Color(0x660B0E16)
    } else {
        palette.surface
    }
    val stroke = palette.border.copy(alpha = if (palette.isLiquidGlass) 0.30f else 0.72f)

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(fill, shape)
            .border(width = 0.75.dp, color = stroke, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = palette.textSecondary
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = palette.textSecondary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
