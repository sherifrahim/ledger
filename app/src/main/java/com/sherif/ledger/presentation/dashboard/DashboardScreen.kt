package com.sherif.ledger.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.presentation.dashboard.components.GreetingHeader
import com.sherif.ledger.presentation.dashboard.components.InsightSection
import com.sherif.ledger.presentation.dashboard.components.MonthlySummaryCard
import com.sherif.ledger.presentation.dashboard.components.QuickStatsSection
import com.sherif.ledger.presentation.dashboard.components.RecentTransactionsSection
import com.sherif.ledger.presentation.dashboard.preview.DashboardPreviewData

@Composable
fun DashboardScreen(
    state: DashboardUiState = DashboardPreviewData.state,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {

        GreetingHeader(
            greeting = state.greeting,
            userName = state.userName,
        )

        MonthlySummaryCard(
            month = state.currentMonth,
            totalSpent = state.totalSpent,
            progress = state.budgetProgress,
        )

        QuickStatsSection(state)

        RecentTransactionsSection(state)

        InsightSection(state)
    }
}
