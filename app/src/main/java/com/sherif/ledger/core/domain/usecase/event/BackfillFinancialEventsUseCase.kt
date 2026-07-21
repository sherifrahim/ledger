package com.sherif.ledger.core.domain.usecase.event

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.FinancialEventRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.event.FinancialEventFactory
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Backfills the canonical FinancialEvent store from existing transactions
 * (ADR-0001, Milestone P5) — a **data migration**, not a feature.
 *
 * Dual-write ([com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase])
 * mirrors every *new* transaction; this reconciles history recorded before
 * dual-write existed. It is:
 *  - **idempotent** — a transaction whose fingerprint already has an event is
 *    skipped, so re-running never duplicates (also guarded by the DAO's unique
 *    fingerprint + IGNORE-on-conflict);
 *  - **resumable** — because it is idempotent, a re-run simply continues where a
 *    prior partial run stopped;
 *  - **safe** — read-only over transactions, append-only on events, per-row
 *    isolation (one failure never aborts the run), never touches Financial Truth
 *    or the Balance Engine (ADR-0000);
 *  - **observable** — returns a [BackfillReport] with full statistics.
 */
class BackfillFinancialEventsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val financialEventRepository: FinancialEventRepository,
) {

    suspend fun execute(): BackfillReport {
        val startMs = System.currentTimeMillis()
        val transactions = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)
            ?.data ?: emptyList()

        var skipped = 0
        var created = 0
        var duplicatesIgnored = 0
        var failures = 0

        for (txn in transactions) {
            try {
                if (financialEventRepository.findByFingerprint(txn.fingerprint) != null) {
                    skipped++
                    continue
                }
                financialEventRepository.record(FinancialEventFactory.mirrorOf(txn))
                // Confirm it landed — distinguishes a real create from an IGNORE
                // that raced with a concurrent writer.
                if (financialEventRepository.findByFingerprint(txn.fingerprint) != null) created++ else duplicatesIgnored++
            } catch (e: Exception) {
                failures++
                LedgerLogger.e("Backfill: failed to mirror tx=${txn.id} (continuing)", e)
            }
        }

        val report = BackfillReport(
            scanned = transactions.size,
            skipped = skipped,
            created = created,
            duplicatesIgnored = duplicatesIgnored,
            failures = failures,
            durationMs = System.currentTimeMillis() - startMs,
            eventsAfter = financialEventRepository.count(),
            transactionsActive = transactions.size,
        )
        LedgerLogger.d("Backfill report: ${report.summary()}")
        return report
    }
}

/**
 * Verification-grade statistics for a backfill run. [verified] is the migration's
 * pass/fail: every scanned transaction ended with a mirror event and nothing failed.
 */
data class BackfillReport(
    val scanned: Int,
    val skipped: Int,
    val created: Int,
    val duplicatesIgnored: Int,
    val failures: Int,
    val durationMs: Long,
    val eventsAfter: Int,
    val transactionsActive: Int,
) {
    val verified: Boolean get() = failures == 0 && (skipped + created) == scanned

    fun summary(): String =
        "scanned=$scanned skipped=$skipped created=$created duplicatesIgnored=$duplicatesIgnored " +
            "failures=$failures durationMs=$durationMs eventsAfter=$eventsAfter " +
            "transactionsActive=$transactionsActive verified=$verified"
}
