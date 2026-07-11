package com.sherif.ledger.presentation.dashboard.preview

import com.sherif.ledger.presentation.dashboard.*

object DashboardPreviewData {

    val state = DashboardUiState(
        totalBalance = "$ 120,432.05",
        balanceChangePercentage = "+8%",
        monthlyExpenses = "$ 20,321",
        monthlyExpensesProgress = 0.65f,
        needsReviewCount = 2,
        needsReviewAmount = "50,12",
        categories = listOf(
            CategoryFilterUiModel("all", "All", true),
            CategoryFilterUiModel("electricity", "Electricity"),
            CategoryFilterUiModel("subscription", "Subscription"),
            CategoryFilterUiModel("food", "Food & Drink"),
            CategoryFilterUiModel("groceries", "Groceries")
        ),
        recentActivity = listOf(
            ActivityGroupUiModel(
                title = "Today",
                items = listOf(
                    ActivityItemUiModel("1", "Salary", "Income", "1,200.00", false, "10:45 AM", "ADCB"),
                    ActivityItemUiModel("2", "Carrefour Market", "Groceries", "98.25", true, "10:32 AM", "Expense")
                )
            ),
            ActivityGroupUiModel(
                title = "Yesterday",
                items = listOf(
                    ActivityItemUiModel("3", "ADDC", "Electricity", "323.00", true, "06:30 PM", ""),
                    ActivityItemUiModel("4", "Spotify", "Subscription", "15.99", true, "09:30 AM", "")
                )
            )
        )
    )
}
