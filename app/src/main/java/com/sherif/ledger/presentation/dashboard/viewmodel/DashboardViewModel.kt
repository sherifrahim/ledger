package com.sherif.ledger.presentation.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.relationship.RelationshipEngine
import com.sherif.ledger.feature.transactions.presentation.FinancialStoryPresenter
import com.sherif.ledger.presentation.dashboard.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
    private val relationshipEngine: RelationshipEngine,
    private val storyPresenter: FinancialStoryPresenter,
) : ViewModel() {

    private val currentMonthRange = run {
        val now = java.time.ZonedDateTime.now()
        val start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
        val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999).toInstant()
        start to end
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.observeRecentTransactions(20),
        transactionRepository.observeTransactionsBetween(currentMonthRange.first, currentMonthRange.second),
        accountRepository.observeAllAccounts()
    ) { recentResult, monthResult, _ ->

        val netWorth = getFinancialAnalyticsUseCase.computeNetWorth()
        val primaryCurrency = netWorth.currency
        val totalBalanceUnits = netWorth.netWorthMinor

        val monthTransactions = (monthResult as? LedgerResult.Success)?.data ?: emptyList()
        val analytics = getFinancialAnalyticsUseCase.compute(monthTransactions, currentMonthRange.first, currentMonthRange.second)

        val recentTransactions = (recentResult as? LedgerResult.Success)?.data ?: emptyList()
        val relationships = relationshipEngine.analyze(recentTransactions)

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val activityGroups = recentTransactions.groupBy { txn ->
            val date = txn.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
            when {
                date == LocalDate.now() -> "Today"
                date == LocalDate.now().minusDays(1) -> "Yesterday"
                else -> date.format(DateTimeFormatter.ofPattern("d MMMM"))
            }
        }.map { (title, txns) ->
            ActivityGroupUiModel(
                title = title,
                items = txns.map { txn ->
                    ActivityItemUiModel(
                        id = txn.id.toString(),
                        merchantName = txn.rawText ?: "Unknown",
                        category = "Other",
                        amount = MoneyFormatter.format(txn.amount, includeSymbol = false),
                        isExpense = txn.type == TransactionType.EXPENSE,
                        time = txn.timestamp.atZone(ZoneId.systemDefault()).format(timeFormatter),
                        explanation = storyPresenter.format(txn, relationships)
                    )
                }
            )
        }

        // Determine if we are in "learning" phase
        val isLearning = monthTransactions.size < 5

        DashboardUiState(
            totalBalance = MoneyFormatter.format(Money(totalBalanceUnits, primaryCurrency), includeSymbol = true),
            balanceChangePercentage = if (isLearning) "Learning..." else "+8%",
            monthlyExpenses = if (isLearning) "Tracking..." else MoneyFormatter.format(Money(analytics.netSpendMinor, primaryCurrency), includeSymbol = true),
            monthlyExpensesProgress = if (isLearning) 0.1f else 0.65f,
            needsReviewCount = 0,
            needsReviewAmount = "0.00",
            categories = analytics.categoryTotals.map { CategoryFilterUiModel(it.category, it.category) },
            recentActivity = activityGroups,
            insights = emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EMPTY_STATE
    )

    companion object {
        private val EMPTY_STATE = DashboardUiState(
            totalBalance = "0.00",
            balanceChangePercentage = "0%",
            monthlyExpenses = "0.00",
            monthlyExpensesProgress = 0f,
            needsReviewCount = 0,
            needsReviewAmount = "0.00",
            categories = emptyList(),
            recentActivity = emptyList(),
            insights = emptyList()
        )
    }
}
