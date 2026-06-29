package com.sherif.ledger.feature.review.presentation

/**
 * Presentation state for the Review Inbox.
 */
data class ReviewInboxUiState(
    val items: List<ReviewItemUi>,
    val selectedFilter: ReviewFilter = ReviewFilter.All,
    val pendingCount: Int = 0,
    val confirmedTodayCount: Int = 0,
    val ignoredTodayCount: Int = 0,
)

enum class ReviewFilter(val label: String) {
    All("All"),
    LowConfidence("Low"),
    MediumConfidence("Medium"),
    HighConfidence("High"),
}

data class ReviewItemUi(
    val id: String,
    val merchant: String,
    val merchantCategory: String,
    val merchantAccentHue: Long,
    val amount: String,
    val isIncome: Boolean = false,
    val suggestedCategory: String,
    val suggestedAccount: String,
    val confidence: Int,
    val reason: String,
    val timestamp: String,
)
