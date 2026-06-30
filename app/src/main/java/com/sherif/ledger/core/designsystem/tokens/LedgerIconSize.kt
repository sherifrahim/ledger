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
    val Small: Dp = 16.dp
    val Medium: Dp = 20.dp
    val Large: Dp = 24.dp
    val XLarge: Dp = 32.dp
    val Huge: Dp = 48.dp
    val Massive: Dp = 64.dp
    val Navigation: Dp = 20.dp // Reduced for RC-003 to feel lighter
}
