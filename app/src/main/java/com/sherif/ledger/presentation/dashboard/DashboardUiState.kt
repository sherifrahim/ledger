package com.sherif.ledger.presentation.dashboard

data class DashboardUiState(
    val greeting: String,
    val userName: String,
    val currentMonth: String,
    val totalSpent: String,
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
)

data class InsightUiModel(
    val title: String,
    val subtitle: String,
)
