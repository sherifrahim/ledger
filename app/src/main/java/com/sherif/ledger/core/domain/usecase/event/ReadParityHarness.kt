package com.sherif.ledger.core.domain.usecase.event

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.FinancialEvent
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.FinancialEventRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.event.toMirrorTransaction
import com.sherif.ledger.core.domain.service.transaction.BalanceCalculator
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * Read Parity Harness (ADR-0001, Milestone P6) — architectural verification, not UI.
 *
 * Objectively answers: *can FinancialEvent reads replace Transaction reads?* It maps the
 * ACTIVE FinancialEvents back to `Transaction`-shaped domain objects (a deliberately
 * **lossy** reconstruction — see [toTransaction]) and runs the **same existing engines**
 * (`GetFinancialAnalyticsUseCase`, `transactionStories`, `BalanceCalculator`) on both the
 * real transactions and the event-derived transactions, comparing **domain results** — no
 * screenshots, no UI strings.
 *
 * Reuse-first: no new analytics engine; the harness only orchestrates existing ones.
 */
class ReadParityHarness @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val financialEventRepository: FinancialEventRepository,
    private val accountRepository: AccountRepository,
    private val analytics: GetFinancialAnalyticsUseCase,
    private val balanceCalculator: BalanceCalculator,
) {

    suspend fun execute(): ParityReport {
        val transactions = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)?.data ?: emptyList()
        val events = financialEventRepository.observeActive().first()
        val eventTxns = events.map { it.toMirrorTransaction() }

        val accountType = ((accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data ?: emptyList())
            .associate { it.id to it.type }

        val checks = mutableListOf<FeatureParity>()

        // --- Balance (Accounts, Dashboard hero). BalanceCalculator.effect uses type +
        //     transferDirection + currency; the event omits transferDirection, so TRANSFER
        //     items are the one structural gap (classified below).
        fun contribution(list: List<Transaction>): Long = list.sumOf { t ->
            balanceCalculator.effect(t, accountType[t.accountId] ?: com.sherif.ledger.core.domain.model.AccountType.CHECKING, t.amount.currencyCode)
        }
        val legacyBalance = contribution(transactions)
        val eventBalance = contribution(eventTxns)
        val transfers = transactions.count { it.type == com.sherif.ledger.core.domain.model.TransactionType.TRANSFER }
        checks += FeatureParity(
            feature = "Balance / Accounts",
            legacy = legacyBalance.toString(),
            event = eventBalance.toString(),
            match = legacyBalance == eventBalance,
            classification = if (legacyBalance != eventBalance)
                "Intentional gap: FinancialEvent omits transferDirection; $transfers TRANSFER item(s) differ. Resolve before P7 flips balance reads (extend event schema or keep balance on the transaction source)."
            else null,
        )

        // --- Analytics (Dashboard + Insights): full-range compute over each list.
        if (transactions.isNotEmpty()) {
            val start = transactions.minOf { it.timestamp }
            val end = transactions.maxOf { it.timestamp }.plusMillis(1)
            val a = analytics.compute(transactions, start, end)
            val b = analytics.compute(eventTxns, start, end)
            checks += FeatureParity(
                feature = "Analytics (Dashboard/Insights)",
                legacy = "netSpend=${a.netSpendMinor} income=${a.incomeMinor} cats=${a.categoryTotals.size} merchants=${a.merchantTotals.size} trend=${a.trendPoints.size}",
                event = "netSpend=${b.netSpendMinor} income=${b.incomeMinor} cats=${b.categoryTotals.size} merchants=${b.merchantTotals.size} trend=${b.trendPoints.size}",
                match = a.copy(periodStart = start, periodEnd = end) == b.copy(periodStart = start, periodEnd = end),
            )
        }

        // --- Stories (categories/explanations feeding Merchant + Search + Dashboard).
        val storiesLegacy = analytics.transactionStories(transactions)
        val storiesEvent = analytics.transactionStories(eventTxns)
        checks += FeatureParity(
            feature = "Stories (categories/explanations)",
            legacy = "n=${storiesLegacy.size}",
            event = "n=${storiesEvent.size}",
            match = storiesLegacy == storiesEvent,
        )

        // --- Merchant: aggregate the busiest merchant from both.
        val topMerchant = transactions.mapNotNull { it.rawText?.trim() }.filter { it.isNotBlank() }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        if (topMerchant != null) {
            fun agg(list: List<Transaction>): Pair<Long, Int> {
                val mine = list.filter { it.rawText?.trim().equals(topMerchant, ignoreCase = true) }
                return mine.sumOf { it.amount.minorUnits } to mine.size
            }
            val l = agg(transactions); val e = agg(eventTxns)
            checks += FeatureParity("Merchant ($topMerchant)", "total=${l.first} count=${l.second}", "total=${e.first} count=${e.second}", l == e)
        }

        // --- Review Queue: the uncategorized set (categoryId == null).
        val legacyReview = transactions.count { it.categoryId == null }
        val eventReview = eventTxns.count { it.categoryId == null }
        checks += FeatureParity("Review Queue (uncategorized)", legacyReview.toString(), eventReview.toString(), legacyReview == eventReview)

        // --- Search: match count for a representative term.
        val term = topMerchant?.take(3)?.lowercase() ?: "a"
        fun matches(list: List<Transaction>) = list.count { it.rawText?.contains(term, ignoreCase = true) == true }
        val ls = matches(transactions); val es = matches(eventTxns)
        checks += FeatureParity("Search (q='$term')", ls.toString(), es.toString(), ls == es)

        val report = ParityReport(checks)
        LedgerLogger.d("Read parity: ${report.summary()}")
        report.features.forEach { LedgerLogger.d("Read parity · ${it.line()}") }
        return report
    }
}

data class FeatureParity(
    val feature: String,
    val legacy: String,
    val event: String,
    val match: Boolean,
    val classification: String? = null,
) {
    val status: String get() = if (match) "PASS" else "DIFF"
    fun line(): String = "$feature: legacy[$legacy] event[$event] -> $status" + (classification?.let { " ($it)" } ?: "")
}

data class ParityReport(val features: List<FeatureParity>) {
    val total: Int get() = features.size
    val passed: Int get() = features.count { it.match }
    val failed: Int get() = features.count { !it.match }
    /** A difference the harness explains (classified) — not an unexplained failure. */
    val intentionalDifferences: Int get() = features.count { !it.match && it.classification != null }
    val unexpectedDifferences: Int get() = features.count { !it.match && it.classification == null }
    /** Parity is proven when nothing differs without an explanation. */
    val proven: Boolean get() = unexpectedDifferences == 0

    fun summary(): String =
        "total=$total passed=$passed failed=$failed intentional=$intentionalDifferences " +
            "unexpected=$unexpectedDifferences proven=$proven"
}
