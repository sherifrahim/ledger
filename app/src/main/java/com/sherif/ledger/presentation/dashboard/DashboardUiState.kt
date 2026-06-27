package com.sherif.ledger.presentation.dashboard

data class DashboardUiState(
    val greeting: String,
    val userName: String,
    val currentMonth: String,
    val totalSpent: String,
    val balanceAmount: String = "",
    val balanceCurrency: String = "AED",
    val budgetProgress: Float,
    val expense: String,
    val income: String,
    val savings: String,
    val recentTransactions: List<TransactionUiModel>,
    val insights: List<InsightUiModel>,
)

data class TransactionUiModel(
    val merchant: String,
    val category: String,
    val amount: String,
    val isExpense: Boolean = true,
    val date: String = "Today",
)

data class InsightUiModel(
    val title: String,
    val subtitle: String,
    val indicator: String = "",
)
