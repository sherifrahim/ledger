package com.sherif.ledger.core.designsystem.atmosphere

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale

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

        // 1. Entry wash
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.22f),
                    primary.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                start = Offset(0f, 0f),
                end = Offset(w * 0.85f, h * 0.32f),
            ),
            size = Size(w, h * 0.38f),
        )

        // 2. Light core
        rotate(-22f, Offset(w * 0.22f, h * 0.08f)) {
            scale(3.0f, 0.5f, Offset(w * 0.22f, h * 0.08f)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(secondary.copy(alpha = 0.18f), Color.Transparent),
                        Offset(w * 0.22f, h * 0.08f), h * 0.09f,
                    ),
                )
            }
        }

        // 3. Cool temperature
        rotate(-28f, Offset(w * 0.48f, h * 0.04f)) {
            scale(3.2f, 0.35f, Offset(w * 0.48f, h * 0.04f)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(cool.copy(alpha = 0.10f), Color.Transparent),
                        Offset(w * 0.48f, h * 0.04f), h * 0.07f,
                    ),
                )
            }
        }

        // 4. Mid-path
        rotate(-15f, Offset(w * 0.45f, h * 0.12f)) {
            scale(2.6f, 0.45f, Offset(w * 0.45f, h * 0.12f)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(primary.copy(alpha = 0.10f), Color.Transparent),
                        Offset(w * 0.45f, h * 0.12f), h * 0.08f,
                    ),
                )
            }
        }

        // 5. Light spill
        rotate(-10f, Offset(w * 0.7f, h * 0.16f)) {
            scale(2.2f, 0.35f, Offset(w * 0.7f, h * 0.16f)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(primary.copy(alpha = 0.05f), Color.Transparent),
                        Offset(w * 0.7f, h * 0.16f), h * 0.06f,
                    ),
                )
            }
        }

        // 6. Warm shadow
        rotate(8f, Offset(w * 0.55f, h * 0.24f)) {
            scale(3.5f, 0.3f, Offset(w * 0.55f, h * 0.24f)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(warm.copy(alpha = 0.05f), Color.Transparent),
                        Offset(w * 0.55f, h * 0.24f), h * 0.06f,
                    ),
                )
            }
        }

        // 7. Surface continuity
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.04f),
                    cool.copy(alpha = 0.02f),
                    Color.Transparent,
                ),
                start = Offset(w * 0.1f, h * 0.20f),
                end = Offset(w * 0.8f, h * 0.42f),
            ),
            topLeft = Offset(0f, h * 0.18f),
            size = Size(w, h * 0.25f),
        )

        // 8. Secondary cool wisp
        rotate(-35f, Offset(w * 0.6f, h * 0.02f)) {
            scale(2.5f, 0.25f, Offset(w * 0.6f, h * 0.02f)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(cool.copy(alpha = 0.06f), Color.Transparent),
                        Offset(w * 0.6f, h * 0.02f), h * 0.05f,
                    ),
                )
            }
        }
    }
}
