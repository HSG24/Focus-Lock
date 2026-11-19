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
            background = Color(0xFF040406),
            surface = Color(0xFF0E0E12),
            surfaceElevated = Color(0xFF16161C),
            pillGlassFill = Color(0xE8101014),
            border = Color(0xFF1F1F27),
            borderHighlight = Color(0xFF2E2E3A),
            primary = Color(0xFFFF242E),
            primarySecondary = Color(0xFFD71921),
            primaryDeep = Color(0xFF9E1117),
            primaryGlow = Color(0xFF2C0609),
            primaryContainer = Color(0xFF23080C),
            primaryBorderTint = Color(0x45FF242E),
            onPrimary = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFF7F7FA),
            textSecondary = Color(0xFF8B8B99),
            success = Color(0xFF14C88D),
            warning = Color(0xFFF5A623),
            displayFont = DotGothic16,
            numeralFont = DotGothic16,
            bodyFont = PlusJakartaSans,
            useDottedDividers = true
        )

        val CyberIce = FocusThemePalette(
            theme = AppVisualTheme.CYBER_ICE,
            background = Color(0xFF04080E),
            surface = Color(0xFF0B121D),
            surfaceElevated = Color(0xFF121D2D),
            pillGlassFill = Color(0xE80A111B),
            border = Color(0xFF18273B),
            borderHighlight = Color(0xFF263E5C),
            primary = Color(0xFF00E5FF),
            primarySecondary = Color(0xFF00B8D4),
            primaryDeep = Color(0xFF00768C),
            primaryGlow = Color(0xFF042230),
            primaryContainer = Color(0xFF072232),
            primaryBorderTint = Color(0x4500E5FF),
            onPrimary = Color(0xFF021119),
            textPrimary = Color(0xFFF2F9FF),
            textSecondary = Color(0xFF7D9BBA),
            success = Color(0xFF22E5A8),
            warning = Color(0xFFFFB020),
            displayFont = Orbitron,
            numeralFont = Orbitron,
            bodyFont = PlusJakartaSans,
            useHudGridBackdrop = true
        )

        val SolarAmber = FocusThemePalette(
            theme = AppVisualTheme.SOLAR_AMBER,
            background = Color(0xFF080705),
            surface = Color(0xFF13110D),
            surfaceElevated = Color(0xFF1D1913),
            pillGlassFill = Color(0xE813110D),
            border = Color(0xFF29241B),
            borderHighlight = Color(0xFF3E3629),
            primary = Color(0xFFFFB71B),
            primarySecondary = Color(0xFFFF9100),
            primaryDeep = Color(0xFF946100),
            primaryGlow = Color(0xFF291B04),
            primaryContainer = Color(0xFF291D07),
            primaryBorderTint = Color(0x45FFB71B),
            onPrimary = Color(0xFF181000),
            textPrimary = Color(0xFFFFFBF2),
            textSecondary = Color(0xFF9E927E),
            success = Color(0xFF22C55E),
            warning = Color(0xFFFFB71B),
            displayFont = ChakraPetch,
            numeralFont = ChakraPetch,
            bodyFont = PlusJakartaSans,
            useChronographTicks = true
        )

        val UltravioletPulse = FocusThemePalette(
            theme = AppVisualTheme.ULTRAVIOLET_PULSE,
            background = Color(0xFF06050B),
            surface = Color(0xFF100D18),
            surfaceElevated = Color(0xFF181425),
            pillGlassFill = Color(0xE8100D18),
            border = Color(0xFF241D36),
            borderHighlight = Color(0xFF372C52),
            primary = Color(0xFFB78EFF),
            primarySecondary = Color(0xFF8354FF),
            primaryDeep = Color(0xFF532EB8),
            primaryGlow = Color(0xFF1D1138),
            primaryContainer = Color(0xFF221540),
            primaryBorderTint = Color(0x45B78EFF),
            onPrimary = Color(0xFF0E0620),
            textPrimary = Color(0xFFF6F3FF),
            textSecondary = Color(0xFF988BB8),
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
