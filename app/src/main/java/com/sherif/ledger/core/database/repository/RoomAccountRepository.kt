package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.common.logging.LedgerLogger
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
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class RoomAccountRepository @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    init {
        LedgerLogger.d("EXECUTING: RoomAccountRepository")
    }

    override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> =
        accountDao.observeAllAccounts()
            .map { entities -> 
                val result: LedgerResult<List<Account>> = LedgerResult.Success(entities.map { it.toDomain() })
                result
            }
            .onEach { result ->
                if (result is LedgerResult.Success) {
                    LedgerLogger.d("Repository: observeAllAccounts emission. Count: ${result.data.size}")
                }
            }
            .catch { e -> 
                LedgerLogger.e("Repository: observeAllAccounts error", e)
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
        
        LedgerLogger.d("Repository: insertAccount SUCCESS. ID: $id, Rows Inserted: 1")
        
        // Integrity Check: Reload and Verify
        val reloaded = accountDao.getAccountById(id)
        if (reloaded != null && reloaded.id == id) {
            LedgerResult.Success(id)
        } else {
            LedgerLogger.e("Repository: insertAccount Integrity Failure. ID mismatch or not found after insert.")
            LedgerResult.Failure(LedgerError.DatabaseFailure)
        }
    } catch (e: Exception) {
        LedgerLogger.e("Repository: insertAccount Failure", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun updateAccount(account: Account): LedgerResult<Unit> = try {
        val balanceBefore = accountDao.getAccountById(account.id)?.openingBalanceMinor
        
        val rowsUpdated = accountDao.updateAccount(account.toEntity())
        
        LedgerLogger.d("Repository: updateAccount PK: ${account.id}, Rows Updated: $rowsUpdated")
        LedgerLogger.pipeline("Persistence", "Account update: PK=${account.id}, NewOpeningBal=${account.openingBalance.minorUnits}")
        
        if (rowsUpdated != 1) {
            LedgerLogger.e("Repository: updateAccount FAILED. Affected rows: $rowsUpdated (Expected: 1)")
            LedgerResult.Failure(LedgerError.DatabaseFailure)
        } else {
            // Integrity Check: Reload and Verify
            val reloaded = accountDao.getAccountById(account.id)
            val balancePersisted = account.openingBalance.minorUnits
            val balanceReloaded = reloaded?.openingBalanceMinor
            
            LedgerLogger.d("Repository: updateAccount Balance Audit - Before: $balanceBefore, Persisted: $balancePersisted, Reloaded: $balanceReloaded")
            
            if (balanceReloaded == balancePersisted) {
                LedgerResult.Success(Unit)
            } else {
                LedgerLogger.e("Repository: updateAccount Integrity Failure. Persisted value mismatch.")
                LedgerResult.Failure(LedgerError.DatabaseFailure)
            }
        }
    } catch (e: Exception) {
        LedgerLogger.e("Repository: updateAccount Failure", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun deleteAccount(id: Long): LedgerResult<Unit> = try {
        val rowsUpdated = accountDao.softDeleteAccount(id)
        
        LedgerLogger.d("Repository: softDeleteAccount PK: $id, Rows Updated: $rowsUpdated")
        
        if (rowsUpdated == 1) {
            LedgerResult.Success(Unit)
        } else {
            LedgerLogger.e("Repository: softDeleteAccount FAILED. Affected rows: $rowsUpdated (Expected: 1)")
            LedgerResult.Failure(LedgerError.AccountNotFound)
        }
    } catch (e: Exception) {
        LedgerLogger.e("Repository: softDeleteAccount Failure", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }
}

