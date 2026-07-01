package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.AccountDao
import com.sherif.ledger.core.database.mapper.toDomain
import com.sherif.ledger.core.database.mapper.toEntity
import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomAccountRepository @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> =
        accountDao.observeAllAccounts()
            .map { entities -> 
                val result: LedgerResult<List<Account>> = LedgerResult.Success(entities.map { it.toDomain() })
                result
            }
            .catch { e -> 
                emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error"))) 
            }

    override suspend fun getAccountById(id: Long): LedgerResult<Account> = try {
        val entity = accountDao.getAccountById(id)
        if (entity != null) {
            LedgerResult.Success(entity.toDomain())
        } else {
            LedgerResult.Failure(LedgerError.AccountNotFound)
        }
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun insertAccount(account: Account): LedgerResult<Long> = try {
        val id = accountDao.insertAccount(account.toEntity())
        LedgerResult.Success(id)
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun updateAccount(account: Account): LedgerResult<Unit> = try {
        accountDao.updateAccount(account.toEntity())
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun deleteAccount(id: Long): LedgerResult<Unit> = try {
        accountDao.softDeleteAccount(id)
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }
}
