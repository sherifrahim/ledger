package com.sherif.ledger.presentation.dashboard.preview

import com.sherif.ledger.presentation.dashboard.*

object DashboardPreviewData {

    val state = DashboardUiState(
        totalBalance = "$ 120,432.05",
        balanceChangePercentage = "+8%",
        monthlyExpenses = "$ 20,321",
        categories = listOf(
            CategoryFilterUiModel("all", "All", true),
            CategoryFilterUiModel("electricity", "Electricity"),
            CategoryFilterUiModel("subscription", "Subscription"),
            CategoryFilterUiModel("food", "Food & Drink"),
            CategoryFilterUiModel("groceries", "Groceries")
        ),
        intelligenceSummary = listOf(
            "4 Financial Stories matched",
            "2 recurring subscriptions identified",
        ),
        recentActivity = listOf(
            ActivityGroupUiModel(
                title = "Today",
                items = listOf(
                    ActivityItemUiModel("1", "Salary", "Income", "1,200.00", false, "10:45 AM", "Salary received"),
                    ActivityItemUiModel("2", "Carrefour Market", "GROCERIES", "98.25", true, "10:32 AM", "Expense")
                )
            ),
            ActivityGroupUiModel(
                title = "Yesterday",
                items = listOf(
                    ActivityItemUiModel("3", "ADDC", "UTILITIES", "323.00", true, "06:30 PM", "Monthly bill"),
                    ActivityItemUiModel("4", "Spotify", "ENTERTAINMENT", "15.99", true, "09:30 AM", "Recurring subscription")
                )
            )
        )
    )
}

