package com.sherif.ledger.feature.story.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
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
import kotlinx.coroutines.flow.map
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

    val uiState: StateFlow<StoryUiState> = transactionReadSource.observeRecentTransactions(50)
        .map { result ->
            val txns = (result as? LedgerResult.Success)?.data ?: emptyList()
            if (txns.isEmpty()) return@map StoryUiState()

            // Bug found in on-device verification (2026-08-06): this used to be a
            // SEPARATE observeTransactionsBetween(start, end) query with `end` a
            // ViewModel-construction-time Instant.now() -- frozen for the whole
            // ViewModel's lifetime. Any transaction captured AFTER the ViewModel
            // was created (i.e. any transaction arriving while the app is actually
            // running) fell after that frozen upper bound and was silently
            // excluded from its own "this week" summary. Filtering the
            // already-fetched `txns` with a freshly-read Instant.now() on every
            // recomputation has no such staleness -- there is no frozen bound to
            // go stale.
            val weekCutoff = Instant.now().minus(7, ChronoUnit.DAYS)
            val weekTxns = txns.filter { it.timestamp.isAfter(weekCutoff) }
            val weeklyNarrative = buildWeeklyNarrative(weekTxns, weekCutoff)

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
    private fun buildWeeklyNarrative(weekTxns: List<Transaction>, weekCutoff: Instant): String? {
        val expenseCount = weekTxns.count { it.type == TransactionType.EXPENSE }
        if (expenseCount < MIN_NARRATIVE_TRANSACTIONS) return null

        val analytics = getFinancialAnalyticsUseCase.compute(weekTxns, weekCutoff, Instant.now())
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
