package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAvatar
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.transactions.presentation.MerchantCategory
import com.sherif.ledger.feature.transactions.presentation.TransactionState
import com.sherif.ledger.feature.transactions.presentation.TransactionUi

/**
 * Stateless transaction row for the financial timeline.
 *
 * Visual hierarchy: amount (right, colored) > merchant (left, bold) >
 * category and subtitle (quiet). The user should understand each
 * transaction in under one second.
 *
 * State indicators use text, not color alone: "Pending" and
 * "\u21BA Recurring" are readable without color perception.
 *
 * Touch target meets the 56dp accessibility minimum.
 */
@Composable
fun TransactionRow(
    transaction: TransactionUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val catColor = transaction.category.toColor()
    val isIncome = transaction.category == MerchantCategory.Salary
    val amountColor = if (isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense
    val sign = if (isIncome) "+" else "-"
    val dimmed = transaction.state == TransactionState.Pending

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = LedgerSpacing.Group, vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedgerAvatar(
            name = transaction.merchant,
            color = catColor,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.width(LedgerSpacing.Small))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
        ) {
            Text(
                transaction.merchant,
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.label.let { if (dimmed) it.copy(alpha = LedgerTheme.opacity.Secondary) else it },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${transaction.category.displayName()} \u00B7 ${transaction.subtitle}",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(LedgerSpacing.Content))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${sign}AED ${transaction.amount.stripTrailingZeros().toPlainString()}",
                style = LedgerTextStyles.Label,
                color = amountColor.let { if (dimmed) it.copy(alpha = LedgerTheme.opacity.Secondary) else it },
            )
            when (transaction.state) {
                TransactionState.Pending -> Text(
                    "Pending",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.pending,
                )
                TransactionState.Recurring -> Text(
                    "\u21BA Recurring",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.tertiaryLabel,
                )
                TransactionState.Posted -> {}
            }
        }
    }
}

// ---- Category utilities (local to transactions, one consumer) ----

private fun MerchantCategory.toColor(): Color = when (this) {
    MerchantCategory.Grocery -> Color(0xFF22C55E)
    MerchantCategory.Shopping -> Color(0xFFA855F7)
    MerchantCategory.Coffee -> Color(0xFF92400E)
    MerchantCategory.Fuel -> Color(0xFFEF4444)
    MerchantCategory.Salary -> Color(0xFF047857)
    MerchantCategory.Bills -> Color(0xFFEAB308)
    MerchantCategory.Transport -> Color(0xFF3B82F6)
    MerchantCategory.Entertainment -> Color(0xFFEC4899)
    MerchantCategory.Electronics -> Color(0xFF6B7280)
    MerchantCategory.Food -> Color(0xFFF97316)
    MerchantCategory.Healthcare -> Color(0xFF14B8A6)
    MerchantCategory.Travel -> Color(0xFF0EA5E9)
    MerchantCategory.Education -> Color(0xFF8B5CF6)
}

private fun MerchantCategory.displayName(): String = when (this) {
    MerchantCategory.Grocery -> "Groceries"
    MerchantCategory.Shopping -> "Shopping"
    MerchantCategory.Coffee -> "Coffee"
    MerchantCategory.Fuel -> "Fuel"
    MerchantCategory.Salary -> "Income"
    MerchantCategory.Bills -> "Bills"
    MerchantCategory.Transport -> "Transport"
    MerchantCategory.Entertainment -> "Entertainment"
    MerchantCategory.Electronics -> "Electronics"
    MerchantCategory.Food -> "Food"
    MerchantCategory.Healthcare -> "Healthcare"
    MerchantCategory.Travel -> "Travel"
    MerchantCategory.Education -> "Education"
}
