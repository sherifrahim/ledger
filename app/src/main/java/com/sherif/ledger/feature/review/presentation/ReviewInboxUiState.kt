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
    // The ORIGINAL raw SMS/notification text (not [merchant], which may be a
    // title-cased display fallback) — the key a category correction must be
    // learned against so MerchantResolver's own normalization matches it later.
    val rawMerchantText: String? = null,
    // The verbatim captured message (Transaction.rawText) — genuinely different
    // from [rawMerchantText] above, which can itself be an extracted/title-cased
    // name for newer rows (see Transaction.merchantOrRawText). Real user testing:
    // "user doesn't understand in one look which transaction the app is
    // referring to" — shown on the card so a person can just read the actual
    // bank SMS and categorize from that, the way they naturally would.
    val rawMessage: String? = null,
)
