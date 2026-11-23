package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class AppVisualTheme(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val badgeTag: String
) {
    LIQUID_GLASS(
        id = "liquid_glass",
        displayName = "Liquid Glass",
        subtitle = "Smoked obsidian glass & soft specular light",
        badgeTag = "GLASS"
    ),
    NOTHING_DOT(
        id = "nothing_dot",
        displayName = "Nothing Dot",
        subtitle = "Industrial dot-matrix & Nothing red",
        badgeTag = "DOT MATRIX"
    ),
    SOLAR_AMBER(
        id = "solar_amber",
        displayName = "Solar Amber",
        subtitle = "Warm carbon & precision amber chronograph",
        badgeTag = "CHRONO"
    );

    companion object {
        fun fromIdOrName(raw: String?): AppVisualTheme {
            if (raw.isNullOrBlank()) return LIQUID_GLASS
            val normalized = raw.trim().lowercase()
            return when (normalized) {
                "nothing_crimson", "nothingcrimson" -> NOTHING_DOT
                "nordic_sage", "nordicsage" -> LIQUID_GLASS
                else -> entries.firstOrNull {
                    it.name.equals(normalized, ignoreCase = true) ||
                        it.id.equals(normalized, ignoreCase = true)
                } ?: LIQUID_GLASS
            }
        }
    }
}

@Immutable
data class FocusThemePalette(
    val theme: AppVisualTheme,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val pillGlassFill: Color,
    val border: Color,
    val borderHighlight: Color,
    val primary: Color,
    val primarySecondary: Color,
    val primaryDeep: Color,
    val primaryGlow: Color,
    val primaryContainer: Color,
    val primaryBorderTint: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val warning: Color,
    val error: Color = Color(0xFFFF4D57),
    val displayFont: FontFamily,
    val numeralFont: FontFamily,
    val bodyFont: FontFamily,
    val useDottedDividers: Boolean = false,
    val isLiquidGlass: Boolean = false,
    val useChronographTicks: Boolean = false
) {
    val isNothingDot: Boolean get() = theme == AppVisualTheme.NOTHING_DOT
    val isSolarAmber: Boolean get() = theme == AppVisualTheme.SOLAR_AMBER

    companion object {
        val NothingDot = FocusThemePalette(
            theme = AppVisualTheme.NOTHING_DOT,
            background = Color(0xFF040406),
            surface = Color(0xFF0F0F14),
            surfaceElevated = Color(0xFF17171F),
            pillGlassFill = Color(0xF0101016),
            border = Color(0xFF23232E),
            borderHighlight = Color(0xFF363646),
            primary = Color(0xFFFF2E38),
            primarySecondary = Color(0xFFFF5A62),
            primaryDeep = Color(0xFF9E1117),
            primaryGlow = Color(0xFF2C0609),
            primaryContainer = Color(0xFF26090E),
            primaryBorderTint = Color(0x4DFF2E38),
            onPrimary = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFFAFAFD),
            textSecondary = Color(0xFFA0A0B0),
            success = Color(0xFF22D399),
            warning = Color(0xFFF6B13A),
            displayFont = DotGothic16,
            numeralFont = DotGothic16,
            bodyFont = PlusJakartaSans,
            useDottedDividers = true
        )

        val LiquidGlass = FocusThemePalette(
            theme = AppVisualTheme.LIQUID_GLASS,
            background = Color(0xFF020305),
            surface = Color(0xFF0C0F15),
            surfaceElevated = Color(0xFF161B25),
            pillGlassFill = Color(0xE80C0F15),
            border = Color(0x1EFFFFFF),
            borderHighlight = Color(0x3CFFFFFF),
            primary = Color(0xFFD5DFEE),
            primarySecondary = Color(0xFF93A0B4),
            primaryDeep = Color(0xFF475266),
            primaryGlow = Color(0xFF141924),
            primaryContainer = Color(0xFF19202D),
            primaryBorderTint = Color(0x38CFD8E6),
            onPrimary = Color(0xFF07090D),
            textPrimary = Color(0xFFF2F5FA),
            textSecondary = Color(0xFF9EA8BA),
            success = Color(0xFF4EE0B5),
            warning = Color(0xFFF0C674),
            displayFont = PlusJakartaSans,
            numeralFont = JetBrainsMono,
            bodyFont = PlusJakartaSans,
            isLiquidGlass = true
        )

        val SolarAmber = FocusThemePalette(
            theme = AppVisualTheme.SOLAR_AMBER,
            background = Color(0xFF070604),
            surface = Color(0xFF13110C),
            surfaceElevated = Color(0xFF1E1A12),
            pillGlassFill = Color(0xEE14110C),
            border = Color(0xFF2D271C),
            borderHighlight = Color(0xFF473D2C),
            primary = Color(0xFFFFB71B),
            primarySecondary = Color(0xFFFF961A),
            primaryDeep = Color(0xFF946100),
            primaryGlow = Color(0xFF291B04),
            primaryContainer = Color(0xFF2A1E08),
            primaryBorderTint = Color(0x4DFFB71B),
            onPrimary = Color(0xFF140D00),
            textPrimary = Color(0xFFFFFBF2),
            textSecondary = Color(0xFFB5A790),
            success = Color(0xFF2ED47A),
            warning = Color(0xFFFFC13B),
            displayFont = ChakraPetch,
            numeralFont = ChakraPetch,
            bodyFont = PlusJakartaSans,
            useChronographTicks = true
        )

        fun from(theme: AppVisualTheme): FocusThemePalette = when (theme) {
            AppVisualTheme.NOTHING_DOT -> NothingDot
            AppVisualTheme.LIQUID_GLASS -> LiquidGlass
            AppVisualTheme.SOLAR_AMBER -> SolarAmber
        }
    }
}

val LocalFocusTheme = staticCompositionLocalOf { FocusThemePalette.LiquidGlass }
