package com.sherif.ledger.feature.story.presentation

/**
 * The Financial Story feed — a chronological narrative of the user's real
 * captured activity. Each item pairs a transaction with the relationship-derived
 * explanation the intelligence engine already produces (e.g. "Recurring
 * subscription", "Salary received"). Nothing here is fabricated: an empty feed
 * renders the honest empty state, never sample stories.
 */
data class StoryUiState(val groups: List<StoryGroupUi> = emptyList())

data class StoryGroupUi(val title: String, val items: List<StoryItemUi>)

data class StoryItemUi(
    val id: String,
    val merchant: String,
    val explanation: String,
    val amount: String,
    val isExpense: Boolean,
)
