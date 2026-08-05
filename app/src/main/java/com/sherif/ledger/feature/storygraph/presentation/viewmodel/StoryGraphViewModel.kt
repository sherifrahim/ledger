package com.sherif.ledger.feature.storygraph.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.designsystem.component.graph.GraphData
import com.sherif.ledger.core.designsystem.component.graph.GraphEdge
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.merchantOrRawText
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.BudgetRepository
import com.sherif.ledger.core.domain.repository.GoalRepository
import com.sherif.ledger.core.domain.repository.TagRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.feature.merchant.MerchantResolver
import com.sherif.ledger.feature.storygraph.StoryGraphBuilder
import com.sherif.ledger.feature.storygraph.StoryGraphPalette
import com.sherif.ledger.feature.storygraph.StoryNodeKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject

/** How far back the graph looks. "All time" is the default — a filter should narrow, never surprise. */
enum class StoryGraphDateRange(val label: String) {
    ALL_TIME("All time"),
    THIS_MONTH("This month"),
    LAST_3_MONTHS("3 months"),
    THIS_YEAR("This year"),
}

data class StoryGraphFilters(
    val dateRange: StoryGraphDateRange = StoryGraphDateRange.ALL_TIME,
    /** Empty means "every account" — a filter with nothing selected excludes nothing. */
    val accountIds: Set<Long> = emptySet(),
    val categories: Set<String> = emptySet(),
    val tagIds: Set<Long> = emptySet(),
    /** Node kinds to HIDE from the canvas — empty shows everything the data has. */
    val hiddenKinds: Set<StoryNodeKind> = emptySet(),
) {
    val isActive: Boolean
        get() = dateRange != StoryGraphDateRange.ALL_TIME || accountIds.isNotEmpty() ||
            categories.isNotEmpty() || tagIds.isNotEmpty() || hiddenKinds.isNotEmpty()
}

data class StoryGraphFilterOptions(
    val accounts: List<FilterAccountUi> = emptyList(),
    val categories: List<String> = emptyList(),
    val tags: List<FilterTagUi> = emptyList(),
)

data class FilterAccountUi(val id: Long, val name: String)
data class FilterTagUi(val id: Long, val name: String)

data class StoryGraphUiState(
    val graph: GraphData = GraphData.EMPTY,
    /** The real transactions behind each node, so a selection can list them. */
    val transactionsByNode: Map<String, List<com.sherif.ledger.feature.storygraph.GraphTransactionRef>> = emptyMap(),
    val selectedId: String? = null,
    val query: String = "",
    val isLoading: Boolean = true,
    val filters: StoryGraphFilters = StoryGraphFilters(),
    val filterOptions: StoryGraphFilterOptions = StoryGraphFilterOptions(),
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

    private val _filters = MutableStateFlow(StoryGraphFilters())

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
                _filters,
            ) { transactionsResult, tagsByTransaction, budgets, goals, filters ->
                val allTransactions = (transactionsResult as? LedgerResult.Success)?.data ?: emptyList()
                val accounts = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data
                    ?: emptyList()
                // Stories give each transaction its real category through the same
                // analytics path every other screen uses — the graph must not invent
                // a second categorisation.
                val stories = analytics.transactionStories(allTransactions)
                val categoryOf: (Transaction) -> String = { stories[it.id]?.category ?: "UNKNOWN" }

                val filtered = applyFilters(allTransactions, filters, categoryOf, tagsByTransaction)
                val filteredIds = filtered.map { it.id }.toSet()

                val result = builder.build(
                    transactions = filtered,
                    accounts = accounts,
                    merchantNameOf = { merchantResolver.resolve(it.merchantOrRawText).displayName },
                    categoryOf = categoryOf,
                    tagsByTransaction = tagsByTransaction.filterKeys { it in filteredIds },
                    budgets = budgets,
                    goals = goals,
                    palette = palette,
                )
                val visibleGraph = hideKinds(result.graph, filters.hiddenKinds)

                Quintuple(
                    visibleGraph,
                    result.transactionsByNode.filterKeys { it in visibleGraph.nodesById },
                    buildFilterOptions(allTransactions, accounts, categoryOf, tagsByTransaction),
                    filters,
                    Unit,
                )
            }
                // A real replay pass (RelationshipEngine + the whole builder) on the
                // collector's context would be the same main-thread regression
                // DashboardViewModel already hit once — see its own flowOn note.
                .flowOn(Dispatchers.Default)
                .collect { (graph, txByNode, options, filters, _) ->
                    _uiState.value = _uiState.value.copy(
                        graph = graph,
                        transactionsByNode = txByNode,
                        isLoading = false,
                        filterOptions = options,
                        filters = filters,
                    )
                }
        }
    }

    /** Named 5-tuple; kotlin.Quintuple doesn't exist and this reads better than a raw Tuple5 import. */
    private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    private fun applyFilters(
        transactions: List<Transaction>,
        filters: StoryGraphFilters,
        categoryOf: (Transaction) -> String,
        tagsByTransaction: Map<Long, List<com.sherif.ledger.core.domain.model.Tag>>,
    ): List<Transaction> {
        val since: Instant? = when (filters.dateRange) {
            StoryGraphDateRange.ALL_TIME -> null
            StoryGraphDateRange.THIS_MONTH -> ZonedDateTime.now().withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
            StoryGraphDateRange.LAST_3_MONTHS -> ZonedDateTime.now().minusMonths(3).toInstant()
            StoryGraphDateRange.THIS_YEAR -> ZonedDateTime.now().withDayOfYear(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
        }
        return transactions.filter { txn ->
            (since == null || !txn.timestamp.isBefore(since)) &&
                (filters.accountIds.isEmpty() || txn.accountId in filters.accountIds) &&
                (filters.categories.isEmpty() || categoryOf(txn) in filters.categories) &&
                (filters.tagIds.isEmpty() || tagsByTransaction[txn.id]?.any { it.id in filters.tagIds } == true)
        }
    }

    private fun hideKinds(graph: GraphData, hidden: Set<StoryNodeKind>): GraphData {
        if (hidden.isEmpty()) return graph
        val hiddenNames = hidden.map { it.name }.toSet()
        val nodes = graph.nodes.filterNot { it.kind in hiddenNames }
        val ids = nodes.map { it.id }.toSet()
        val edges = graph.edges.filter { it.fromId in ids && it.toId in ids }
        return GraphData(nodes, edges)
    }

    private fun buildFilterOptions(
        allTransactions: List<Transaction>,
        accounts: List<com.sherif.ledger.core.domain.model.Account>,
        categoryOf: (Transaction) -> String,
        tagsByTransaction: Map<Long, List<com.sherif.ledger.core.domain.model.Tag>>,
    ): StoryGraphFilterOptions {
        val usedAccountIds = allTransactions.map { it.accountId }.toSet()
        return StoryGraphFilterOptions(
            accounts = accounts.filter { it.id in usedAccountIds }
                .map { FilterAccountUi(it.id, it.name) },
            categories = allTransactions.map(categoryOf).filter { it != "UNKNOWN" }.distinct().sorted(),
            tags = tagsByTransaction.values.flatten().distinctBy { it.id }
                .map { FilterTagUi(it.id, it.name) }.sortedBy { it.name },
        )
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

    fun setDateRange(range: StoryGraphDateRange) {
        _filters.value = _filters.value.copy(dateRange = range)
    }

    fun toggleAccountFilter(accountId: Long) {
        _filters.value = _filters.value.let {
            it.copy(accountIds = it.accountIds.toggled(accountId))
        }
    }

    fun toggleCategoryFilter(category: String) {
        _filters.value = _filters.value.let {
            it.copy(categories = it.categories.toggled(category))
        }
    }

    fun toggleTagFilter(tagId: Long) {
        _filters.value = _filters.value.let {
            it.copy(tagIds = it.tagIds.toggled(tagId))
        }
    }

    fun toggleKindVisible(kind: StoryNodeKind) {
        _filters.value = _filters.value.let {
            it.copy(hiddenKinds = it.hiddenKinds.toggled(kind))
        }
    }

    fun clearFilters() {
        _filters.value = StoryGraphFilters()
    }

    private fun <T> Set<T>.toggled(value: T): Set<T> =
        if (value in this) this - value else this + value
}
