package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.LedgerResult
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAllAccounts(): Flow<LedgerResult<List<Account>>>
    suspend fun getAccountById(id: Long): LedgerResult<Account>
    suspend fun insertAccount(account: Account): LedgerResult<Long>
    suspend fun updateAccount(account: Account): LedgerResult<Unit>
    suspend fun deleteAccount(id: Long): LedgerResult<Unit>

    /** Soft-deleted accounts — invisible to [observeAllAccounts] and every balance computation. Diagnostics-only (Balance Inspector). */
    suspend fun getDeletedAccounts(): LedgerResult<List<Account>>

    /** RC7 Phase B: unresolved-institution accounts (Account.isCandidate) — invisible to [observeAllAccounts]/every balance figure until promoted or dismissed. Developer Console only. */
    fun observeCandidateAccounts(): Flow<LedgerResult<List<Account>>>
}
