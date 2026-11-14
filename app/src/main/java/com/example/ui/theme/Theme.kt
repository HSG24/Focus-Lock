package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@Composable
fun MyApplicationTheme(
    appTheme: AppVisualTheme = AppVisualTheme.NOTHING_CRIMSON,
    content: @Composable () -> Unit
) {
    val palette = remember(appTheme) { FocusThemePalette.from(appTheme) }
    val colorScheme = remember(palette) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.textSecondary,
            onSecondary = palette.background,
            secondaryContainer = palette.surfaceElevated,
            onSecondaryContainer = palette.textPrimary,
            tertiary = palette.primarySecondary,
            onTertiary = palette.onPrimary,
            tertiaryContainer = palette.primaryGlow,
            onTertiaryContainer = palette.textPrimary,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            surfaceContainerLowest = palette.background,
            surfaceContainerLow = palette.surface,
            surfaceContainer = palette.surfaceElevated,
            surfaceContainerHigh = palette.surfaceElevated,
            surfaceContainerHighest = palette.border,
            outline = palette.borderHighlight,
            outlineVariant = palette.border,
            error = ColorErrorCrimson,
            onError = PureWhite,
            errorContainer = ColorErrorContainer,
            onErrorContainer = PureWhite
        )
    }
    val themeTypography = remember(palette.displayFont, palette.bodyFont) {
        typographyForTheme(
            displayFont = palette.displayFont,
            bodyFont = palette.bodyFont
        )
    }

    CompositionLocalProvider(LocalFocusTheme provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = themeTypography,
            content = content
        )
    }
}

private val ColorErrorCrimson = androidx.compose.ui.graphics.Color(0xFFFF4D57)
private val ColorErrorContainer = androidx.compose.ui.graphics.Color(0xFF3B090E)
