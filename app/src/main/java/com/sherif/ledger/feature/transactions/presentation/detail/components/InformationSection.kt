package com.sherif.ledger.feature.transactions.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Key-value information group inside a grouped surface.
 *
 * Renders label-value pairs separated by hairlines inside a
 * LedgerSurface. Future consumers: Review Inbox (parsed fields)
 * and Merchant Profile (merchant metadata).
 */
@Composable
fun InformationSection(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Inline),
    ) {
        items.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Small),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                Text(value, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
            }
            if (index != items.lastIndex) {
                LedgerHairline()
            }
        }
    }
}
