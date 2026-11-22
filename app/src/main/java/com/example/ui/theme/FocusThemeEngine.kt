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
        subtitle = "Deep obsidian dark glass with subtle specular depth",
        badgeTag = "GLASS"
    ),
    NOTHING_CRIMSON(
        id = "nothing_crimson",
        displayName = "Nothing Crimson",
        subtitle = "Dot-matrix pitch black & Nothing red",
        badgeTag = "DOT MATRIX"
    ),
    SOLAR_AMBER(
        id = "solar_amber",
        displayName = "Solar Amber",
        subtitle = "Warm carbon & tactical amber chronograph",
        badgeTag = "CHRONO"
    ),
    NORDIC_SAGE(
        id = "nordic_sage",
        displayName = "Nordic Sage",
        subtitle = "Basalt stone charcoal & muted botanical sage",
        badgeTag = "ORGANIC"
    );

    companion object {
        fun fromIdOrName(raw: String?): AppVisualTheme {
            if (raw.isNullOrBlank()) return LIQUID_GLASS
            return entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) || it.id.equals(raw, ignoreCase = true)
            } ?: LIQUID_GLASS
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
    val useChronographTicks: Boolean = false,
    val useOrganicHorizon: Boolean = false
) {
    companion object {
        val NothingCrimson = FocusThemePalette(
            theme = AppVisualTheme.NOTHING_CRIMSON,
            background = Color(0xFF040406),
            surface = Color(0xFF111116),
            surfaceElevated = Color(0xFF1B1B23),
            pillGlassFill = Color(0xEB121218),
            border = Color(0xFF23232D),
            borderHighlight = Color(0xFF333342),
            primary = Color(0xFFFF2E38),
            primarySecondary = Color(0xFFFF5A62),
            primaryDeep = Color(0xFF9E1117),
            primaryGlow = Color(0xFF2C0609),
            primaryContainer = Color(0xFF280A0F),
            primaryBorderTint = Color(0x45FF2E38),
            onPrimary = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFFAFAFD),
            textSecondary = Color(0xFFA2A2B2),
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
            borderHighlight = Color(0x38FFFFFF),
            primary = Color(0xFFCFD8E6),
            primarySecondary = Color(0xFF93A0B4),
            primaryDeep = Color(0xFF475266),
            primaryGlow = Color(0xFF141924),
            primaryContainer = Color(0xFF19202D),
            primaryBorderTint = Color(0x33CFD8E6),
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
            surface = Color(0xFF14120D),
            surfaceElevated = Color(0xFF201C14),
            pillGlassFill = Color(0xEB15120D),
            border = Color(0xFF2C261C),
            borderHighlight = Color(0xFF42392B),
            primary = Color(0xFFFFB71B),
            primarySecondary = Color(0xFFFF9E1A),
            primaryDeep = Color(0xFF946100),
            primaryGlow = Color(0xFF291B04),
            primaryContainer = Color(0xFF2B1F08),
            primaryBorderTint = Color(0x45FFB71B),
            onPrimary = Color(0xFF140D00),
            textPrimary = Color(0xFFFFFBF2),
            textSecondary = Color(0xFFB3A690),
            success = Color(0xFF2ED47A),
            warning = Color(0xFFFFC13B),
            displayFont = ChakraPetch,
            numeralFont = ChakraPetch,
            bodyFont = PlusJakartaSans,
            useChronographTicks = true
        )

        val NordicSage = FocusThemePalette(
            theme = AppVisualTheme.NORDIC_SAGE,
            background = Color(0xFF070908),
            surface = Color(0xFF121714),
            surfaceElevated = Color(0xFF1C2420),
            pillGlassFill = Color(0xEB131815),
            border = Color(0xFF242E28),
            borderHighlight = Color(0xFF36463E),
            primary = Color(0xFF8EC2A1),
            primarySecondary = Color(0xFF6EA381),
            primaryDeep = Color(0xFF3E634D),
            primaryGlow = Color(0xFF1A2D22),
            primaryContainer = Color(0xFF17271E),
            primaryBorderTint = Color(0x4A8EC2A1),
            onPrimary = Color(0xFF07130B),
            textPrimary = Color(0xFFF4F7F4),
            textSecondary = Color(0xFF9EADA4),
            success = Color(0xFF7CD4A2),
            warning = Color(0xFFE9C380),
            displayFont = PlusJakartaSans,
            numeralFont = JetBrainsMono,
            bodyFont = PlusJakartaSans,
            useOrganicHorizon = true
        )

        fun from(theme: AppVisualTheme): FocusThemePalette = when (theme) {
            AppVisualTheme.NOTHING_CRIMSON -> NothingCrimson
            AppVisualTheme.LIQUID_GLASS -> LiquidGlass
            AppVisualTheme.SOLAR_AMBER -> SolarAmber
            AppVisualTheme.NORDIC_SAGE -> NordicSage
        }
    }
}

val LocalFocusTheme = staticCompositionLocalOf { FocusThemePalette.LiquidGlass }
