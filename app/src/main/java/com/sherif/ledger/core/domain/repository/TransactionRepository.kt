package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TransactionRepository {
    fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>>
    fun observeTransactionsForAccount(accountId: Long): Flow<LedgerResult<List<Transaction>>>
    fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>>
    suspend fun insertTransaction(transaction: Transaction): LedgerResult<Long>
    suspend fun deleteTransaction(id: Long): LedgerResult<Unit>
}
