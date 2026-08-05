package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * The screen-title header: back button, headline, optional subtitle, optional
 * trailing actions.
 *
 * A sweep of the app found this exact shape hand-rolled independently in nine
 * different screens — each a few pixels off from the next (some at
 * [LedgerTextStyles.Headline], one at the smaller [LedgerTextStyles.Title];
 * some starting the back button and title on the same row, one stacking them
 * on separate rows; two screens using an entirely different component,
 * [LedgerTopBar], a small centered 56dp bar unrelated in size or alignment to
 * what every other screen had converged on independently) — which is what made
 * the app read as an inconsistent patchwork even after density and spacing
 * were already fixed. This is that shape, written once.
 *
 * Deliberately NOT used everywhere a screen has a title: a hero identity block
 * (a merchant's own name as the page's masthead, a transaction's amount, a
 * greeting with the user's avatar) is a different kind of header with its own
 * reasons for its own layout, and forcing it through this component would be
 * the same mistake as the inconsistency this fixes — just in the other
 * direction.
 */
@Composable
fun LedgerScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LedgerTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null) {
            LedgerIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBackClick,
                contentDescription = "Back",
                tint = colors.textPrimary,
            )
            Spacer(Modifier.width(LedgerSpacing.Small))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = LedgerTextStyles.Headline, color = colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = LedgerTextStyles.BodyMedium, color = colors.textSecondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}
