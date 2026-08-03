package com.sherif.ledger.core.designsystem.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much space the floating navigation island occupies at the bottom of the
 * screen, including the system gesture inset beneath it.
 *
 * The island is an overlay — it is drawn on top of the nav host rather than
 * inside a Scaffold slot — which is deliberate, because content is meant to
 * scroll and frost underneath it. The cost is that nothing tells a scrolling
 * screen how tall it is, so every screen was carrying its own guess
 * (`LedgerSpacing.ScreenBottom + 100.dp`, repeated in four files) and
 * `ProfileScreen` was carrying none at all, which left the last row of Settings
 * permanently unreachable behind the island.
 *
 * Measured once from the real composable and published here, so the number can
 * never drift from the thing it is describing: change the island's padding, icon
 * size or label and every screen's bottom inset follows automatically.
 *
 * Defaults to 0.dp, which is correct for the pushed secondary screens that have
 * no island at all — they get their own gesture-bar padding from
 * [ledgerScreenBottomPadding] instead.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

/**
 * The bottom content padding a scrolling screen should use.
 *
 * Whichever obstruction is taller — the navigation island on a tab screen, or the
 * bare system gesture bar on a pushed secondary screen — plus a breath of space so
 * the last row never sits flush against it. `max` rather than a sum because the
 * island already includes the gesture inset in its own height; adding them would
 * strand a blank band under every list.
 */
val ledgerScreenBottomPadding: Dp
    @androidx.compose.runtime.Composable
    get() {
        val systemBars = WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
        return maxOf(LocalBottomBarInset.current, systemBars) + LedgerSpacing.Large
    }
