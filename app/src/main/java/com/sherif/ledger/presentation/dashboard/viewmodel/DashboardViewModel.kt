package com.sherif.ledger.presentation.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.service.diagnostic.FinancialTraceCollector
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
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

/**
 * Phase 10: consumes ONLY [GetFinancialAnalyticsUseCase] — never
 * [com.sherif.ledger.feature.relationship.RelationshipEngine] or
 * [com.sherif.ledger.core.domain.service.transaction.FinancialStoryPresenter]
 * directly. Relationship analysis and story formatting happen exactly once,
 * inside the analytics use case; this ViewModel only renders what it returns.
 *
 * No value here is fabricated. balanceChangePercentage is null (and the UI hides
 * its badge) whenever a real month-over-month comparison isn't computable —
 * never a static placeholder. There is no "learning phase" that replaces a real,
 * already-computed number with status text: if the data exists, it's shown.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionReadSource: TransactionReadSource,
    private val accountRepository: AccountRepository,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
    private val financialTraceCollector: FinancialTraceCollector, // RC4: permanent, replaces the disposable RC2/RC3 BalanceTraceDiagnostic
) : ViewModel() {

    // TEMPORARY: runs the diagnostic exactly once per ViewModel lifetime, purely
    // to log findings — it never touches or alters anything the UI displays.
    // Remove this flag and the trigger below once the investigation concludes.
    private var diagnosticHasRun = false

    private val currentMonthRange = run {
        val now = java.time.ZonedDateTime.now()
        val start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
        val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999).toInstant()
        start to end
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionReadSource.observeRecentTransactions(20),
        transactionReadSource.observeTransactionsBetween(currentMonthRange.first, currentMonthRange.second),
        accountRepository.observeAllAccounts()
    ) { recentResult, monthResult, _ ->

        if (!diagnosticHasRun) {
            diagnosticHasRun = true
            try {
                financialTraceCollector.buildReport() // logs a structured report via LedgerLogger, changes nothing displayed
            } catch (e: Exception) {
                com.sherif.ledger.core.common.logging.LedgerLogger.e("FinancialTraceCollector failed", e)
            }
        }

        val netWorth = getFinancialAnalyticsUseCase.computeNetWorth()
        val primaryCurrency = netWorth.currency
        val totalBalanceUnits = netWorth.netWorthMinor

        val monthTransactions = (monthResult as? LedgerResult.Success)?.data ?: emptyList()
        val analytics = getFinancialAnalyticsUseCase.compute(monthTransactions, currentMonthRange.first, currentMonthRange.second)

        val balanceChangePercentage = getFinancialAnalyticsUseCase.computeMonthOverMonthChange(
            analytics.netSpendMinor, currentMonthRange.first,
        )

        val recentTransactions = (recentResult as? LedgerResult.Success)?.data ?: emptyList()
        // The ONLY place relationship-derived explanations/categories are resolved
        // for this screen — one call into the analytics layer, not a direct
        // RelationshipEngine invocation here.
        val stories = getFinancialAnalyticsUseCase.transactionStories(recentTransactions)

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
                    val story = stories[txn.id]
                    ActivityItemUiModel(
                        id = txn.id.toString(),
                        merchantName = txn.rawText ?: "Unknown",
                        category = story?.category ?: "UNKNOWN",
                        amount = MoneyFormatter.format(txn.amount, includeSymbol = false),
                        isExpense = txn.type == TransactionType.EXPENSE,
                        time = txn.timestamp.atZone(ZoneId.systemDefault()).format(timeFormatter),
                        explanation = story?.explanation ?: ""
                    )
                }
            )
        }

        DashboardUiState(
            totalBalance = MoneyFormatter.format(Money(totalBalanceUnits, primaryCurrency), includeSymbol = true),
            isNegativeBalance = totalBalanceUnits < 0,
            balanceChangePercentage = balanceChangePercentage,
            monthlyExpenses = MoneyFormatter.format(Money(analytics.netSpendMinor, primaryCurrency), includeSymbol = true),
            categories = analytics.categoryTotals.map { CategoryFilterUiModel(it.category, it.category) },
            recentActivity = activityGroups,
            intelligenceSummary = analytics.intelligenceSummary,
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
            balanceChangePercentage = null,
            monthlyExpenses = "0.00",
            categories = emptyList(),
            recentActivity = emptyList(),
            intelligenceSummary = emptyList(),
            insights = emptyList()
        )
    }
}







