package com.sherif.ledger.feature.story.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.story.presentation.StoryGroupUi
import com.sherif.ledger.feature.story.presentation.StoryItemUi
import com.sherif.ledger.feature.story.presentation.StoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
) : ViewModel() {

    val uiState: StateFlow<StoryUiState> = transactionReadSource.observeRecentTransactions(50)
        .map { result ->
            val txns = (result as? LedgerResult.Success)?.data ?: emptyList()
            if (txns.isEmpty()) return@map StoryUiState()

            val stories = getFinancialAnalyticsUseCase.transactionStories(txns)
            val groups = txns.groupBy { txn ->
                dateLabel(txn.timestamp.atZone(ZoneId.systemDefault()).toLocalDate())
            }.map { (title, list) ->
                StoryGroupUi(
                    title = title,
                    items = list.map { txn ->
                        StoryItemUi(
                            id = txn.id.toString(),
                            merchant = txn.rawText ?: "Unknown",
                            explanation = stories[txn.id]?.explanation ?: "",
                            amount = MoneyFormatter.format(txn.amount, includeSymbol = false),
                            isExpense = txn.type == TransactionType.EXPENSE,
                        )
                    },
                )
            }
            StoryUiState(groups)
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
}
