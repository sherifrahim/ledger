package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerStatMetric
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

/**
 * Quick stats row.
 *
 * Removes the surface container to allow the metrics to sit directly
 * on the atmosphere. This reduces visual noise and emphasizes the
 * "Instrument" feel.
 */
@Composable
fun QuickStatsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        LedgerStatMetric("Income", state.income, valueColor = LedgerTheme.colors.income)
        LedgerStatMetric("Expense", state.expense, valueColor = LedgerTheme.colors.expense)
        LedgerStatMetric("Savings", state.savings, valueColor = LedgerTheme.colors.secondaryLabel)
    }
}
