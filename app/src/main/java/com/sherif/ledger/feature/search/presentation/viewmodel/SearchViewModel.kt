package com.sherif.ledger.feature.search.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.isOutflow
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.search.presentation.SearchResultUi
import com.sherif.ledger.feature.search.presentation.SearchUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Universal search over the user's real captured data. Reuses
 * [TransactionReadSource] (and the analytics story layer for the category label)
 * — no separate search engine, no fabricated results (Product Hardening, PART 1).
 * Matches merchant text and amount; empty query yields no results (the screen
 * shows quick-access to real destinations instead).
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val transactionReadSource: TransactionReadSource,
    private val analytics: GetFinancialAnalyticsUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    fun onQueryChange(value: String) { query.value = value }

    private val timeFormat = DateTimeFormatter.ofPattern("d MMM")

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        transactionReadSource.observeAllTransactions(),
    ) { q, result ->
        val term = q.trim()
        if (term.isBlank()) return@combine SearchUiState(query = q)

        val all = (result as? LedgerResult.Success)?.data ?: emptyList()
        val matches = all.filter { txn ->
            txn.rawText?.contains(term, ignoreCase = true) == true ||
                MoneyFormatter.format(txn.amount, includeSymbol = false).contains(term)
        }.sortedByDescending { it.timestamp }.take(40)

        val stories = analytics.transactionStories(matches)
        SearchUiState(
            query = q,
            results = matches.map { txn ->
                SearchResultUi(
                    id = txn.id.toString(),
                    merchant = prettify(txn.rawText ?: "Unknown"),
                    amount = MoneyFormatter.format(txn.amount, includeSymbol = false),
                    isExpense = txn.isOutflow,
                    time = txn.timestamp.atZone(ZoneId.systemDefault()).format(timeFormat),
                    category = stories[txn.id]?.category?.let { prettify(it) } ?: "",
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    private fun prettify(raw: String): String = raw.trim()
        .lowercase()
        .split(Regex("\\s+"))
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        .ifBlank { "Unknown" }
}
