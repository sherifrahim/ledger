package com.sherif.ledger.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ledger V3 Spacing Scale (Architectural & Generous)
 * 
 * Inspired by Apple Journal and premium editorial layouts.
 */
object LedgerSpacing {
    val Atomic: Dp = 4.dp
    val Tiny: Dp = 8.dp
    val Small: Dp = 12.dp
    val Medium: Dp = 16.dp
    val Large: Dp = 24.dp
    val XLarge: Dp = 32.dp
    val Huge: Dp = 48.dp
    val Massive: Dp = 64.dp

    // V2 Compatibility Aliases
    val XxSmall = Atomic
    val XSmall = Tiny
    val XxLarge = Huge
    val XxxLarge = Massive
    
    val Inline = Tiny
    val Content = Medium
    val Group = Large
    val Section = XLarge
    val Screen = Large
    val ScreenBottom = Massive
    val AvatarIndent = Huge

    // Semantic Aliases
    val ScreenPadding = Large
    val SectionGap = XLarge
    val ContentGap = Medium
    val InlineGap = Tiny
}
