package com.sherif.ledger.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ledger's spacing scale.
 *
 * Previously described as "architectural and generous", and it was — but generous
 * against what? The owner's phone runs a 480dpi override, so the logical canvas is
 * 360dp wide, and on that canvas a 24dp screen inset plus 32dp section gaps left
 * Ledger looking noticeably heavier than every other app beside it. The system
 * density already expresses the user's size preference; a design system that adds
 * its own generosity on top is double-counting it.
 *
 * The scale keeps its shape — same ratios, same semantic names — at roughly 80% of
 * the former values. Everything is expressed through these tokens, so this is the
 * one place the app's overall density is set.
 */
object LedgerSpacing {
    val Atomic: Dp = 4.dp
    val Tiny: Dp = 6.dp
    val Small: Dp = 10.dp
    val Medium: Dp = 14.dp
    val Large: Dp = 20.dp
    val XLarge: Dp = 26.dp
    val Huge: Dp = 38.dp
    val Massive: Dp = 52.dp

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
