package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles

/**
 * Standard LDS row for displaying transaction-like summaries.
 *
 * This is a presentational component only. It accepts display strings and does
 * not depend on transaction entities, repositories, parsers, or ViewModels.
 */
@Composable
fun LedgerTransactionRow(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metadata: String? = null,
    tag: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(LedgerSpacing.XxLarge)
                .clip(MaterialTheme.shapes.small)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                style = LedgerTextStyles.Label,
                color = accentColor,
            )
        }
        Spacer(modifier = Modifier.width(LedgerSpacing.Medium))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
        ) {
            Text(
                text = title,
                style = LedgerTextStyles.Label,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val supportingText = listOfNotNull(subtitle, metadata).joinToString(" - ")
            if (supportingText.isNotEmpty()) {
                Text(
                    text = supportingText,
                    style = LedgerTextStyles.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (tag != null) {
                LedgerTag(text = tag)
            }
        }
        Spacer(modifier = Modifier.width(LedgerSpacing.Medium))
        LedgerAmount(amount = amount)
    }
}
