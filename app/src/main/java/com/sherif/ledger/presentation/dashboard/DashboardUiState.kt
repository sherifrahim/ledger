package com.sherif.ledger.presentation.dashboard

import com.sherif.ledger.feature.transactions.presentation.MerchantCategory

data class DashboardUiState(
    val totalBalance: String = "0.00",
    // Null when a month-over-month comparison genuinely can't be computed (no
    // prior-period data, or a non-finite result) — never a fabricated fallback
    // value. The UI hides the change badge entirely when this is null.
    val balanceChangePercentage: String? = null,
    val monthlyExpenses: String = "0.00",
    val categories: List<CategoryFilterUiModel> = emptyList(),
    val recentActivity: List<ActivityGroupUiModel> = emptyList(),
    // Real, backend-derived facts about relationships found this period. See
    // FinancialAnalytics.intelligenceSummary. Empty when nothing was found yet —
    // never a fabricated confidence figure or static placeholder text.
    val intelligenceSummary: List<String> = emptyList(),
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

