package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * The read side of transaction history, as a list of [Transaction] domain objects.
 *
 * ADR-0001, P7 (event-first reads): production list reads (Dashboard, Insights,
 * Accounts activity, Merchant, Review, Search) depend on THIS interface, not on
 * [TransactionRepository] directly, so their source can be swapped without touching
 * any feature. The bound implementation reads canonical FinancialEvents; the
 * legacy Transaction path stays only where a read needs Transaction-only fields
 * (balance via `AccountBalanceService`; the transaction-detail record view) — those
 * are documented in docs/READ_PARITY_REPORT.md. Read-only: no writes here.
 */
interface TransactionReadSource {
    fun observeAllTransactions(): Flow<LedgerResult<List<Transaction>>>
    fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>>
    fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>>
}
