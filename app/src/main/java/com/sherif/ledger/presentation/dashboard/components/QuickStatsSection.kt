package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerStatCard
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun QuickStatsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
    ) {
        LedgerStatCard(
            modifier = Modifier.weight(1f),
            label = "Expense",
            value = state.expense,
            valueColor = LedgerTheme.colors.expense,
        )
        LedgerStatCard(
            modifier = Modifier.weight(1f),
            label = "Income",
            value = state.income,
            valueColor = LedgerTheme.colors.income,
        )
        LedgerStatCard(
            modifier = Modifier.weight(1f),
            label = "Savings",
            value = state.savings,
            valueColor = LedgerTheme.colors.label,
        )
    }
}
