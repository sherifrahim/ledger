package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.*
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

/**
 * Ledger V3 Transaction Activity Row
 * 
 * Focuses on Intelligence: Explains the event rather than just listing it.
 */
@Composable
fun LedgerTransactionRow(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    explanation: String = "", // "Intelligence Layer"
    currency: String = "AED",
    isExpense: Boolean = true,
    subtitle: String? = null,
    metadata: String? = null,
    tag: String? = null,
    status: String? = null,
    amountColor: Color? = null,
    dimmed: Boolean = false,
    time: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LedgerTheme.colors
    val finalExplanation = explanation.ifEmpty { subtitle ?: "" }
    val finalAmountColor = amountColor ?: if (isExpense) colors.textPrimary else colors.positive

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.ledgerClickable(onClick = onClick) else Modifier)
            .padding(vertical = LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)
    ) {
        // 1. Merchant Identity
        LedgerBrandIcon(
            name = title,
            size = 40.dp
        )

        // 2. Narrative Intelligence
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (finalExplanation.isNotEmpty()) {
                Text(
                    text = finalExplanation,
                    style = LedgerTextStyles.Label,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }

        // 3. Authority of Amount
        Column(horizontalAlignment = Alignment.End) {
            LedgerAmount(
                amount = (if (isExpense && !amount.startsWith("-")) "-" else if (!isExpense && !amount.startsWith("+")) "+" else "") + amount,
                currency = currency,
                style = LedgerAmountStyle.Regular,
                color = finalAmountColor
            )
            if (time != null || status != null) {
                Text(
                    text = time ?: status ?: "",
                    style = LedgerTextStyles.Caption,
                    color = colors.textTertiary
                )
            }
        }
    }
}
