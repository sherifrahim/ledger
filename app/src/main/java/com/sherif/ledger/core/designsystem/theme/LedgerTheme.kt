package com.sherif.ledger.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.sherif.ledger.core.designsystem.atmosphere.LedgerAtmosphere
import com.sherif.ledger.core.designsystem.atmosphere.LedgerAtmosphereProvider
import com.sherif.ledger.core.designsystem.haptics.LedgerHaptics
import com.sherif.ledger.core.designsystem.haptics.LedgerHapticProvider
import com.sherif.ledger.core.designsystem.tokens.LedgerBorder
import com.sherif.ledger.core.designsystem.tokens.LedgerIconSize
import com.sherif.ledger.core.designsystem.tokens.LedgerOpacity
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Applies the Ledger Design Language.
 *
 * Material3 remains underneath as a toolkit. LDL components read
 * [LedgerTheme] for semantic decisions, not Material color roles.
 */
@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LedgerDarkColorScheme
        else -> LedgerLightColorScheme
    }
    val ledgerColors = if (darkTheme) LedgerDarkColors else LedgerLightColors

    CompositionLocalProvider(LocalLedgerColors provides ledgerColors) {
        LedgerAtmosphereProvider {
            LedgerHapticProvider {
                MaterialTheme(
                    colorScheme = colorScheme,
                    typography = LedgerTypography,
                    shapes = LedgerShapes,
                    content = content,
                )
            }
        }
    }
}

/**
 * Single semantic entry point for LDL.
 *
 * Only [colors] varies with light/dark (CompositionLocal). Everything
 * else is stable tokens exposed directly.
 */
object LedgerTheme {
    val colors: LedgerColors
        @Composable @ReadOnlyComposable get() = LocalLedgerColors.current
    val atmosphere @Composable @ReadOnlyComposable get() = LedgerAtmosphere.current
    val haptics @Composable @ReadOnlyComposable get() = LedgerHaptics.current
    val typography get() = LedgerTypography
    val radius get() = LedgerRadius
    val border get() = LedgerBorder
    val opacity get() = LedgerOpacity
    val motion get() = LedgerMotion
    val elevation get() = LedgerElevation
    val iconSize get() = LedgerIconSize
}
