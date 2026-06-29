package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Standard LDL row for transaction-like summaries.
 *
 * Designed with a "Typography-First" architecture. Removed the standard
 * "initial-in-box" Android convention to prioritize information density
 * and clean rhythm.
 */
@Composable
fun LedgerTransactionRow(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metadata: String? = null,
    tag: String? = null,
    accentColor: Color = LedgerTheme.colors.tint,
    amountColor: Color = LedgerTheme.colors.label,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .ledgerClickable { /* TODO */ }
            .padding(vertical = LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
        ) {
            Text(
                text = title,
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.label,
            )
            val supportingText = listOfNotNull(subtitle, metadata).joinToString(" - ")
            if (supportingText.isNotEmpty()) {
                Text(
                    text = supportingText,
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.secondaryLabel,
                )
            }
        }
        
        Spacer(modifier = Modifier.width(LedgerSpacing.Medium))
        
        Column(horizontalAlignment = Alignment.End) {
            LedgerAmount(
                amount = amount,
                color = amountColor,
                style = LedgerAmountStyle.Regular,
            )
            if (tag != null) {
                Spacer(Modifier.padding(top = LedgerSpacing.XxSmall))
                LedgerTag(text = tag)
            }
        }
    }
}
