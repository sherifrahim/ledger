package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.LedgerTransactionRow
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

/**
 * Recent activity list.
 *
 * Removes the surrounding card surface. The hierarchy is established
 * through vertical rhythm and refined hairline separators.
 */
@Composable
fun RecentTransactionsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
        LedgerSectionHeader(
            title = "RECENT ACTIVITY",
            titleColor = LedgerTheme.colors.tertiaryLabel,
            trailing = "See all",
            onTrailingClick = onSeeAllClick,
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            state.recentTransactions.forEachIndexed { index, txn ->
                val sign = if (txn.isExpense) "-" else "+"
                LedgerTransactionRow(
                    title = txn.merchant,
                    subtitle = txn.category,
                    amount = "$sign${txn.amount}",
                    amountColor = if (txn.isExpense) LedgerTheme.colors.expense else LedgerTheme.colors.income,
                    accentColor = Color(txn.merchantAccentHue),
                )
                if (index != state.recentTransactions.lastIndex) {
                    LedgerHairline(modifier = Modifier.padding(start = LedgerSpacing.AvatarIndent))
                }
            }
        }
    }
}
