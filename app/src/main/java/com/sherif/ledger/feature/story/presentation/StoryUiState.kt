package com.sherif.ledger.feature.story.presentation

/**
 * The Financial Story feed — a chronological narrative of the user's real
 * captured activity. Each item pairs a transaction with the relationship-derived
 * explanation the intelligence engine already produces (e.g. "Recurring
 * subscription", "Salary received"). Nothing here is fabricated: an empty feed
 * renders the honest empty state, never sample stories.
 */
data class StoryUiState(
    val groups: List<StoryGroupUi> = emptyList(),
    /**
     * Design review finding F5 (2026-08-06): before this, Story rendered the
     * exact same chronological list as Dashboard's Recent Activity — same
     * fields, same grouping, no narrative framing a screen literally named
     * "Story" ought to have. A plain-language weekly summary, built from the
     * same [com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase]
     * category totals Dashboard already reads elsewhere — never a separate
     * fabricated engine. Null when there's fewer than 2 transactions this week
     * (nothing worth summarizing yet), never a placeholder sentence.
     */
    val weeklyNarrative: String? = null,
)

data class StoryGroupUi(val title: String, val items: List<StoryItemUi>)

data class StoryItemUi(
    val id: String,
    val merchant: String,
    val explanation: String,
    val amount: String,
    val isExpense: Boolean,
)
