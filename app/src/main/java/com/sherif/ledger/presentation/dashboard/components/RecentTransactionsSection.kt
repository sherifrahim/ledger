package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerSectionTitle
import com.sherif.ledger.core.designsystem.component.LedgerTransactionRow
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun RecentTransactionsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        LedgerSectionTitle(
            text = "Recent Activity",
        )

        state.recentTransactions.forEach {

            LedgerTransactionRow(
                title = it.merchant,
                subtitle = it.category,
                amount = it.amount,
            )

        }

    }
}
