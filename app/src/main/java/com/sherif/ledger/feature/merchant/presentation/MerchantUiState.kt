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
    val loaded: Boolean = false,
)

data class CategorySlice(val label: String, val fraction: Float)
