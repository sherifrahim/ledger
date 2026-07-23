package com.sherif.ledger.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.sherif.ledger.core.designsystem.haptics.LedgerHaptics
import com.sherif.ledger.core.designsystem.haptics.LedgerHapticProvider
import com.sherif.ledger.core.designsystem.tokens.LedgerBorder
import com.sherif.ledger.core.designsystem.tokens.LedgerIconSize
import com.sherif.ledger.core.designsystem.tokens.LedgerOpacity
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Ledger V3 Theme (Machined Financial Instrument)
 * 
 * A complete reboot focused on editorial clarity, architectural precision,
 * and high-fidelity typography. Replaces all V1/V2 visual systems.
 */
@Composable
fun LedgerTheme(
    themeType: LedgerThemeType = LedgerThemeType.Classic,
    liquidGlass: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val ledgerColors = when (themeType) {
        LedgerThemeType.Classic -> if (darkTheme) LedgerV3DarkColors else LedgerV3LightColors
        LedgerThemeType.MidnightGlass -> LedgerV3DarkColors
    }
    val materialColorScheme = if (darkTheme) LedgerDarkColorScheme else LedgerLightColorScheme

    CompositionLocalProvider(
        LocalLedgerColors provides ledgerColors,
        LocalLedgerGlass provides liquidGlass,
    ) {
        LedgerHapticProvider {
            MaterialTheme(
                colorScheme = materialColorScheme,
                typography = LedgerTypography,
                shapes = LedgerShapes,
                content = content,
            )
        }
    }
}

/**
 * Single authority for Ledger Design System (LDS) tokens.
 */
object LedgerTheme {
    val colors: LedgerColors
        @Composable @ReadOnlyComposable get() = LocalLedgerColors.current

    /** True when the user has opted into the optional Liquid Glass surfaces. */
    val glass: Boolean
        @Composable @ReadOnlyComposable get() = LocalLedgerGlass.current

    val haptics @Composable @ReadOnlyComposable get() = LedgerHaptics.current
    
    val typography get() = LedgerTypography
    val spacing get() = LedgerSpacing
    val radius get() = LedgerRadius
    val border get() = LedgerBorder
    val opacity get() = LedgerOpacity
    val motion get() = LedgerMotion

    // V2 Compatibility
    val iconSize get() = LedgerIconSize
    val elevation get() = LedgerElevation
    val atmosphere @Composable @ReadOnlyComposable get() = com.sherif.ledger.core.designsystem.atmosphere.LedgerAtmosphere.current
}
