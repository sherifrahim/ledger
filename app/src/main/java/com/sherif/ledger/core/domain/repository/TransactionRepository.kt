package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** How many transactions with a given origin signature are currently bound to a
 *  given account. Read-only projection — never used to mutate anything by itself. */
data class AccountOriginCount(val accountId: Long, val count: Int)

interface TransactionRepository {
    fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>>
    fun observeAllTransactions(): Flow<LedgerResult<List<Transaction>>>
    fun observeTransactionsForAccount(accountId: Long): Flow<LedgerResult<List<Transaction>>>
    fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>>
    suspend fun getTransactionById(id: Long): LedgerResult<Transaction>
    suspend fun insertTransaction(transaction: Transaction): LedgerResult<Long>
    suspend fun deleteTransaction(id: Long): LedgerResult<Unit>

    /** Sets or clears the single user-authored note on a transaction. Passing
     *  null clears it. Never consulted by any balance or analytics computation —
     *  purely descriptive metadata. */
    suspend fun updateNote(id: Long, note: String?): LedgerResult<Unit>

    /** Bounded, indexed, read-only: how many transactions with this exact
     *  (package, tail) origin signature currently sit on each account. Powers
     *  AccountIdentityResolver's historical-match and repeated-observation
     *  evidence. Never mutates anything. */
    suspend fun countTransactionsByOrigin(packageName: String, cardTail: String): List<AccountOriginCount>

    /** Explicit, bounded, transactional, idempotent reassignment of transactions
     *  matching an exact origin signature from one account to another. Returns the
     *  number of rows affected. Re-running after a successful reassignment affects
     *  zero rows, since no matching rows remain on [fromAccountId]. Never invoked
     *  automatically as a side effect of ingestion. */
    suspend fun reassignTransactions(
        fromAccountId: Long,
        packageName: String,
        cardTail: String,
        toAccountId: Long,
    ): Int
}


