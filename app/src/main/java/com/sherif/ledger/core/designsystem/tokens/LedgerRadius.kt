package com.sherif.ledger.core.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LDL corner radius vocabulary.
 *
 * Pre-built shapes for Compose modifiers plus raw Dp values for cases
 * where a shape object is not accepted (e.g. lerp interpolation in the
 * collapsing hero). This is the single authority for LDL radii.
 *
 * Note: [com.sherif.ledger.core.designsystem.theme.LedgerShapes] feeds
 * Material components via the Material Shapes API and carries its own
 * values. Those are toolkit plumbing, not LDL identity.
 */
object LedgerRadius {
    val SmallDp: Dp = 16.dp
    val MediumDp: Dp = 24.dp
    val LargeDp: Dp = 32.dp
    val XLargeDp: Dp = 40.dp
    val FullDp: Dp = 100.dp

    val Small = RoundedCornerShape(SmallDp)
    val Medium = RoundedCornerShape(MediumDp)
    val Large = RoundedCornerShape(LargeDp)
    val XLarge = RoundedCornerShape(XLargeDp)
    val Full = RoundedCornerShape(FullDp)
}
