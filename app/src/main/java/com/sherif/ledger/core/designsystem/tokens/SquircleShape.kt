package com.sherif.ledger.core.designsystem.tokens

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A "continuous corner" rounded rectangle — the iOS/Apple Wallet corner shape,
 * where the curve blends into the straight edge rather than starting and
 * stopping abruptly the way a circular-arc corner ([androidx.compose.foundation.shape.RoundedCornerShape])
 * does. That abrupt transition is a large part of why a Compose app's default
 * cards read as generic Android Material Design at a glance, independent of
 * colour or type — the corner geometry itself is a different shape family.
 *
 * Built the same way [androidx.compose.foundation.shape.RoundedCornerShape]
 * is internally — four cubic BÃ©zier corners — but with the control-point
 * ("kappa") distance tunable instead of fixed at the circular-arc constant
 * (~0.552). Raising it past that stretches the curve's influence further
 * along each edge before it turns, which is what reads as "smoother" /
 * "continuous" rather than "quarter-circle stuck onto a rectangle."
 */
class SquircleShape(
    private val cornerRadius: Dp,
    /** 0.552 reproduces a true circular arc (RoundedCornerShape); higher is smoother. */
    private val smoothness: Float = 0.8f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = with(density) { cornerRadius.toPx() }
            .coerceAtMost(minOf(size.width, size.height) / 2f)
            .coerceAtLeast(0f)
        if (radius <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))

        val w = size.width
        val h = size.height
        val k = radius * smoothness

        val path = Path().apply {
            moveTo(radius, 0f)
            lineTo(w - radius, 0f)
            cubicTo(w - radius + k, 0f, w, k, w, radius)
            lineTo(w, h - radius)
            cubicTo(w, h - radius + k, w - k, h, w - radius, h)
            lineTo(radius, h)
            cubicTo(radius - k, h, 0f, h - k, 0f, h - radius)
            lineTo(0f, radius)
            cubicTo(0f, radius - k, radius - k, 0f, radius, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
