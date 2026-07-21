package com.sherif.ledger.core.domain.service.intelligence

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.relationship.RelationshipEngine
import com.sherif.ledger.feature.relationship.RelationshipType
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToLong

/** RC8 Phase E: how often a [RecurringSchedule] repeats. */
enum class RecurrenceFrequency { WEEKLY, MONTHLY, QUARTERLY, YEARLY, IRREGULAR }

/** What kind of recurring series this is — a label for the Intelligence Inspector, not a new relationship concept. */
enum class RecurringKind { SUBSCRIPTION, RECURRING_BILL, RECURRING_MERCHANT, LOAN_EMI, SALARY, RENT }

/** RC8 Phase E: a projected schedule — the piece the RC7-era relationship resolvers never computed (they only ever looked backward at pairs, never forward). */
data class RecurringSchedule(
    val label: String,
    val kind: RecurringKind,
    val frequency: RecurrenceFrequency,
    val averageAmountMinor: Long,
    val currencyCode: CurrencyCode,
    val lastOccurrence: Instant,
    val nextExpectedDate: Instant,
    val confidence: Int,
    val transactionIds: List<Long>,
)

/**
 * RC8 Phase E — Subscription & Recurring Engine. Deliberately does NOT
 * reimplement recurrence detection: for subscriptions/recurring bills/
 * recurring merchants/loan-EMIs it reuses [RelationshipEngine.analyze]'s
 * ALREADY-detected relationships (frozen, untouched) and unions their
 * pairwise matches back into full time series, then computes what those
 * pairwise relationships never did — a frequency label, a last-occurrence
 * date, and a projected next-expected date. Salary and Rent have no existing
 * RelationshipEngine resolver (confirmed by reading RelationshipResolvers.kt
 * before writing this — SALARY_FUNDS_EXPENSE pairs salary WITH the expenses
 * it funds, it does not detect salary recurring against itself), so those two
 * use a small, separately-scoped keyword grouping here — never touching the
 * frozen RelationshipEngine/RelationshipResolvers files.
 */
@Singleton
class RecurringScheduleAnalyzer @Inject constructor(
    private val relationshipEngine: RelationshipEngine,
) {
    private val salaryWords = listOf("salary", "payroll", "wps")
    private val rentWords = listOf("rent", "tenancy", "lease")

    fun analyze(transactions: List<Transaction>): List<RecurringSchedule> {
        if (transactions.isEmpty()) return emptyList()
        val txnById = transactions.associateBy { it.id }
        val relationships = relationshipEngine.analyze(transactions)

        val schedules = mutableListOf<RecurringSchedule>()

        val relationshipDriven = mapOf(
            setOf(RelationshipType.SUBSCRIPTION) to RecurringKind.SUBSCRIPTION,
            setOf(RelationshipType.RECURRING_BILL) to RecurringKind.RECURRING_BILL,
            setOf(RelationshipType.RECURRING_MERCHANT) to RecurringKind.RECURRING_MERCHANT,
            setOf(RelationshipType.LOAN_REPAYMENT, RelationshipType.INSTALLMENT_PAYMENT) to RecurringKind.LOAN_EMI,
        )
        for ((types, kind) in relationshipDriven) {
            val relevant = relationships.filter { it.type in types }
            schedules += groupIntoSchedules(relevant, txnById, kind)
        }

        schedules += keywordSeries(transactions, TransactionType.INCOME, salaryWords, RecurringKind.SALARY)
        schedules += keywordSeries(transactions, TransactionType.EXPENSE, rentWords, RecurringKind.RENT)

        return schedules
    }

    /** Unions pairwise relationships sharing a transaction id into one full series per underlying recurring identity. */
    private fun groupIntoSchedules(
        relevant: List<com.sherif.ledger.feature.relationship.FinancialRelationship>,
        txnById: Map<Long, Transaction>,
        kind: RecurringKind,
    ): List<RecurringSchedule> {
        if (relevant.isEmpty()) return emptyList()
        val txnToGroup = mutableMapOf<Long, MutableSet<Long>>()
        for (rel in relevant) {
            val ids = listOfNotNull(rel.sourceTransactionId, rel.targetTransactionId)
            val group = ids.firstNotNullOfOrNull { txnToGroup[it] } ?: mutableSetOf()
            group += ids
            ids.forEach { txnToGroup[it] = group }
        }
        val distinctGroups = txnToGroup.values.distinctBy { System.identityHashCode(it) }
        return distinctGroups.mapNotNull { ids -> buildSchedule(ids.mapNotNull { txnById[it] }, kind) }
    }

    private fun keywordSeries(
        transactions: List<Transaction>,
        type: TransactionType,
        words: List<String>,
        kind: RecurringKind,
    ): List<RecurringSchedule> {
        val matching = transactions.filter {
            it.type == type && (it.rawText?.lowercase()?.let { t -> words.any { w -> t.contains(w) } } ?: false)
        }
        return matching.groupBy { it.accountId }.values.mapNotNull { buildSchedule(it, kind) }
    }

    private fun buildSchedule(groupTxns: List<Transaction>, kind: RecurringKind): RecurringSchedule? {
        if (groupTxns.size < 2) return null
        val sorted = groupTxns.sortedBy { it.timestamp }
        val gaps = sorted.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.timestamp, b.timestamp).toDouble() }
        if (gaps.isEmpty() || gaps.any { it <= 0.0 }) return null
        val avgGap = gaps.average()
        val frequency = classifyFrequency(avgGap)
        val variance = gaps.map { abs(it - avgGap) }.average()
        val confidence = confidenceFromVariance(variance, avgGap, sorted.size)
        val last = sorted.last()
        return RecurringSchedule(
            label = last.rawText?.take(60) ?: kind.name,
            kind = kind,
            frequency = frequency,
            averageAmountMinor = sorted.map { it.amount.minorUnits }.average().roundToLong(),
            currencyCode = last.amount.currencyCode,
            lastOccurrence = last.timestamp,
            nextExpectedDate = last.timestamp.plus(avgGap.roundToLong(), ChronoUnit.DAYS),
            confidence = confidence,
            transactionIds = sorted.map { it.id },
        )
    }

    private fun classifyFrequency(avgGapDays: Double): RecurrenceFrequency = when {
        avgGapDays in 5.0..9.0 -> RecurrenceFrequency.WEEKLY
        avgGapDays in 25.0..35.0 -> RecurrenceFrequency.MONTHLY
        avgGapDays in 80.0..100.0 -> RecurrenceFrequency.QUARTERLY
        avgGapDays in 350.0..380.0 -> RecurrenceFrequency.YEARLY
        else -> RecurrenceFrequency.IRREGULAR
    }

    /** Tighter gap variance and more occurrences both raise confidence — never a flat/fabricated number. */
    private fun confidenceFromVariance(variance: Double, avgGap: Double, occurrenceCount: Int): Int {
        val varianceRatio = if (avgGap > 0) (variance / avgGap).coerceIn(0.0, 1.0) else 1.0
        val base = 95 - (varianceRatio * 40).roundToLong().toInt()
        val occurrenceBonus = if (occurrenceCount >= 4) 0 else -10
        return (base + occurrenceBonus).coerceIn(30, 95)
    }
}
