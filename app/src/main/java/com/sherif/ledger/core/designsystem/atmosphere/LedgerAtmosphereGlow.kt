package com.sherif.ledger.core.designsystem.atmosphere

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Renders the Material Surface Illumination.
 *
 * This component moves away from "glow" and toward "material lighting."
 * It recreates the effect of light grazing a curved premium surface.
 */
@Composable
fun LedgerAtmosphereGlow(
    modifier: Modifier = Modifier,
    atmosphere: Atmosphere = LedgerAtmosphere.current,
) {
    val primary = atmosphere.primaryGlow
    val secondary = atmosphere.secondaryGlow
    
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // 1. Directional Surface Wash (Environmental Depth)
        // This is a large, low-contrast wash that gives the material its emerald base.
        // It is NOT a circular glow; it follows a wide vertical gradient.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    primary.copy(alpha = 0.04f),
                    primary.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h * 0.45f
            )
        )

        // 2. The Restrained Grazing Arc (Edge Illumination)
        // Simulates light catching a curved "carved" edge.
        // Refined for RC-010: More integrated into the material surface.
        val arcPath = Path().apply {
            moveTo(0f, h * 0.44f)
            quadraticTo(
                w / 2f, h * 0.40f,
                w, h * 0.44f
            )
        }

        // Grazing light - subtle stroke representing physical surface edge
        drawPath(
            path = arcPath,
            color = primary.copy(alpha = 0.10f),
            style = Stroke(width = 0.6.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Primary Directional Light (Restrained Surface presence)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.04f), Color.Transparent),
                center = Offset(w / 2f, h * 0.38f),
                radius = h * 0.30f
            )
        )
    }
}
