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
    // Light Core — editorial paper. Page and cards are both near-white; card
    // separation comes from a soft shadow + whisper hairline (see LedgerCard),
    // not from a grey fill. Inset is a cool off-white for chips/nested rows.
    val Paper = Color(0xFFFFFFFF)
    val Snow = Color(0xFFF5F5F6)
    val Ink = Color(0xFF0A0A0B)
    val Slate = Color(0xFF6B7076)
    val Smoke = Color(0xFFECEDEE)

    // Dark Core — premium near-black with a faint cool cast (echoes the brand
    // panel). Cards sit one step lighter than the page; separation is by
    // lightness, not shadow.
    val Obsidian = Color(0xFF0E1013)   // page base
    val Charcoal = Color(0xFF181B20)   // elevated card / inset
    val Cloud = Color(0xFFEDEEF0)
    val Graphite = Color(0xFF9A9FA7)
    val Jet = Color(0xFF262A30)

    // Semantic accents — restrained. Green is the brand accent (growth/positive
    // and primary CTAs); the rest are reserved for their meaning only.
    val Mint = Color(0xFF22C55E)  // Positive / Growth / Brand accent
    val Rose = Color(0xFFF04438)  // Negative / Reduction
    val Azure = Color(0xFF3B82F6) // Intelligence / System / Links
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
    // Card philosophy: the primary content surface. In light it is pure paper
    // (separated from the page by a soft shadow); in dark it steps one level
    // lighter than the page so it reads without a shadow. See LedgerCard.
    val surfaceCard: Color get() = if (isDark) LedgerV3Palette.Charcoal else LedgerV3Palette.Paper
    /** Colour for the soft card shadow (light theme only — near-zero effect in dark). */
    val shadowColor: Color get() = if (isDark) Color(0xFF000000) else Color(0xFF101828)
    /** Whisper hairline on card edges, softer than the standard content [border]. */
    val cardBorder: Color get() = if (isDark) LedgerV3Palette.Jet else LedgerV3Palette.Smoke
    /** Brand accent — the single saturated colour used for primary emphasis. */
    val accent: Color get() = positive

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
    isDark = false,
    surfaceBase = LedgerV3Palette.Paper,
    surfaceInset = LedgerV3Palette.Snow,
    surfaceSection = LedgerV3Palette.Snow,
    surfaceOverlay = LedgerV3Palette.Paper,
    textPrimary = LedgerV3Palette.Ink,
    textSecondary = LedgerV3Palette.Slate,
    textTertiary = LedgerV3Palette.Slate.copy(alpha = 0.65f), // a11y: raised from 0.5 for contrast
    border = LedgerV3Palette.Smoke,
    positive = LedgerV3Palette.Mint,
    negative = LedgerV3Palette.Rose,
    system = LedgerV3Palette.Azure,
    attention = LedgerV3Palette.Amber,
)

val LedgerV3DarkColors = LedgerColors(
    isDark = true,
    surfaceBase = LedgerV3Palette.Obsidian,
    surfaceInset = LedgerV3Palette.Charcoal,
    surfaceSection = LedgerV3Palette.Charcoal,
    surfaceOverlay = LedgerV3Palette.Charcoal,
    textPrimary = LedgerV3Palette.Cloud,
    textSecondary = LedgerV3Palette.Graphite,
    textTertiary = LedgerV3Palette.Graphite.copy(alpha = 0.68f), // a11y: raised from 0.5 for contrast
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
