package com.sherif.ledger.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Ledger V3 Color Palette (Machined Financial Instrument)
 * 
 * Retiring Emerald identity. Moving to semantic, architectural colors.
 */
object LedgerV3Palette {
    // Light Core
    val Paper = Color(0xFFFFFFFF)
    val Snow = Color(0xFFF7F7F7)
    val Ink = Color(0xFF0F0F0F)
    val Slate = Color(0xFF6E6E6E)
    val Smoke = Color(0xFFE0E0E0)

    // Dark Core
    val Obsidian = Color(0xFF0F0F0F)
    val Charcoal = Color(0xFF191919)
    val Cloud = Color(0xFFE8E8E8)
    val Graphite = Color(0xFFA0A0A0)
    val Jet = Color(0xFF222222)

    // Semantic
    val Mint = Color(0xFF22C55E)  // Positive / Growth
    val Rose = Color(0xFFEF4444)  // Negative / Reduction
    val Azure = Color(0xFF3B82F6) // Intelligence / System
    val Amber = Color(0xFFF59E0B) // Warning / Attention
}

enum class LedgerSurfaceLevel { 
    Base, Inset, Section, Overlay;
    
    companion object {
        val Level0 = Base
        val Level1 = Inset
        val Level2 = Section
        val Level3 = Overlay
        val Hero = Base
    }
}

/**
 * LDL V3 semantic color contract.
 */
data class LedgerColors(
    val themeType: LedgerThemeType,
    val isDark: Boolean,
    val surfaceBase: Color,
    val surfaceInset: Color,
    val surfaceSection: Color,
    val surfaceOverlay: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val positive: Color,
    val negative: Color,
    val system: Color,
    val attention: Color,
) {
    // V2 Compatibility Aliases
    val label: Color get() = textPrimary
    val secondaryLabel: Color get() = textSecondary
    val tertiaryLabel: Color get() = textTertiary
    val separator: Color get() = border
    val tint: Color get() = system
    val onTint: Color get() = if (isDark) LedgerV3Palette.Obsidian else LedgerV3Palette.Paper
    val income: Color get() = positive
    val expense: Color get() = negative
    val pending: Color get() = attention
    val success: Color get() = positive
    val surfaceLevel0: Color get() = surfaceBase
    val surfaceLevel1: Color get() = surfaceInset
    val surfaceLevel2: Color get() = surfaceSection
    val surfaceLevel3: Color get() = surfaceOverlay

    // Hero Glow Compatibility
    val heroGlowPrimary: Color get() = positive
    val heroGlowSecondary: Color get() = system
    val heroGlowCool: Color get() = border
    val heroGlowWarm: Color get() = attention

    fun surface(level: LedgerSurfaceLevel): Color = when (level) {
        LedgerSurfaceLevel.Base -> surfaceBase
        LedgerSurfaceLevel.Inset -> surfaceInset
        LedgerSurfaceLevel.Section -> surfaceSection
        LedgerSurfaceLevel.Overlay -> surfaceOverlay
    }
}

val LedgerV3LightColors = LedgerColors(
    themeType = LedgerThemeType.Classic,
    isDark = false,
    surfaceBase = LedgerV3Palette.Paper,
    surfaceInset = LedgerV3Palette.Snow,
    surfaceSection = LedgerV3Palette.Snow,
    surfaceOverlay = LedgerV3Palette.Paper,
    textPrimary = LedgerV3Palette.Ink,
    textSecondary = LedgerV3Palette.Slate,
    textTertiary = LedgerV3Palette.Slate.copy(alpha = 0.5f),
    border = LedgerV3Palette.Smoke,
    positive = LedgerV3Palette.Mint,
    negative = LedgerV3Palette.Rose,
    system = LedgerV3Palette.Azure,
    attention = LedgerV3Palette.Amber,
)

val LedgerV3DarkColors = LedgerColors(
    themeType = LedgerThemeType.Classic,
    isDark = true,
    surfaceBase = LedgerV3Palette.Obsidian,
    surfaceInset = LedgerV3Palette.Charcoal,
    surfaceSection = LedgerV3Palette.Charcoal,
    surfaceOverlay = LedgerV3Palette.Charcoal,
    textPrimary = LedgerV3Palette.Cloud,
    textSecondary = LedgerV3Palette.Graphite,
    textTertiary = LedgerV3Palette.Graphite.copy(alpha = 0.5f),
    border = LedgerV3Palette.Jet,
    positive = LedgerV3Palette.Mint,
    negative = LedgerV3Palette.Rose,
    system = LedgerV3Palette.Azure,
    attention = LedgerV3Palette.Amber,
)

val LocalLedgerColors = staticCompositionLocalOf { LedgerV3LightColors }

// Material 3 Mappings (Toolkit Fallback)
val LedgerLightColorScheme = lightColorScheme(
    primary = LedgerV3Palette.Ink,
    onPrimary = LedgerV3Palette.Paper,
    background = LedgerV3Palette.Paper,
    surface = LedgerV3Palette.Paper,
)

val LedgerDarkColorScheme = darkColorScheme(
    primary = LedgerV3Palette.Cloud,
    onPrimary = LedgerV3Palette.Obsidian,
    background = LedgerV3Palette.Obsidian,
    surface = LedgerV3Palette.Obsidian,
)
