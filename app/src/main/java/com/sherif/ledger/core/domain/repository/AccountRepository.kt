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
}
