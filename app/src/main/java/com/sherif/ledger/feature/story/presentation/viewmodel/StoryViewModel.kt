package com.sherif.ledger.feature.story.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.isOutflow
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.MerchantRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.service.transaction.TransactionDisplayName
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.merchant.MerchantResolver
import com.sherif.ledger.feature.story.presentation.StoryGroupUi
import com.sherif.ledger.feature.story.presentation.StoryItemUi
import com.sherif.ledger.feature.story.presentation.StoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Financial Story — real narrative, not scaffolding. Reuses the existing
 * [GetFinancialAnalyticsUseCase.transactionStories] (relationship + intelligence
 * engines) for each transaction's explanation; no new engine, no fabricated
 * copy. Reads originate from [TransactionReadSource] like every other list
 * screen (FinancialEvent-sourced, ADR-0001).
 */
@HiltViewModel
class StoryViewModel @Inject constructor(
    private val transactionReadSource: TransactionReadSource,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
    private val merchantRepository: MerchantRepository,
    private val merchantResolver: MerchantResolver,
) : ViewModel() {

    private val weekRange = run {
        val now = Instant.now()
        now.minus(7, ChronoUnit.DAYS) to now
    }

    val uiState: StateFlow<StoryUiState> = combine(
        transactionReadSource.observeRecentTransactions(50),
        transactionReadSource.observeTransactionsBetween(weekRange.first, weekRange.second),
    ) { result, weekResult ->
            val txns = (result as? LedgerResult.Success)?.data ?: emptyList()
            if (txns.isEmpty()) return@combine StoryUiState()

            val weekTxns = (weekResult as? LedgerResult.Success)?.data ?: emptyList()
            val weeklyNarrative = buildWeeklyNarrative(weekTxns)

            val stories = getFinancialAnalyticsUseCase.transactionStories(txns)
            val brandNames = (merchantRepository.getAllBrands() as? LedgerResult.Success)
                ?.data?.associate { it.id to it.name } ?: emptyMap()
            val groups = txns.groupBy { txn ->
                dateLabel(txn.timestamp.atZone(ZoneId.systemDefault()).toLocalDate())
            }.map { (title, list) ->
                StoryGroupUi(
                    title = title,
                    items = list.map { txn ->
                        StoryItemUi(
                            id = txn.id.toString(),
                            merchant = TransactionDisplayName.resolve(txn, brandNames, merchantResolver),
                            explanation = stories[txn.id]?.explanation ?: "",
                            amount = MoneyFormatter.format(txn.amount, includeSymbol = false),
                            isExpense = txn.isOutflow,
                        )
                    },
                )
            }
            StoryUiState(groups, weeklyNarrative)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StoryUiState(),
        )

    private fun dateLabel(date: LocalDate): String = when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM"))
    }

    /**
     * A plain-language summary of the last 7 days, reusing the same
     * [GetFinancialAnalyticsUseCase.compute] category totals Dashboard already
     * reads — no separate narrative engine, nothing fabricated. Null below
     * [MIN_NARRATIVE_TRANSACTIONS]: one transaction isn't a story, it's a receipt.
     */
    private fun buildWeeklyNarrative(weekTxns: List<com.sherif.ledger.core.domain.model.Transaction>): String? {
        val expenseCount = weekTxns.count { it.type == TransactionType.EXPENSE }
        if (expenseCount < MIN_NARRATIVE_TRANSACTIONS) return null

        val analytics = getFinancialAnalyticsUseCase.compute(weekTxns, weekRange.first, weekRange.second)
        if (analytics.netSpendMinor <= 0) return null

        val spent = MoneyFormatter.format(Money(analytics.netSpendMinor, analytics.currency), includeSymbol = true)
        val topCategory = analytics.categoryTotals.firstOrNull()
        val txnWord = if (expenseCount == 1) "transaction" else "transactions"

        return if (topCategory != null && analytics.categoryTotals.size > 1) {
            val topSpent = MoneyFormatter.format(Money(topCategory.amountMinor, analytics.currency), includeSymbol = true)
            "You spent $spent across $expenseCount $txnWord this week. " +
                "${prettifyCategory(topCategory.category)} was your biggest category at $topSpent."
        } else {
            "You spent $spent across $expenseCount $txnWord this week."
        }
    }

    /** Same underscore fix as MerchantViewModel.prettify — see its own doc comment. */
    private fun prettifyCategory(raw: String): String = raw.trim()
        .replace('_', ' ')
        .lowercase()
        .split(Regex("\\s+"))
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    private companion object {
        const val MIN_NARRATIVE_TRANSACTIONS = 2
    }
}
