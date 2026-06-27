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
import com.sherif.ledger.core.designsystem.tokens.LedgerBorder
import com.sherif.ledger.core.designsystem.tokens.LedgerOpacity
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Applies the Ledger Design Language.
 *
 * Material3 stays underneath purely as a toolkit: it still receives a
 * [androidx.compose.material3.ColorScheme] so stray Material widgets do not
 * render broken. LDL components, however, read meaning from [LedgerTheme], not
 * from Material color roles. That separation is what lets Ledger shed Material's
 * visual identity without rebuilding the component toolkit.
 *
 * Dynamic color remains available only as an explicit opt in and is off by
 * default, so Ledger keeps its own palette rather than adopting the device's.
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
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LedgerTypography,
            shapes = LedgerShapes,
            content = content,
        )
    }
}

/**
 * The single semantic entry point for LDL.
 *
 * Components reach design decisions through this object rather than importing
 * tokens piecemeal or reading Material roles, so the vocabulary stays one
 * consistent surface. Only [colors] varies with light and dark, so only it flows
 * through a CompositionLocal. The rest are stable tokens exposed directly, which
 * keeps the theme honest and avoids CompositionLocals that never change.
 */
object LedgerTheme {
    val colors: LedgerColors
        @Composable @ReadOnlyComposable get() = LocalLedgerColors.current

    val radius get() = LedgerRadius
    val border get() = LedgerBorder
    val opacity get() = LedgerOpacity
    val motion get() = LedgerMotion
    val elevation get() = LedgerElevation
}
