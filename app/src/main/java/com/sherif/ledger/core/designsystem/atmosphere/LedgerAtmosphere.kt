package com.sherif.ledger.core.designsystem.atmosphere

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL atmospheric configuration.
 *
 * Defines the environmental lighting and mood that penetrates surfaces.
 * This is the global state that [LedgerSurface] and other components
 * query to decide how to reflect light or apply translucency.
 */
data class Atmosphere(
    val primaryGlow: Color,
    val secondaryGlow: Color,
    val coolGlow: Color,
    val warmGlow: Color,
    val intensity: Float = 1.0f,
)

val LocalLedgerAtmosphere = staticCompositionLocalOf {
    Atmosphere(
        primaryGlow = Color.Transparent,
        secondaryGlow = Color.Transparent,
        coolGlow = Color.Transparent,
        warmGlow = Color.Transparent,
        intensity = 0f,
    )
}

/**
 * Entry point for atmospheric effects.
 */
object LedgerAtmosphere {
    val current: Atmosphere
        @Composable @ReadOnlyComposable get() = LocalLedgerAtmosphere.current

    /**
     * Default calm atmosphere derived from theme colors.
     */
    @Composable
    @ReadOnlyComposable
    fun default(): Atmosphere {
        val colors = LedgerTheme.colors
        return Atmosphere(
            primaryGlow = colors.heroGlowPrimary,
            secondaryGlow = colors.heroGlowSecondary,
            coolGlow = colors.heroGlowCool,
            warmGlow = colors.heroGlowWarm,
        )
    }
}

/**
 * Provides the [Atmosphere] to the [content].
 */
@Composable
fun LedgerAtmosphereProvider(
    atmosphere: Atmosphere = LedgerAtmosphere.default(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLedgerAtmosphere provides atmosphere,
        content = content,
    )
}
