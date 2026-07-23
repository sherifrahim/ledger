package com.sherif.ledger.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeState
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
    themeType: LedgerThemeType = LedgerThemeType.Dark,
    liquidGlass: Boolean = false,
    darkTheme: Boolean = themeType == LedgerThemeType.Dark,
    content: @Composable () -> Unit,
) {
    val ledgerColors = if (darkTheme) LedgerV3DarkColors else LedgerV3LightColors
    val materialColorScheme = if (darkTheme) LedgerDarkColorScheme else LedgerLightColorScheme

    // Two backdrop-blur layers for Liquid Glass: one for the scrolling content
    // beneath the nav island, one for the ambient backdrop beneath cards. Held
    // here so every glass surface in the tree shares them. Cheap and unused when
    // glass is off.
    val navHazeState = remember { HazeState() }
    val cardHazeState = remember { HazeState() }

    CompositionLocalProvider(
        LocalLedgerColors provides ledgerColors,
        LocalLedgerGlass provides liquidGlass,
        LocalNavHazeState provides navHazeState,
        LocalCardHazeState provides cardHazeState,
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
