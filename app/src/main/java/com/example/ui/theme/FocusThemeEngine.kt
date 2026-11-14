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
    NOTHING_CRIMSON(
        id = "nothing_crimson",
        displayName = "Nothing Crimson",
        subtitle = "Dot-matrix pitch black & Nothing red",
        badgeTag = "DOT MATRIX"
    ),
    CYBER_ICE(
        id = "cyber_ice",
        displayName = "Cyber Ice",
        subtitle = "Abyssal navy, slate glass & glacier cyan HUD",
        badgeTag = "HUD GRID"
    ),
    SOLAR_AMBER(
        id = "solar_amber",
        displayName = "Solar Amber",
        subtitle = "Warm carbon & tactical amber chronograph",
        badgeTag = "CHRONO"
    ),
    ULTRAVIOLET_PULSE(
        id = "ultraviolet_pulse",
        displayName = "Ultraviolet Pulse",
        subtitle = "Void obsidian & electric neon violet aura",
        badgeTag = "NEON AURA"
    );

    companion object {
        fun fromIdOrName(raw: String?): AppVisualTheme {
            if (raw.isNullOrBlank()) return NOTHING_CRIMSON
            return entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) || it.id.equals(raw, ignoreCase = true)
            } ?: NOTHING_CRIMSON
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
    val displayFont: FontFamily,
    val numeralFont: FontFamily,
    val bodyFont: FontFamily,
    val useDottedDividers: Boolean = false,
    val useHudGridBackdrop: Boolean = false,
    val useChronographTicks: Boolean = false,
    val useRadialAuraRings: Boolean = false
) {
    companion object {
        val NothingCrimson = FocusThemePalette(
            theme = AppVisualTheme.NOTHING_CRIMSON,
            background = Color(0xFF000000),
            surface = Color(0xFF141416),
            surfaceElevated = Color(0xFF1B1B1D),
            pillGlassFill = Color(0xEB141416),
            border = Color(0xFF28282C),
            borderHighlight = Color(0xFF3A3A40),
            primary = Color(0xFFFF1E27),
            primarySecondary = Color(0xFFD71921),
            primaryDeep = Color(0xFF990F15),
            primaryGlow = Color(0xFF360508),
            primaryContainer = Color(0xFF2A070B),
            primaryBorderTint = Color(0x66D71921),
            onPrimary = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF9A9AA3),
            success = Color(0xFF10B981),
            warning = Color(0xFFF59E0B),
            displayFont = DotGothic16,
            numeralFont = DotGothic16,
            bodyFont = PlusJakartaSans,
            useDottedDividers = true
        )

        val CyberIce = FocusThemePalette(
            theme = AppVisualTheme.CYBER_ICE,
            background = Color(0xFF03070C),
            surface = Color(0xFF0B131E),
            surfaceElevated = Color(0xFF111D2E),
            pillGlassFill = Color(0xEB0B131E),
            border = Color(0xFF1B2E46),
            borderHighlight = Color(0xFF29466B),
            primary = Color(0xFF00E5FF),
            primarySecondary = Color(0xFF00B8D4),
            primaryDeep = Color(0xFF006D82),
            primaryGlow = Color(0xFF022330),
            primaryContainer = Color(0xFF062333),
            primaryBorderTint = Color(0x6600E5FF),
            onPrimary = Color(0xFF021018),
            textPrimary = Color(0xFFF0F9FF),
            textSecondary = Color(0xFF84A4C4),
            success = Color(0xFF22E5A8),
            warning = Color(0xFFFFB020),
            displayFont = Orbitron,
            numeralFont = Orbitron,
            bodyFont = PlusJakartaSans,
            useHudGridBackdrop = true
        )

        val SolarAmber = FocusThemePalette(
            theme = AppVisualTheme.SOLAR_AMBER,
            background = Color(0xFF090806),
            surface = Color(0xFF161410),
            surfaceElevated = Color(0xFF211D17),
            pillGlassFill = Color(0xEB161410),
            border = Color(0xFF332C22),
            borderHighlight = Color(0xFF473E2F),
            primary = Color(0xFFFFB300),
            primarySecondary = Color(0xFFFF9100),
            primaryDeep = Color(0xFF8C5B00),
            primaryGlow = Color(0xFF2B1C03),
            primaryContainer = Color(0xFF2E2108),
            primaryBorderTint = Color(0x66FFB300),
            onPrimary = Color(0xFF1A1200),
            textPrimary = Color(0xFFFFFAF0),
            textSecondary = Color(0xFFA89C87),
            success = Color(0xFF22C55E),
            warning = Color(0xFFFFB300),
            displayFont = ChakraPetch,
            numeralFont = ChakraPetch,
            bodyFont = PlusJakartaSans,
            useChronographTicks = true
        )

        val UltravioletPulse = FocusThemePalette(
            theme = AppVisualTheme.ULTRAVIOLET_PULSE,
            background = Color(0xFF06040A),
            surface = Color(0xFF120E1A),
            surfaceElevated = Color(0xFF1B1528),
            pillGlassFill = Color(0xEB120E1A),
            border = Color(0xFF2B2240),
            borderHighlight = Color(0xFF3F325E),
            primary = Color(0xFFB388FF),
            primarySecondary = Color(0xFF7C4DFF),
            primaryDeep = Color(0xFF4A25A8),
            primaryGlow = Color(0xFF1E103D),
            primaryContainer = Color(0xFF241645),
            primaryBorderTint = Color(0x66B388FF),
            onPrimary = Color(0xFF0F0621),
            textPrimary = Color(0xFFF6F2FF),
            textSecondary = Color(0xFF9F92BD),
            success = Color(0xFF34D399),
            warning = Color(0xFFFBBF24),
            displayFont = PlusJakartaSans,
            numeralFont = JetBrainsMono,
            bodyFont = PlusJakartaSans,
            useRadialAuraRings = true
        )

        fun from(theme: AppVisualTheme): FocusThemePalette = when (theme) {
            AppVisualTheme.NOTHING_CRIMSON -> NothingCrimson
            AppVisualTheme.CYBER_ICE -> CyberIce
            AppVisualTheme.SOLAR_AMBER -> SolarAmber
            AppVisualTheme.ULTRAVIOLET_PULSE -> UltravioletPulse
        }
    }
}

val LocalFocusTheme = staticCompositionLocalOf { FocusThemePalette.NothingCrimson }
