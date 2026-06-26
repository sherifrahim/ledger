package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerStatCard
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun QuickStatsSection(

    state: DashboardUiState

) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.SpaceBetween

    ) {

        LedgerStatCard(

            modifier = Modifier.weight(1f),

            label = "Expense",

            value = state.expense

        )

        LedgerStatCard(

            modifier = Modifier.weight(1f),

            label = "Income",

            value = state.income

        )

        LedgerStatCard(

            modifier = Modifier.weight(1f),

            label = "Savings",

            value = state.savings

        )

    }

}
