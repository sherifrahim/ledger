package com.sherif.ledger.core.domain.service.intelligence

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RC8 Phase G — deterministic forecast foundation. No AI anywhere in this
 * class (per the spec: "No AI required initially. Design interfaces so AI
 * can enhance forecasting later.") — [historicalMonthlyNetMinor] is
 * deliberately shaped to drop straight into the ALREADY-EXISTING
 * [com.sherif.ledger.feature.ai.context.AIContextBuilder.forecast] (built in
 * RC5/RC6, never wired to anything) without any interface change, the day a
 * future RC decides to enhance this with AI commentary — that wiring is
 * explicitly NOT done here (RC8's own constraint: no new AI features).
 */
data class ForecastResult(
    val accountId: Long,
    val currencyCode: CurrencyCode,
    val currentBalanceMinor: Long,
    val expectedBalanceMinor: Long,
    val horizonDays: Int,
    /** Recurring schedules (subscriptions, bills, EMIs, rent, salary) due within the horizon, soonest first. */
    val upcomingSchedules: List<RecurringSchedule>,
    val projectedSalaryDate: Instant?,
    /** Last up to 6 calendar months' net flow (income - expense) for this account, oldest first. */
    val historicalMonthlyNetMinor: List<Long>,
)

@Singleton
class ForecastEngine @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountBalanceService: AccountBalanceService,
    private val recurringScheduleAnalyzer: RecurringScheduleAnalyzer,
) {
    suspend fun forecast(accountId: Long, horizonDays: Int = 30): ForecastResult? {
        val balance = accountBalanceService.currentBalance(accountId) ?: return null
        val txnResult = transactionRepository.observeTransactionsForAccount(accountId).first()
        val transactions = (txnResult as? LedgerResult.Success)?.data ?: emptyList()

        val schedules = recurringScheduleAnalyzer.analyze(transactions)
        val now = Instant.now()
        val horizonEnd = now.plus(horizonDays.toLong(), ChronoUnit.DAYS)
        val upcoming = schedules
            .filter { it.nextExpectedDate.isAfter(now) && it.nextExpectedDate.isBefore(horizonEnd) }
            .sortedBy { it.nextExpectedDate }

        val projectedIncome = upcoming.filter { it.kind == RecurringKind.SALARY }.sumOf { it.averageAmountMinor }
        val projectedOutflow = upcoming.filter { it.kind != RecurringKind.SALARY }.sumOf { it.averageAmountMinor }
        val expectedBalance = balance.minorUnits + projectedIncome - projectedOutflow

        val zone = ZoneId.systemDefault()
        val monthlyNet = transactions
            .groupBy { it.timestamp.atZone(zone).let { z -> z.year to z.monthValue } }
            .toSortedMap(compareBy({ it.first }, { it.second }))
            .values
            .map { monthTxns ->
                monthTxns.sumOf { t ->
                    when (t.type) {
                        TransactionType.INCOME -> t.amount.minorUnits
                        TransactionType.EXPENSE -> -t.amount.minorUnits
                        TransactionType.REFUND -> t.amount.minorUnits
                        TransactionType.TRANSFER -> 0L
                    }
                }
            }
            .takeLast(6)

        return ForecastResult(
            accountId = accountId,
            currencyCode = balance.currencyCode,
            currentBalanceMinor = balance.minorUnits,
            expectedBalanceMinor = expectedBalance,
            horizonDays = horizonDays,
            upcomingSchedules = upcoming,
            projectedSalaryDate = upcoming.firstOrNull { it.kind == RecurringKind.SALARY }?.nextExpectedDate,
            historicalMonthlyNetMinor = monthlyNet,
        )
    }
}
