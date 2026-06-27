package com.sherif.ledger.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

val LedgerInk = Color(0xFF101815)
val LedgerGraphite = Color(0xFF47534F)
val LedgerMist = Color(0xFFF7FAF8)
val LedgerPaper = Color(0xFFFFFFFF)
val LedgerLine = Color(0xFFE1EAE5)

val LedgerDarkInk = Color(0xFFE6F2EC)
val LedgerDarkGraphite = Color(0xFFA7B8B0)
val LedgerDarkSurface = Color(0xFF111C18)
val LedgerDarkSurfaceHigh = Color(0xFF182520)
val LedgerDarkBackground = Color(0xFF08110E)
val LedgerDarkLine = Color(0xFF2B3B34)

val LedgerSuccess = Color(0xFF047857)
val LedgerWarning = Color(0xFFB7791F)
val LedgerError = Color(0xFFBA1A1A)

val LedgerLightColorScheme = lightColorScheme(
    primary = LedgerEmerald700,
    onPrimary = Color.White,
    primaryContainer = LedgerEmerald100,
    onPrimaryContainer = LedgerEmerald950,
    secondary = Color(0xFF4D635A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8DD),
    onSecondaryContainer = Color(0xFF0A1F18),
    tertiary = Color(0xFF446277),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6FF),
    onTertiaryContainer = Color(0xFF001E2E),
    error = LedgerError,
    onError = Color.White,
    background = LedgerMist,
    onBackground = LedgerInk,
    surface = LedgerPaper,
    onSurface = LedgerInk,
    surfaceVariant = Color(0xFFEAF2EE),
    onSurfaceVariant = LedgerGraphite,
    outline = Color(0xFF74827B),
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
    secondary = Color(0xFFB4CCC0),
    onSecondary = Color(0xFF20352D),
    secondaryContainer = Color(0xFF364B43),
    onSecondaryContainer = Color(0xFFD0E8DD),
    tertiary = Color(0xFFACCAE0),
    onTertiary = Color(0xFF143348),
    tertiaryContainer = Color(0xFF2C4A5F),
    onTertiaryContainer = Color(0xFFC8E6FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = LedgerDarkBackground,
    onBackground = LedgerDarkInk,
    surface = LedgerDarkSurface,
    onSurface = LedgerDarkInk,
    surfaceVariant = LedgerDarkSurfaceHigh,
    onSurfaceVariant = LedgerDarkGraphite,
    outline = Color(0xFF8D9D95),
    outlineVariant = LedgerDarkLine,
    inverseSurface = LedgerDarkInk,
    inverseOnSurface = LedgerDarkBackground,
    inversePrimary = LedgerEmerald700,
    scrim = Color.Black,
)

/**
 * Semantic surface hierarchy for LDL.
 *
 * A component declares the intent of its surface rather than a color or an
 * elevation. The theme resolves intent to a concrete tone per light and dark.
 * In LDL these levels are tonal steps, not shadows, matching the Apple
 * grouped-list grammar where depth reads through tone and hairlines.
 *
 * Level0 app background. Level1 the primary grouped section that holds content.
 * Level2 a nested block inside a section. Level3 transient top layer, sheets and
 * menus.
 */
enum class LedgerSurfaceLevel { Level0, Level1, Level2, Level3 }

/**
 * The LDL semantic color contract, read through [LedgerTheme.colors].
 *
 * Components never reference raw palette constants or Material color roles. They
 * ask for meaning: a surface level, a label, a separator, or a financial
 * semantic such as income or expense. This is the layer that lets Ledger drop
 * Material's visual identity while keeping Material as the toolkit underneath.
 *
 * Financial semantics carry the Monzo layer of LDL: direction and status of
 * money are always communicated by a dedicated color, never by decoration.
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
) {
    /** Resolves a semantic surface level to its concrete tone. */
    fun surface(level: LedgerSurfaceLevel): Color = when (level) {
        LedgerSurfaceLevel.Level0 -> surfaceLevel0
        LedgerSurfaceLevel.Level1 -> surfaceLevel1
        LedgerSurfaceLevel.Level2 -> surfaceLevel2
        LedgerSurfaceLevel.Level3 -> surfaceLevel3
    }
}

val LedgerLightColors = LedgerColors(
    isDark = false,
    surfaceLevel0 = Color(0xFFF2F3F5),
    surfaceLevel1 = Color(0xFFFFFFFF),
    surfaceLevel2 = Color(0xFFF6F7F9),
    surfaceLevel3 = Color(0xFFFFFFFF),
    label = Color(0xFF0F1714),
    secondaryLabel = Color(0xFF5B6B64),
    tertiaryLabel = Color(0xFF8A9A92),
    separator = Color(0xFFE2E7E4),
    tint = LedgerEmerald700,
    onTint = Color(0xFFFFFFFF),
    income = LedgerEmerald700,
    expense = Color(0xFFC0362C),
    pending = Color(0xFFB7791F),
    refund = Color(0xFF2563EB),
    warning = Color(0xFFB7791F),
    success = LedgerEmerald700,
    neutral = Color(0xFF6B7770),
)

val LedgerDarkColors = LedgerColors(
    isDark = true,
    surfaceLevel0 = Color(0xFF08110E),
    surfaceLevel1 = Color(0xFF111C18),
    surfaceLevel2 = Color(0xFF182520),
    surfaceLevel3 = Color(0xFF1F2C27),
    label = Color(0xFFE6F2EC),
    secondaryLabel = Color(0xFFA7B8B0),
    tertiaryLabel = Color(0xFF7C8B84),
    separator = Color(0xFF2B3B34),
    tint = LedgerEmerald300,
    onTint = LedgerEmerald950,
    income = LedgerEmerald300,
    expense = Color(0xFFF97066),
    pending = Color(0xFFF0B860),
    refund = Color(0xFF60A5FA),
    warning = Color(0xFFF0B860),
    success = LedgerEmerald300,
    neutral = Color(0xFF9CA9A2),
)

/** Theme-provided LDL colors. Read via [LedgerTheme.colors], never directly. */
val LocalLedgerColors = staticCompositionLocalOf { LedgerLightColors }
