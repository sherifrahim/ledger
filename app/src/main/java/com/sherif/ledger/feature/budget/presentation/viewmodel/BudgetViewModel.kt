package com.sherif.ledger.feature.budget.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.BudgetStatus
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.BudgetRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.usecase.budget.GetBudgetStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

data class BudgetUiState(
    val statuses: List<BudgetStatus> = emptyList(),
    /** Categories the user actually spends in, offered when adding a budget —
     *  a picker listing every category in the enum would mostly be noise. */
    val suggestedCategories: List<String> = emptyList(),
    val currency: CurrencyCode = CurrencyCode.AED,
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionReadSource: TransactionReadSource,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
    private val getBudgetStatusUseCase: GetBudgetStatusUseCase,
) : ViewModel() {

    private val currentMonthRange = run {
        val now = java.time.ZonedDateTime.now()
        val start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
        val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
            .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999).toInstant()
        start to end
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRepository.observeAll(),
        transactionReadSource.observeTransactionsBetween(currentMonthRange.first, currentMonthRange.second),
    ) { budgets, monthResult ->
        val monthTransactions = (monthResult as? LedgerResult.Success)?.data ?: emptyList()
        // The SAME analytics pass every other screen uses — see
        // GetBudgetStatusUseCase for why spend is never recomputed here.
        val analytics = getFinancialAnalyticsUseCase.compute(
            monthTransactions, currentMonthRange.first, currentMonthRange.second,
        )

        BudgetUiState(
            statuses = getBudgetStatusUseCase.execute(budgets, analytics.categoryTotals),
            suggestedCategories = analytics.categoryTotals
                .map { it.category }
                .filter { it != "UNKNOWN" }
                .filterNot { category -> budgets.any { it.category.equals(category, ignoreCase = true) } },
            currency = analytics.currency,
        )
    }
        // Same reason as DashboardViewModel: a combine transform runs on the
        // collector's context, and this one replays analytics over the month.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    fun setBudget(category: String, limitMinor: Long, currency: CurrencyCode) {
        viewModelScope.launch { budgetRepository.setBudget(category, limitMinor, currency) }
    }

    fun removeBudget(category: String) {
        viewModelScope.launch { budgetRepository.removeBudget(category) }
    }
}
