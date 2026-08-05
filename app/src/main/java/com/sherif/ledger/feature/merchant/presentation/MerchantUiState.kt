package com.sherif.ledger.feature.merchant.presentation

/**
 * Live state for the Merchant relationship page (P2). Every figure is derived from
 * the user's real transactions with this merchant — no fabricated ratings or stats.
 */
data class MerchantUiState(
    val name: String = "",
    val since: String = "",
    val totalSpent: String = "—",
    val txCount: Int = 0,
    val avgMonthly: String = "—",
    val largest: String = "—",
    val currency: String = "AED",
    val insights: List<String> = emptyList(),
    val categories: List<CategorySlice> = emptyList(),
    val related: List<String> = emptyList(),
    // Design review finding F4 (2026-08-06): this page had aggregate stats and
    // nothing else, trailing into a large empty scroll area below them. The
    // actual transactions with this merchant are the obvious content to fill it
    // with — real data this page already loads (`mine` in the ViewModel), just
    // never rendered as a list before now.
    val transactions: List<MerchantTransactionUi> = emptyList(),
    val loaded: Boolean = false,
)

data class CategorySlice(val label: String, val fraction: Float)

data class MerchantTransactionUi(
    val id: String,
    val amount: String,
    val isExpense: Boolean,
    val dateLabel: String,
    val time: String,
)
