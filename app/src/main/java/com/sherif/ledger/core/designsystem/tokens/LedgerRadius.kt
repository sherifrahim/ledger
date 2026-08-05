package com.sherif.ledger.core.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ledger's corner radii.
 *
 * Reduced alongside the spacing scale: at 24dp a card corner on a 360dp-wide
 * canvas is a visible arc rather than a softened edge, which is most of what made
 * cards and the nav island read as oversized blobs. Softness should be felt, not
 * measured.
 */
object LedgerRadius {
    val SmallDp: Dp = 6.dp
    val MediumDp: Dp = 12.dp
    val LargeDp: Dp = 18.dp
    val XLargeDp: Dp = 24.dp
    val FullDp: Dp = 100.dp

    // Continuous ("squircle") corners rather than circular arcs — see
    // SquircleShape. This one change is what most separates the app's actual
    // look from generic Android Material Design, since every card, surface,
    // chip and button in the app draws its corner from here.
    val Small = SquircleShape(SmallDp)
    val Medium = SquircleShape(MediumDp)
    val Large = SquircleShape(LargeDp)
    val XLarge = SquircleShape(XLargeDp)
    // A pill is already a continuous curve — no corner transition to smooth.
    val Full = RoundedCornerShape(FullDp)
}
