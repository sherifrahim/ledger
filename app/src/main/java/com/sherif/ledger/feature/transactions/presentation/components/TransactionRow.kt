package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerTransactionRow
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.transactions.presentation.MerchantCategory
import com.sherif.ledger.feature.transactions.presentation.TransactionState
import com.sherif.ledger.feature.transactions.presentation.TransactionUi

/**
 * Stateless transaction row proxying to canonical [LedgerTransactionRow].
 */
@Composable
fun TransactionRow(
    transaction: TransactionUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val isIncome = transaction.category == MerchantCategory.Salary
    val amountColor = if (isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense
    val sign = if (isIncome) "+" else "-"
    val status = when (transaction.state) {
        TransactionState.Pending -> "\u25CF Pending"
        TransactionState.Recurring -> "\u21BA Monthly"
        else -> null
    }

    LedgerTransactionRow(
        title = transaction.merchant,
        amount = "${sign}AED ${transaction.amount.stripTrailingZeros().toPlainString()}",
        subtitle = transaction.subtitle,
        metadata = transaction.category.displayName(),
        status = status,
        amountColor = amountColor,
        dimmed = transaction.state == TransactionState.Pending,
        onClick = onClick,
        modifier = modifier
    )
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
