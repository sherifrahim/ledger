package com.sherif.ledger.feature.storygraph.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.designsystem.component.graph.GraphData
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.merchantOrRawText
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.BudgetRepository
import com.sherif.ledger.core.domain.repository.GoalRepository
import com.sherif.ledger.core.domain.repository.TagRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.service.transaction.TransactionDisplayName
import com.sherif.ledger.feature.merchant.MerchantResolver
import com.sherif.ledger.feature.storygraph.StoryGraphBuilder
import com.sherif.ledger.feature.storygraph.StoryGraphPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryGraphUiState(
    val graph: GraphData = GraphData.EMPTY,
    /** The real transactions behind each node, so a selection can list them. */
    val transactionsByNode: Map<String, List<com.sherif.ledger.feature.storygraph.GraphTransactionRef>> = emptyMap(),
    val selectedId: String? = null,
    val query: String = "",
    val isLoading: Boolean = true,
) {
    val selectedTransactions: List<com.sherif.ledger.feature.storygraph.GraphTransactionRef>
        get() = selectedId?.let { transactionsByNode[it] }.orEmpty()
}

@HiltViewModel
class StoryGraphViewModel @Inject constructor(
    private val transactionReadSource: TransactionReadSource,
    private val accountRepository: AccountRepository,
    private val tagRepository: TagRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val merchantResolver: MerchantResolver,
    private val analytics: GetFinancialAnalyticsUseCase,
    private val builder: StoryGraphBuilder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryGraphUiState())
    val uiState: StateFlow<StoryGraphUiState> = _uiState.asStateFlow()

    private var palette: StoryGraphPalette? = null

    /** The palette comes from the theme, which only the composition can read. */
    fun setPalette(newPalette: StoryGraphPalette) {
        if (palette != null) return
        palette = newPalette
        load()
    }

    private fun load() {
        val palette = palette ?: return
        viewModelScope.launch {
            combine(
                transactionReadSource.observeAllTransactions(),
                tagRepository.observeTagsByTransaction(),
                budgetRepository.observeAll(),
                goalRepository.observeAll(),
            ) { transactionsResult, tags, budgets, goals ->
                val transactions = (transactionsResult as? LedgerResult.Success)?.data ?: emptyList()
                val accounts = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data
                    ?: emptyList()
                // Stories give each transaction its real category through the same
                // analytics path every other screen uses — the graph must not invent
                // a second categorisation.
                val stories = analytics.transactionStories(transactions)

                builder.build(
                    transactions = transactions,
                    accounts = accounts,
                    merchantNameOf = { merchantResolver.resolve(it.merchantOrRawText).displayName },
                    categoryOf = { stories[it.id]?.category ?: "UNKNOWN" },
                    tagsByTransaction = tags,
                    budgets = budgets,
                    goals = goals,
                    palette = palette,
                )
            }.collect { result ->
                _uiState.value = _uiState.value.copy(
                    graph = result.graph,
                    transactionsByNode = result.transactionsByNode,
                    isLoading = false,
                )
            }
        }
    }

    fun select(id: String?) {
        _uiState.value = _uiState.value.copy(selectedId = id)
    }

    /**
     * Focuses the first node whose label matches. Typing "Amazon" selects the
     * Amazon merchant and, because selection highlights neighbours, everything it
     * connects to comes forward with it.
     */
    fun search(query: String) {
        val match = _uiState.value.graph.nodes
            .firstOrNull { it.label.contains(query, ignoreCase = true) }
            ?.takeIf { query.isNotBlank() }
        _uiState.value = _uiState.value.copy(query = query, selectedId = match?.id ?: _uiState.value.selectedId)
    }
}
