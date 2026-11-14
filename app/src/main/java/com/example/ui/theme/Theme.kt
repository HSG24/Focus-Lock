package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OledDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = PureWhite,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = PureWhite,
    secondary = SlateGray,
    onSecondary = OledBlack,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = PureWhite,
    tertiary = CrimsonDeep,
    onTertiary = PureWhite,
    tertiaryContainer = CrimsonGlow,
    onTertiaryContainer = PureWhite,
    background = OledBlack,
    onBackground = PureWhite,
    surface = ObsidianSurface,
    onSurface = PureWhite,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = SlateGray,
    surfaceContainerLowest = OledBlack,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianElevated,
    surfaceContainerHigh = ObsidianElevated,
    surfaceContainerHighest = ObsidianBorder,
    outline = ObsidianOutline,
    outlineVariant = ObsidianBorder,
    error = CrimsonPrimary,
    onError = PureWhite,
    errorContainer = CrimsonContainer,
    onErrorContainer = PureWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OledDarkColorScheme,
        typography = Typography,
        content = content
    )
}
