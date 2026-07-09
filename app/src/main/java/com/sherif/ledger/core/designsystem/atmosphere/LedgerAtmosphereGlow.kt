package com.sherif.ledger.core.designsystem.atmosphere

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.LedgerThemeType

/**
 * Renders the atmospheric lighting for Ledger.
 *
 * Performance-safe implementation using layered radial gradients to simulate
 * light diffusion and depth without heavy Gaussian blur.
 */
@Composable
fun LedgerAtmosphereGlow(
    modifier: Modifier = Modifier,
    atmosphere: Atmosphere = LedgerAtmosphere.current,
) {
    val colors = LedgerTheme.colors
    val isGlass = colors.themeType != LedgerThemeType.Classic
    
    // Classic uses a very restrained wash. Glass uses richer diffusion.
    val baseAlpha = if (isGlass) 1.0f else 0.4f
    val intensity = atmosphere.intensity * baseAlpha

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // 1. Primary Directional Light (Diffusion source)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    atmosphere.primaryGlow.copy(alpha = 0.15f * intensity),
                    Color.Transparent
                ),
                center = Offset(w * 0.2f, h * 0.15f),
                radius = w * 1.5f
            )
        )

        // 2. Secondary Atmospheric Tone (Temperature modulation)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    atmosphere.secondaryGlow.copy(alpha = 0.10f * intensity),
                    Color.Transparent
                ),
                center = Offset(w * 0.8f, h * 0.45f),
                radius = w * 1.2f
            )
        )

        // 3. Temperature Wash
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    atmosphere.coolGlow.copy(alpha = 0.05f * intensity),
                    atmosphere.warmGlow.copy(alpha = 0.05f * intensity)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}
