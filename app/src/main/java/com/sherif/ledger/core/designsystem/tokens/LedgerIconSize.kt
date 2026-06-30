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
    val Medium: Dp = 18.dp // Reduced from 20
    val Large: Dp = 20.dp // Reduced from 24
    val XLarge: Dp = 28.dp // Reduced from 32
    val Huge: Dp = 44.dp // Reduced from 48
    val Massive: Dp = 56.dp // Reduced from 64
    val Navigation: Dp = 20.dp
}
