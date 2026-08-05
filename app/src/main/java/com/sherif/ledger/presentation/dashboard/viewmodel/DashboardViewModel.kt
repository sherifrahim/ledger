package com.sherif.ledger.presentation.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.isOutflow
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.MerchantRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.service.diagnostic.FinancialTraceCollector
import com.sherif.ledger.core.domain.service.transaction.TransactionDisplayName
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.merchant.MerchantResolver
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
import com.sherif.ledger.core.domain.service.intelligence.RecurrenceFrequency
import com.sherif.ledger.presentation.dashboard.UpcomingUiModel
import com.sherif.ledger.core.domain.model.Transaction
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

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
    private val merchantRepository: MerchantRepository,
    private val merchantResolver: MerchantResolver,
    private val financialTraceCollector: FinancialTraceCollector, // RC4: permanent, replaces the disposable RC2/RC3 BalanceTraceDiagnostic
    private val recurringScheduleAnalyzer: com.sherif.ledger.core.domain.service.intelligence.RecurringScheduleAnalyzer,
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
        accountRepository.observeAllAccounts(),
        // Recurrence needs HISTORY: a monthly subscription appears once per month,
        // so a one-month window can never contain enough occurrences to establish a
        // rhythm. Reading the full set is not a new cost class here — computeNetWorth
        // below already replays every transaction on this same screen.
        transactionReadSource.observeAllTransactions(),
        // Real-world finding (design review, 2026-08-06): a capture that lands on a
        // Candidate Account (unrecognized institution, or a transport/messaging
        // relay never resolved to a real account) is invisible to totalBalance but
        // still shows up in recentActivity below it — a hero number that silently
        // disagrees with the list under it reads as broken. Cross-referenced against
        // allResult below, never used to change what counts toward the balance.
        accountRepository.observeCandidateAccounts(),
    ) { recentResult, monthResult, _, allResult, candidateResult ->

        // Diagnostic-only, and debug-only: logs a structured report via LedgerLogger,
        // changes nothing displayed. Gated so it never runs in a release build.
        if (com.sherif.ledger.BuildConfig.DEBUG && !diagnosticHasRun) {
            diagnosticHasRun = true
            try {
                financialTraceCollector.buildReport()
            } catch (e: Exception) {
                com.sherif.ledger.core.common.logging.LedgerLogger.e("FinancialTraceCollector failed", e)
            }
        }

        val netWorth = getFinancialAnalyticsUseCase.computeNetWorth()
        val primaryCurrency = netWorth.currency
        // "Total Balance" is the money the user HAS. Previously this was net worth,
        // which subtracts credit-card debt — so a real AED 1,568.52 balance with an
        // AED 11,888 card balance displayed as AED -11,771.65. Debt is still shown,
        // as its own figure, never folded into this one.
        val totalBalanceUnits = netWorth.cashBalanceMinor

        val monthTransactions = (monthResult as? LedgerResult.Success)?.data ?: emptyList()
        val analytics = getFinancialAnalyticsUseCase.compute(monthTransactions, currentMonthRange.first, currentMonthRange.second)

        // A change badge comparing this month's spend against last month's is
        // meaningless when this month's own spend is zero (there is nothing to have
        // changed) — showing e.g. "-100% vs last month" beside a genuinely zero
        // figure reads as an error, not as a real comparison. Real month-over-month
        // math is otherwise untouched; this only decides whether the badge renders.
        val balanceChangePercentage = if (analytics.netSpendMinor == 0L) {
            null
        } else {
            getFinancialAnalyticsUseCase.computeMonthOverMonthChange(
                analytics.netSpendMinor, currentMonthRange.first,
            )
        }

        val recentTransactions = (recentResult as? LedgerResult.Success)?.data ?: emptyList()
        // The ONLY place relationship-derived explanations/categories are resolved
        // for this screen — one call into the analytics layer, not a direct
        // RelationshipEngine invocation here.
        val stories = getFinancialAnalyticsUseCase.transactionStories(recentTransactions)
        // Clean row titles: brandId→Brand.name (what capture resolved), else the
        // deterministic merchant registry — never the raw SMS text.
        val brandNames = (merchantRepository.getAllBrands() as? LedgerResult.Success)
            ?.data?.associate { it.id to it.name } ?: emptyMap()

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
                        merchantName = TransactionDisplayName.resolve(txn, brandNames, merchantResolver),
                        category = story?.category ?: "UNKNOWN",
                        amount = MoneyFormatter.format(txn.amount, includeSymbol = false),
                        isExpense = txn.isOutflow,
                        time = txn.timestamp.atZone(ZoneId.systemDefault()).format(timeFormatter),
                        explanation = story?.explanation ?: ""
                    )
                }
            )
        }

        val allTransactions = (allResult as? LedgerResult.Success)?.data ?: emptyList()
        val upcoming = buildUpcoming(allTransactions)

        val candidateAccountIds = (candidateResult as? LedgerResult.Success)?.data
            ?.map { it.id }?.toSet() ?: emptySet()
        val unattributedCount = if (candidateAccountIds.isEmpty()) {
            0
        } else {
            allTransactions.count { it.accountId in candidateAccountIds }
        }

        DashboardUiState(
            totalBalance = MoneyFormatter.format(Money(totalBalanceUnits, primaryCurrency), includeSymbol = true),
            isNegativeBalance = totalBalanceUnits < 0,
            balanceChangePercentage = balanceChangePercentage,
            monthlyExpenses = MoneyFormatter.format(Money(analytics.netSpendMinor, primaryCurrency), includeSymbol = true),
            categories = analytics.categoryTotals.map { CategoryFilterUiModel(it.category, it.category) },
            recentActivity = activityGroups,
            intelligenceSummary = analytics.intelligenceSummary,
            upcoming = upcoming,
            unattributedCount = unattributedCount,
            insights = emptyList()
        )
    }
        // Every one of the steps above is real work over the full history — a
        // balance replay, a relationship pass, and now recurrence detection — and
        // a combine's transform runs on the collector's context, which for
        // viewModelScope is the MAIN THREAD. Left there it renders the app at well
        // under one frame per second and the navigation bar stops responding
        // entirely, which is exactly what surfacing the recurring engine caused on
        // a real 385-transaction library. The computation belongs on Default; only
        // the resulting state belongs on Main.
        .flowOn(Dispatchers.Default)
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EMPTY_STATE
    )

    /**
     * The next few expected charges, from the recurring engine that until now was
     * reachable only from a debug inspector.
     *
     * Three deliberate filters, all of them about not over-claiming:
     *  - **Irregular series are dropped.** The engine labels a series it could not
     *    fit to a cadence as IRREGULAR; projecting a date from one is fiction.
     *  - **Low confidence is dropped.** Below the threshold the engine is telling
     *    us it is not sure, and a dashboard is the wrong place to argue with it.
     *  - **Already-past projections are dropped.** A "next expected" date that has
     *    come and gone means the charge either arrived (and is in the feed) or the
     *    series ended; either way it is not upcoming.
     *
     * Sorted by how soon, and capped, because this is a glance surface — the full
     * list belongs on a screen of its own when one exists.
     */
    private fun buildUpcoming(transactions: List<Transaction>): List<UpcomingUiModel> {
        if (transactions.isEmpty()) return emptyList()
        val schedules = runCatching { recurringScheduleAnalyzer.analyze(transactions) }
            .getOrElse {
                com.sherif.ledger.core.common.logging.LedgerLogger.e("RecurringScheduleAnalyzer failed", it)
                emptyList()
            }
        return UpcomingProjection.from(schedules, Instant.now())
    }

    companion object {
        /** Below this the engine is signalling doubt; the dashboard stays quiet. */
        private const val MIN_UPCOMING_CONFIDENCE = 60

        /** A glance surface, not a list screen. */
        private const val MAX_UPCOMING = 3

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







