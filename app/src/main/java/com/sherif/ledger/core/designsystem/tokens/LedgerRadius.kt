package com.sherif.ledger.core.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ledger V3 Radius Scale (Soft Precision)
 */
object LedgerRadius {
    val SmallDp: Dp = 8.dp
    val MediumDp: Dp = 16.dp
    val LargeDp: Dp = 24.dp
    val XLargeDp: Dp = 32.dp
    val FullDp: Dp = 100.dp

    val Small = RoundedCornerShape(SmallDp)
    val Medium = RoundedCornerShape(MediumDp)
    val Large = RoundedCornerShape(LargeDp)
    val XLarge = RoundedCornerShape(XLargeDp)
    val Full = RoundedCornerShape(FullDp)
}
