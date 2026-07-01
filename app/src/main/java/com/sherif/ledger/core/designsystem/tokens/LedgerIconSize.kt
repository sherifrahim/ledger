package com.sherif.ledger.core.designsystem.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LDL icon size vocabulary.
 *
 * Components reference these instead of raw dp so icon density stays
 * consistent across screens.
 */
object LedgerIconSize {
    val Small: Dp = 12.dp
    val Medium: Dp = 14.dp
    val Large: Dp = 18.dp
    val XLarge: Dp = 24.dp
    val Huge: Dp = 36.dp // Reduced for RC-009 (~20% reduction from 44dp)
    val Massive: Dp = 48.dp // Reduced for RC-009
    val Navigation: Dp = 20.dp
}
