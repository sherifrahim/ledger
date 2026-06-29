package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.LedgerTransactionRow
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun RecentTransactionsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recent activity", style = LedgerTextStyles.Section, color = LedgerTheme.colors.label)
            Text(
                "See all",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tint,
                modifier = if (onSeeAllClick != null) Modifier.clickable(onClick = onSeeAllClick) else Modifier,
            )
        }

        LedgerSurface(level = LedgerSurfaceLevel.Level1, contentPadding = PaddingValues(horizontal = LedgerSpacing.Medium)) {
            state.recentTransactions.forEachIndexed { index, txn ->
                val sign = if (txn.isExpense) "-" else "+"
                LedgerTransactionRow(
                    title = txn.merchant,
                    subtitle = txn.category,
                    amount = "$sign${txn.amount}",
                    amountColor = if (txn.isExpense) LedgerTheme.colors.expense else LedgerTheme.colors.income,
                )
                if (index != state.recentTransactions.lastIndex) {
                    LedgerHairline()
                }
            }
        }
    }
}
