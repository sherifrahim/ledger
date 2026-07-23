package com.sherif.ledger.core.designsystem.atmosphere

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
/**
 * Renders the atmospheric lighting for Ledger.
 *
 * Rebuilt for V3 identity: Multi-layer elliptical diffusion sitting off-axis.
 * Simulates environmental architectural lighting rather than a UI decoration.
 */
@Composable
fun LedgerAtmosphereGlow(
    modifier: Modifier = Modifier,
    atmosphere: Atmosphere = LedgerAtmosphere.current,
    scrollProgress: Float = 0f // Deterministic scroll-based reaction
) {
    // Luxury intensity through restraint.
    val intensity = (atmosphere.intensity * 0.35f).coerceIn(0f, 1f)

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // 1. LAYER A: Massive Off-Axis Elliptical Wash (The Source)
        // Sitting 20% above and 20% left of the viewport.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    atmosphere.primaryGlow.copy(alpha = 0.12f * intensity),
                    Color.Transparent
                ),
                center = Offset(w * 0.15f, -h * 0.1f),
                radius = w * 1.8f
            )
        )

        // 2. LAYER B: The Environmental Diffusion (The Air)
        // Central wash that reacts softly to scroll progress.
        val diffusionExpansion = 1.0f + (scrollProgress * 0.15f)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    atmosphere.primaryGlow.copy(alpha = 0.04f * intensity),
                    atmosphere.coolGlow.copy(alpha = 0.02f * intensity),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h * 0.6f * diffusionExpansion
            )
        )

        // 3. LAYER C: Graphite Vignette (The Ground)
        // Ensures the instrument settles into the negative space.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.08f * intensity)
                ),
                startY = h * 0.7f,
                endY = h
            )
        )
    }
}
