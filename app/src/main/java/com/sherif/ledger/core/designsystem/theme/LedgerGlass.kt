package com.sherif.ledger.core.designsystem.theme

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Liquid Glass — an *optional*, off-by-default surface style layered on top of
 * whichever base theme (light or dark) is active. Turned on in Settings →
 * Appearance; the solid surfaces are the default.
 *
 * ## Real backdrop blur (not a fake)
 *
 * This is genuine Apple-style glass: a live `RenderEffect` gaussian blur of the
 * content *behind* a surface (via the Haze library), with a translucent tint
 * and a luminous edge — the same construction as iOS/macOS materials. On
 * API < 31 (no `RenderEffect`) Haze degrades to a translucent scrim.
 *
 * Backdrop blur only reads as glass when there is something worth blurring
 * behind the surface, so there are two blur layers:
 *
 *  - **Nav island** blurs the *scrolling screen content* passing beneath it —
 *    exactly like an iOS navigation bar. It samples [LocalNavHazeState].
 *  - **Cards** blur a soft **ambient backdrop** ([ledgerAmbientBackground])
 *    rendered once behind the whole app. A card sits at the top of its screen
 *    with nothing behind it, so without this it would blur a flat colour and
 *    look like a plain panel (the bug in the first, translucency-only pass).
 *    Cards sample [LocalCardHazeState].
 */

/** True when the user has opted into Liquid Glass surfaces. Off by default. */
val LocalLedgerGlass = staticCompositionLocalOf { false }

/** Haze layer for the scrolling screen content — sampled by the nav island. */
val LocalNavHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Haze layer for the ambient backdrop — sampled by glass cards. */
val LocalCardHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * The ambient backdrop that glass cards refract: the base surface plus two very
 * soft emerald / azure light pools. Sits behind opaque screen content, so it is
 * invisible directly — it only shows up, blurred, through glass surfaces. That
 * is what gives the frosted-with-a-hint-of-colour Apple look instead of a flat
 * grey panel.
 */
fun Modifier.ledgerAmbientBackground(isDark: Boolean): Modifier = this.drawBehind {
    val base = if (isDark) Color(0xFF080B0A) else Color(0xFFEFF2F6)
    drawRect(base)
    drawRect(
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF10B981).copy(alpha = if (isDark) 0.18f else 0.12f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.16f, size.height * 0.06f),
            radius = size.maxDimension * 0.75f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = if (isDark) 0.14f else 0.10f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.9f, size.height * 0.9f),
            radius = size.maxDimension * 0.75f,
        ),
    )
}

/**
 * Real glass surface treatment. Blurs whatever [hazeState] captured behind it,
 * tints it toward [containerColor], clips to [shape] and finishes with a
 * luminous hairline edge. Used by [LedgerCard] and the nav island in place of
 * the solid fill when glass is enabled.
 */
@Composable
fun Modifier.ledgerGlassSurface(
    hazeState: HazeState,
    shape: Shape,
    isDark: Boolean,
    containerColor: Color,
): Modifier {
    val style = if (isDark) {
        HazeMaterials.thin(containerColor)
    } else {
        HazeMaterials.regular(containerColor)
    }
    val edge = if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.55f)
    return this
        .clip(shape)
        .hazeEffect(state = hazeState, style = style)
        .border(1.dp, edge, shape)
}
