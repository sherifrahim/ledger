package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun QuickStatsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatMetric("Expense", state.expense, LedgerTheme.colors.expense)
        StatMetric("Income", state.income, LedgerTheme.colors.income)
        StatMetric("Savings", state.savings, LedgerTheme.colors.label)
    }
}

@Composable
private fun StatMetric(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
        Spacer(Modifier.height(LedgerSpacing.XxSmall))
        Text(
            text = value,
            style = LedgerTextStyles.Section,
            color = color,
        )
    }
}
