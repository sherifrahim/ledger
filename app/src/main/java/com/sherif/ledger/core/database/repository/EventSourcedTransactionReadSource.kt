package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.repository.FinancialEventRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.service.event.toMirrorTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

/**
 * Event-first implementation of [TransactionReadSource] (ADR-0001, P7).
 *
 * Reads the canonical, ACTIVE FinancialEvents and reconstructs Transaction-shaped
 * domain objects ([toMirrorTransaction]). `observeActive` already excludes VOID
 * (soft-deleted) and SUPERSEDED events, so this matches the legacy `is_deleted = 0`
 * filter. Read parity with the legacy Transaction path is proven (P6,
 * docs/READ_PARITY_REPORT.md) and guarded by the startup harness.
 *
 * Ordering matches the legacy DAO: `all` ascending by time, `recent` descending,
 * `between` descending — so downstream grouping/limits behave identically.
 */
class EventSourcedTransactionReadSource @Inject constructor(
    private val financialEventRepository: FinancialEventRepository,
) : TransactionReadSource {

    private fun activeTransactions(): Flow<List<Transaction>> =
        financialEventRepository.observeActive().map { events -> events.map { it.toMirrorTransaction() } }

    override fun observeAllTransactions(): Flow<LedgerResult<List<Transaction>>> =
        activeTransactions().map { txns ->
            LedgerResult.Success(txns.sortedBy { it.timestamp })
        }

    override fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>> =
        activeTransactions().map { txns ->
            LedgerResult.Success(txns.sortedByDescending { it.timestamp }.take(limit))
        }

    override fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>> =
        activeTransactions().map { txns ->
            LedgerResult.Success(
                txns.filter { !it.timestamp.isBefore(start) && !it.timestamp.isAfter(end) }
                    .sortedByDescending { it.timestamp },
            )
        }
}
