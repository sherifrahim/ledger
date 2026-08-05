package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.LocalCardHazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * A compact, centered-title bar — kept specifically for dense action toolbars
 * where several trailing icon buttons need the room, not for ordinary screen
 * headers.
 *
 * Every real screen in the app uses [LedgerScreenHeader] instead: a sweep
 * found this bar's small centered title (12sp, in a fixed 56dp band) sitting
 * beside eight-plus screens that had each independently converged on a large
 * left-aligned headline with an optional back button and subtitle — one of
 * several inconsistent "top bar treatments" across the app. Every user-facing
 * screen was migrated. The one caller left is the debug-only Developer
 * Console, which packs six trailing action icons into its bar; forcing that
 * screen through [LedgerScreenHeader]'s large headline would leave those
 * icons fighting a 22sp title for room on a single row. Do not add a new
 * user-facing screen here — use [LedgerScreenHeader].
 */
@Composable
fun LedgerTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LedgerTheme.colors
    val cardHaze = LocalCardHazeState.current
    val glass = LedgerTheme.glass && cardHaze != null

    // Under Liquid Glass the top bar becomes a frosted band (iOS-style chrome);
    // otherwise it stays a flat, transparent extension of the page.
    val glassMod = if (glass) {
        Modifier.hazeEffect(cardHaze!!, HazeMaterials.thin(colors.surfaceBase))
    } else {
        Modifier.background(Color.Transparent)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(glassMod)
            .padding(horizontal = LedgerSpacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        if (navigationIcon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart),
            ) {
                navigationIcon()
            }
        }

        Text(
            text = title,
            style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
            color = colors.label,
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions()
        }
    }
}
