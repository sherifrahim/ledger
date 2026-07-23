package com.sherif.ledger.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass — an *optional*, off-by-default surface style layered on top of
 * whichever base theme (light or dark) is active. It is never forced: the
 * default dark/light solid surfaces do the work unless the user turns this on
 * in Settings → Appearance.
 *
 * ## What this is (and honestly isn't)
 *
 * Apple's Liquid Glass is a real-time backdrop blur with specular highlights.
 * A true gaussian *backdrop* blur (blurring the content *behind* a surface) is
 * not achievable in pure Jetpack Compose without a dedicated haze library —
 * `Modifier.blur` only blurs an element's own pixels, not what sits under it,
 * and we deliberately don't pull in a new dependency for a cosmetic option.
 *
 * So this is a **translucency-based interpretation**: a semi-transparent fill
 * (so the page and its ambient glow read faintly through the surface), a
 * top-down specular *sheen* gradient, and a luminous hairline edge. Applied
 * only where it reads well — content cards and the floating nav island — never
 * blanket across the app. It gives the layered, lit-from-above glass feel
 * without pretending to be something the platform can't cheaply do.
 */

/** True when the user has opted into Liquid Glass surfaces. Off by default. */
val LocalLedgerGlass = staticCompositionLocalOf { false }

/**
 * Frosted-glass surface treatment for [LedgerCard]-family surfaces and the nav
 * island. Replaces the solid fill + hairline: a translucent base, a specular
 * sheen from the top, and a bright edge. [isDark] tunes the palette — a faint
 * white veil on dark, a heavier frost on light.
 */
fun Modifier.ledgerGlassSurface(shape: Shape, isDark: Boolean): Modifier {
    val baseFill = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.50f)
    val sheen = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))
        } else {
            listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.30f))
        },
    )
    val edge = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.70f)
    return this
        .clip(shape)
        .background(baseFill, shape)
        .background(sheen, shape)
        .border(1.dp, edge, shape)
}
