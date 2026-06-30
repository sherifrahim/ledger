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
 * Renders the volumetric atmospheric glow.
 *
 * This component pulls from the current [Atmosphere] to create the
 * signature Ledger lighting effect. It should be placed at the
 * bottom of the Z-stack.
 */
@Composable
fun LedgerAtmosphereGlow(
    modifier: Modifier = Modifier,
    atmosphere: Atmosphere = LedgerAtmosphere.current,
) {
    val primary = atmosphere.primaryGlow
    val secondary = atmosphere.secondaryGlow
    val cool = atmosphere.coolGlow
    val warm = atmosphere.warmGlow

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // 1. The Horizon Arc (Signature Ledger Element)
        // Faithfully recreates the elegant emerald curve seen in Screen 1.
        val arcPath = Path().apply {
            moveTo(0f, h * 0.35f)
            quadraticTo(
                w / 2f, h * 0.30f,
                w, h * 0.35f
            )
        }

        // Draw the glowing atmospheric wash above the arc
        drawPath(
            path = arcPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primary.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = h * 0.15f,
                endY = h * 0.35f
            ),
        )

        // 2. Primary concentrated light core (below balance area)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(w / 2f, h * 0.28f),
                radius = h * 0.15f
            )
        )

        // 3. Ambient atmospheric temperature (Cool and Warm wisps)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(cool.copy(alpha = 0.04f), warm.copy(alpha = 0.02f), Color.Transparent),
                start = Offset(0f, 0f),
                end = Offset(w, h * 0.40f)
            )
        )
    }
}
