package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerStatMetric
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun QuickStatsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(vertical = LedgerSpacing.Medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LedgerStatMetric("Income", state.income, valueColor = LedgerTheme.colors.income)
            LedgerStatMetric("Expense", state.expense, valueColor = LedgerTheme.colors.expense)
            LedgerStatMetric("Savings", state.savings, valueColor = LedgerTheme.colors.secondaryLabel)
        }
    }
}
