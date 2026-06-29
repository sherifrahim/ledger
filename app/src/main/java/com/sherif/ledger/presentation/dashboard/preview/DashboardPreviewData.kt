package com.sherif.ledger.presentation.dashboard.preview

import com.sherif.ledger.presentation.dashboard.DashboardUiState
import com.sherif.ledger.presentation.dashboard.InsightUiModel
import com.sherif.ledger.presentation.dashboard.TransactionUiModel

object DashboardPreviewData {

    val state = DashboardUiState(
        greeting = "Good morning",
        userName = "Sherif",
        currentMonth = "June",
        totalSpent = "AED 2,840.25",
        balanceAmount = "2,840.25",
        balanceCurrency = "AED",
        budgetProgress = 0.62f,
        expense = "AED 1,950",
        expenseChange = "↑ 8%",
        income = "AED 5,200",
        incomeChange = "↑ 12%",
        savings = "AED 3,250",
        savingsChange = "↑ 5%",
        recentTransactions = listOf(
            TransactionUiModel("Amazon", "Shopping", "AED 52", isExpense = true, date = "Today", merchantAccentHue = 0xFF232F3E),
            TransactionUiModel("Carrefour", "Groceries", "AED 126", isExpense = true, date = "Today", merchantAccentHue = 0xFF0058A8),
            TransactionUiModel("Costa Coffee", "Coffee", "AED 19", isExpense = true, date = "Yesterday", merchantAccentHue = 0xFF830025),
            TransactionUiModel("Salary", "Income", "AED 5,200", isExpense = false, date = "May 31", merchantAccentHue = 0xFF047857),
        ),
        insights = listOf(
            InsightUiModel("Food spending", "12% lower than last week", "\u2193 12%"),
            InsightUiModel("Subscriptions", "Netflix renews tomorrow", ""),
        ),
    )
}
