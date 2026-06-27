#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying LDL v1.2: neutral rebalance..."

cat > "$B/core/designsystem/theme/LedgerColors.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Emerald accent ramp (unchanged, used only for meaning) ──

val LedgerEmerald50 = Color(0xFFECFDF5)
val LedgerEmerald100 = Color(0xFFD1FAE5)
val LedgerEmerald200 = Color(0xFFA7F3D0)
val LedgerEmerald300 = Color(0xFF6EE7B7)
val LedgerEmerald400 = Color(0xFF34D399)
val LedgerEmerald500 = Color(0xFF10B981)
val LedgerEmerald600 = Color(0xFF059669)
val LedgerEmerald700 = Color(0xFF047857)
val LedgerEmerald800 = Color(0xFF065F46)
val LedgerEmerald900 = Color(0xFF064E3B)
val LedgerEmerald950 = Color(0xFF022C22)

// ── Neutral palette (rebalanced: no green in structural colors) ──

val LedgerInk = Color(0xFF111111)
val LedgerGraphite = Color(0xFF525252)
val LedgerMist = Color(0xFFF5F5F5)
val LedgerPaper = Color(0xFFFFFFFF)
val LedgerLine = Color(0xFFE0E0E0)

val LedgerDarkInk = Color(0xFFE8E8E8)
val LedgerDarkGraphite = Color(0xFFA0A0A0)
val LedgerDarkSurface = Color(0xFF141414)
val LedgerDarkSurfaceHigh = Color(0xFF1C1C1C)
val LedgerDarkBackground = Color(0xFF0A0A0A)
val LedgerDarkLine = Color(0xFF262626)

val LedgerSuccess = Color(0xFF047857)
val LedgerWarning = Color(0xFFB7791F)
val LedgerError = Color(0xFFBA1A1A)

// ── Material schemes (fed underneath, neutral structural colors) ──

val LedgerLightColorScheme = lightColorScheme(
    primary = LedgerEmerald700,
    onPrimary = Color.White,
    primaryContainer = LedgerEmerald100,
    onPrimaryContainer = LedgerEmerald950,
    secondary = Color(0xFF555555),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF505060),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDDDE8),
    onTertiaryContainer = Color(0xFF1A1A22),
    error = LedgerError,
    onError = Color.White,
    background = LedgerMist,
    onBackground = LedgerInk,
    surface = LedgerPaper,
    onSurface = LedgerInk,
    surfaceVariant = Color(0xFFECECEC),
    onSurfaceVariant = LedgerGraphite,
    outline = Color(0xFF787878),
    outlineVariant = LedgerLine,
    inverseSurface = LedgerInk,
    inverseOnSurface = LedgerMist,
    inversePrimary = LedgerEmerald300,
    scrim = Color.Black,
)

val LedgerDarkColorScheme = darkColorScheme(
    primary = LedgerEmerald300,
    onPrimary = LedgerEmerald950,
    primaryContainer = LedgerEmerald800,
    onPrimaryContainer = LedgerEmerald100,
    secondary = Color(0xFFBBBBBB),
    onSecondary = Color(0xFF2A2A2A),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFB0B0C0),
    onTertiary = Color(0xFF1E1E28),
    tertiaryContainer = Color(0xFF363646),
    onTertiaryContainer = Color(0xFFDDDDE8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = LedgerDarkBackground,
    onBackground = LedgerDarkInk,
    surface = LedgerDarkSurface,
    onSurface = LedgerDarkInk,
    surfaceVariant = LedgerDarkSurfaceHigh,
    onSurfaceVariant = LedgerDarkGraphite,
    outline = Color(0xFF888888),
    outlineVariant = LedgerDarkLine,
    inverseSurface = LedgerDarkInk,
    inverseOnSurface = LedgerDarkBackground,
    inversePrimary = LedgerEmerald700,
    scrim = Color.Black,
)

// ── LDL semantic layer ──

enum class LedgerSurfaceLevel { Level0, Level1, Level2, Level3 }

/**
 * The LDL semantic color contract, read through [LedgerTheme.colors].
 *
 * Structural colors (surfaces, labels, separators) are pure neutral graphite.
 * Emerald appears only through [tint], [income], and [success] to communicate
 * financial meaning or interactivity. If every green element were removed the
 * app would still look complete.
 *
 * Hero atmosphere tokens carry the only ambient teal in the entire palette,
 * applied at very low opacity so the glow reads as depth, not color.
 */
data class LedgerColors(
    val isDark: Boolean,
    val surfaceLevel0: Color,
    val surfaceLevel1: Color,
    val surfaceLevel2: Color,
    val surfaceLevel3: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val tint: Color,
    val onTint: Color,
    val income: Color,
    val expense: Color,
    val pending: Color,
    val refund: Color,
    val warning: Color,
    val success: Color,
    val neutral: Color,
    val heroGlowPrimary: Color,
    val heroGlowSecondary: Color,
    val heroGlowWarm: Color,
) {
    fun surface(level: LedgerSurfaceLevel): Color = when (level) {
        LedgerSurfaceLevel.Level0 -> surfaceLevel0
        LedgerSurfaceLevel.Level1 -> surfaceLevel1
        LedgerSurfaceLevel.Level2 -> surfaceLevel2
        LedgerSurfaceLevel.Level3 -> surfaceLevel3
    }
}

val LedgerLightColors = LedgerColors(
    isDark = false,
    surfaceLevel0 = Color(0xFFF3F3F3),
    surfaceLevel1 = Color(0xFFFFFFFF),
    surfaceLevel2 = Color(0xFFF7F7F7),
    surfaceLevel3 = Color(0xFFFFFFFF),
    label = Color(0xFF111111),
    secondaryLabel = Color(0xFF636363),
    tertiaryLabel = Color(0xFF8E8E8E),
    separator = Color(0xFFE0E0E0),
    tint = LedgerEmerald700,
    onTint = Color(0xFFFFFFFF),
    income = LedgerEmerald700,
    expense = Color(0xFFC0362C),
    pending = Color(0xFFB7791F),
    refund = Color(0xFF2563EB),
    warning = Color(0xFFB7791F),
    success = LedgerEmerald700,
    neutral = Color(0xFF737373),
    heroGlowPrimary = Color(0xFFD8E8E4),
    heroGlowSecondary = Color(0xFFCCDDD7),
    heroGlowWarm = Color(0xFFF0EDE0),
)

val LedgerDarkColors = LedgerColors(
    isDark = true,
    surfaceLevel0 = Color(0xFF0A0A0A),
    surfaceLevel1 = Color(0xFF141414),
    surfaceLevel2 = Color(0xFF1C1C1C),
    surfaceLevel3 = Color(0xFF242424),
    label = Color(0xFFE8E8E8),
    secondaryLabel = Color(0xFFA0A0A0),
    tertiaryLabel = Color(0xFF6E6E6E),
    separator = Color(0xFF262626),
    tint = LedgerEmerald300,
    onTint = LedgerEmerald950,
    income = LedgerEmerald300,
    expense = Color(0xFFF97066),
    pending = Color(0xFFF0B860),
    refund = Color(0xFF60A5FA),
    warning = Color(0xFFF0B860),
    success = LedgerEmerald300,
    neutral = Color(0xFF8A8A8A),
    heroGlowPrimary = Color(0xFF0F1F1D),
    heroGlowSecondary = Color(0xFF082220),
    heroGlowWarm = Color(0xFF1A1A14),
)

val LocalLedgerColors = staticCompositionLocalOf { LedgerLightColors }
EOF

echo "Done. LedgerColors.kt rebalanced to neutral graphite."
echo "Run: git add -A && git commit -m 'fix(theme): rebalance palette to neutral graphite, emerald accent only'"
echo "Then: ./gradlew assembleDebug"
