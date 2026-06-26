package com.sherif.ledger.presentation.dashboard.preview

import com.sherif.ledger.presentation.dashboard.*

object DashboardPreviewData {

    val state = DashboardUiState(

        greeting = "Good Evening",

        userName = "Sherif",

        currentMonth = "June Spending",

        totalSpent = "AED 2,840.25",

        budgetProgress = 0.62f,

        expense = "AED 1,950",

        income = "AED 5,200",

        savings = "AED 3,250",

        recentTransactions = listOf(

            TransactionUiModel(
                "Amazon",
                "Shopping",
                "AED 52"
            ),

            TransactionUiModel(
                "Carrefour",
                "Groceries",
                "AED 126"
            ),

            TransactionUiModel(
                "Costa Coffee",
                "Coffee",
                "AED 19"
            )

        ),

        insights = listOf(

            InsightUiModel(
                "Food spending",
                "12% lower than last week"
            ),

            InsightUiModel(
                "Subscriptions",
                "Netflix renews tomorrow"
            )

        )

    )

}
