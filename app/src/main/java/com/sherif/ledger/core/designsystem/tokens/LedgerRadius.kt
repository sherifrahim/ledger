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

    val Small = RoundedCornerShape(SmallDp)
    val Medium = RoundedCornerShape(MediumDp)
    val Large = RoundedCornerShape(LargeDp)
    val XLarge = RoundedCornerShape(XLargeDp)
    val Full = RoundedCornerShape(FullDp)
}
