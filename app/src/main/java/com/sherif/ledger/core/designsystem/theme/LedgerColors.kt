package com.sherif.ledger.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
