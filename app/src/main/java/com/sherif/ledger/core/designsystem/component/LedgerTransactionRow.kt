package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Standard LDL row for transaction-like summaries.
 *
 * Single authority for activity representation. Supports status
 * indicators, tags, and multi-line metadata.
 */
@Composable
fun LedgerTransactionRow(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metadata: String? = null,
    tag: String? = null,
    status: String? = null,
    amountColor: Color = LedgerTheme.colors.label,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val contentAlpha = if (dimmed) LedgerTheme.opacity.Secondary else 1.0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.ledgerClickable(onClick = onClick) else Modifier)
            .padding(vertical = LedgerSpacing.Medium, horizontal = LedgerSpacing.Small), // Optimized padding
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Authentic Brand Identity (RC-007 System)
        LedgerBrandIcon(
            name = title,
            size = LedgerTheme.iconSize.Huge,
            modifier = Modifier
                .graphicsLayer { alpha = contentAlpha }
                .padding(4.dp) // Refined asset normalization
        )

        Spacer(modifier = Modifier.width(LedgerSpacing.Medium))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
        ) {
            Text(
                text = title,
                style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                color = LedgerTheme.colors.label.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val supportingText = listOfNotNull(subtitle, metadata).joinToString(" \u00B7 ")
            if (supportingText.isNotEmpty()) {
                Text(
                    text = supportingText,
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.tertiaryLabel.copy(alpha = 0.50f), // Sharper hierarchy
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.width(LedgerSpacing.Medium))
        
        Column(horizontalAlignment = Alignment.End) {
            LedgerAmount(
                amount = amount,
                color = amountColor.copy(alpha = contentAlpha),
                style = LedgerAmountStyle.Regular,
            )
            
            if (status != null) {
                Text(
                    text = status,
                    style = LedgerTextStyles.Caption,
                    color = if (status.contains("Pending")) LedgerTheme.colors.pending else LedgerTheme.colors.tertiaryLabel,
                )
            } else if (tag != null) {
                Spacer(Modifier.padding(top = LedgerSpacing.XxSmall))
                LedgerTag(text = tag)
            }
        }
    }
}
