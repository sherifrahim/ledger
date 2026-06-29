package com.sherif.ledger.core.designsystem.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL standard metadata row.
 *
 * Used in details screens to present key-value facts with high
 * typographic precision.
 */
@Composable
fun LedgerMetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = LedgerTextStyles.Label,
            color = LedgerTheme.colors.secondaryLabel,
        )
        Text(
            text = value,
            style = LedgerTextStyles.Body,
            color = LedgerTheme.colors.label,
        )
    }
}
