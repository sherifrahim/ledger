package com.sherif.ledger.feature.search.presentation

/**
 * State for Universal Search. Results are real captured transactions matched
 * against the query — there is no fabricated recent-search or suggestion data.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<SearchResultUi> = emptyList(),
)

data class SearchResultUi(
    val id: String,
    val merchant: String,
    val amount: String,
    val isExpense: Boolean,
    val time: String,
    val category: String,
)
