package com.sherif.ledger.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LDL spacing scale.
 *
 * Numeric names remain for compatibility. Semantic aliases describe purpose
 * so future screens read as intent rather than measurement.
 */
object LedgerSpacing {
    val XxSmall: Dp = 4.dp
    val XSmall: Dp = 8.dp
    val Small: Dp = 12.dp
    val Medium: Dp = 16.dp
    val Large: Dp = 20.dp
    val XLarge: Dp = 24.dp
    val XxLarge: Dp = 32.dp
    val XxxLarge: Dp = 40.dp
    val Huge: Dp = 48.dp
    val Massive: Dp = 64.dp

    /** Inline padding within a single line or between icon and text. */
    val Inline: Dp = XxSmall
    /** Padding within a content group (between label and value). */
    val Content: Dp = XSmall
    /** Gap between related items inside a section. */
    val Group: Dp = Medium
    /** Screen edge inset. */
    val Screen: Dp = Large
    /** Vertical gap between major screen sections. */
    val Section: Dp = 28.dp
    /** Bottom safe area padding. */
    val ScreenBottom: Dp = XxxLarge

    /** Standard indent for list items with avatars (IconLarge + Medium Spacing). */
    val AvatarIndent: Dp = 56.dp
}
