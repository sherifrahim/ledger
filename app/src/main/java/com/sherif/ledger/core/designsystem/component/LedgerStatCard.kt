package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles

/**
 * Compact LDS card for displaying one dashboard metric.
 *
 * Pass preformatted values from the screen layer; this component does not
 * calculate balances, totals, or trends.
 */
@Composable
fun LedgerStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    LedgerCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XSmall),
        ) {
            Text(
                text = label,
                style = LedgerTextStyles.Label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LedgerAmount(
                amount = value,
                style = LedgerAmountStyle.Large,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = LedgerTextStyles.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
