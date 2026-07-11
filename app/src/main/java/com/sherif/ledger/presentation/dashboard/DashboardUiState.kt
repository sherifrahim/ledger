package com.sherif.ledger.presentation.dashboard

import com.sherif.ledger.feature.transactions.presentation.MerchantCategory

data class DashboardUiState(
    val totalBalance: String = "0.00",
    val balanceChangePercentage: String = "0%",
    val monthlyExpenses: String = "0.00",
    val monthlyExpensesProgress: Float = 0f,
    val needsReviewCount: Int = 0,
    val needsReviewAmount: String = "0.00",
    val categories: List<CategoryFilterUiModel> = emptyList(),
    val recentActivity: List<ActivityGroupUiModel> = emptyList(),
    // V2 Compatibility
    val insights: List<InsightUiModel> = emptyList()
)

data class CategoryFilterUiModel(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)

data class ActivityGroupUiModel(
    val title: String, // Today, Yesterday
    val items: List<ActivityItemUiModel>
)

data class ActivityItemUiModel(
    val id: String,
    val merchantName: String,
    val category: String,
    val amount: String,
    val isExpense: Boolean,
    val time: String,
    val explanation: String = ""
)

data class InsightUiModel(
    val title: String,
    val subtitle: String,
    val indicator: String = "",
)

// V2 Compatibility
data class TransactionUiModel(
    val merchant: String,
    val category: String,
    val amount: String,
    val isExpense: Boolean = true
)
