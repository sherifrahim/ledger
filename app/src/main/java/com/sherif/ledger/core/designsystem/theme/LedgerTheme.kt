package com.sherif.ledger.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Applies the Ledger Design System theme on top of Material 3.
 *
 * Dynamic color is available for Android 12 and above, but it is disabled by
 * default so Ledger keeps its emerald-based brand palette unless explicitly
 * requested by the caller.
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LedgerTypography,
        shapes = LedgerShapes,
        content = content,
    )
}
