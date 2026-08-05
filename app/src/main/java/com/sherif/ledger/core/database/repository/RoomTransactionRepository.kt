package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.database.dao.TransactionDao
import com.sherif.ledger.core.database.mapper.toDomain
import com.sherif.ledger.core.database.mapper.toEntity
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.repository.AccountOriginCount
import com.sherif.ledger.core.domain.repository.FinancialEventRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val financialEventRepository: FinancialEventRepository,
) : TransactionRepository {

    init {
        LedgerLogger.d("EXECUTING: RoomTransactionRepository")
    }

    override fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>> =
        transactionDao.observeRecentTransactions(limit)
            .map { entities -> 
                val result: LedgerResult<List<Transaction>> = LedgerResult.Success(entities.map { it.toDomain() })
                result
            }
            .onEach { result ->
                if (result is LedgerResult.Success) {
                    LedgerLogger.d("Repository: observeRecentTransactions emission. Count: ${result.data.size}")
                }
            }
            .catch { e -> 
                LedgerLogger.e("Repository: observeRecentTransactions error", e)
                emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error"))) 
            }

    override fun observeTransactionsForAccount(accountId: Long): Flow<LedgerResult<List<Transaction>>> =
        transactionDao.observeTransactionsForAccount(accountId)
            .map { entities -> 
                val result: LedgerResult<List<Transaction>> = LedgerResult.Success(entities.map { it.toDomain() })
                result
            }
            .catch { e -> 
                emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error"))) 
            }

    override fun observeAllTransactions(): Flow<LedgerResult<List<Transaction>>> =
        transactionDao.observeAllTransactions()
            .map { entities ->
                val result: LedgerResult<List<Transaction>> = LedgerResult.Success(entities.map { it.toDomain() })
                result
            }
            .catch { e ->
                emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error")))
            }

    override fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>> =
        transactionDao.observeTransactionsBetween(start.toEpochMilli(), end.toEpochMilli())
            .map { entities -> 
                val result: LedgerResult<List<Transaction>> = LedgerResult.Success(entities.map { it.toDomain() })
                result
            }
            .onEach { result ->
                if (result is LedgerResult.Success) {
                    LedgerLogger.d("Repository: observeTransactionsBetween SUCCESS. Count: ${result.data.size}")
                }
            }
            .catch { e -> 
                LedgerLogger.e("Repository: observeTransactionsBetween error", e)
                emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error"))) 
            }

    override suspend fun getTransactionById(id: Long): LedgerResult<Transaction> = try {
        val entity = transactionDao.getTransactionById(id)
        if (entity != null) {
            LedgerResult.Success(entity.toDomain())
        } else {
            LedgerResult.Failure(LedgerError.Unknown("Transaction not found"))
        }
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun insertTransaction(transaction: Transaction): LedgerResult<Long> = try {
        val id = transactionDao.insertTransaction(transaction.toEntity())
        
        if (id == -1L) {
            LedgerLogger.d("Repository: insertTransaction IGNORED (Duplicate Fingerprint). PK: null")
            LedgerLogger.pipeline("Persistence", "Ignored: Duplicate fingerprint ${transaction.fingerprint.take(8)}...")
            LedgerResult.Failure(LedgerError.DuplicateTransaction)
        } else {
            LedgerLogger.d("Repository: insertTransaction SUCCESS. ID: $id, Rows Inserted: 1")
            LedgerLogger.pipeline("Persistence", "Inserted row ID: $id")
            
            // Integrity Check: Reload and Verify
            val reloaded = transactionDao.getTransactionById(id)?.toDomain()
            
            if (reloaded != null && 
                reloaded.amount.minorUnits == transaction.amount.minorUnits &&
                reloaded.fingerprint == transaction.fingerprint &&
                reloaded.timestamp.toEpochMilli() == transaction.timestamp.toEpochMilli()) {
                
                LedgerLogger.d("Repository: insertTransaction Integrity Verified. ID: $id")
                LedgerResult.Success(id)
            } else {
                LedgerLogger.e("Repository: insertTransaction Integrity Failure. Persisted data mismatch for ID: $id")
                LedgerResult.Failure(LedgerError.DatabaseFailure)
            }
        }
    } catch (e: Exception) {
        LedgerLogger.e("Repository: insertTransaction Failure", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun deleteTransaction(id: Long): LedgerResult<Unit> = try {
        val rowsUpdated = transactionDao.softDeleteTransaction(id)
        
        LedgerLogger.d("Repository: softDeleteTransaction PK: $id, Rows Updated: $rowsUpdated")

        if (rowsUpdated == 1) {
            // Event-first coexistence (ADR-0001, P7): void the mirror event so
            // event-sourced reads exclude this deleted transaction, matching the
            // legacy is_deleted filter. Best-effort — never fails the delete.
            try {
                financialEventRepository.voidByTransactionId(id)
            } catch (e: Exception) {
                LedgerLogger.e("deleteTransaction: could not void mirror event for tx=$id (transaction deleted regardless)", e)
            }
            LedgerResult.Success(Unit)
        } else {
            LedgerLogger.e("Repository: softDeleteTransaction FAILED. Affected rows: $rowsUpdated (Expected: 1)")
            LedgerResult.Failure(LedgerError.DatabaseFailure)
        }
    } catch (e: Exception) {
        LedgerLogger.e("Repository: softDeleteTransaction Failure", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun updateNote(id: Long, note: String?): LedgerResult<Unit> = try {
        val rowsUpdated = transactionDao.updateNote(id, note, System.currentTimeMillis())
        if (rowsUpdated == 1) {
            LedgerResult.Success(Unit)
        } else {
            LedgerLogger.e("Repository: updateNote FAILED. Affected rows: $rowsUpdated (Expected: 1)")
            LedgerResult.Failure(LedgerError.DatabaseFailure)
        }
    } catch (e: Exception) {
        LedgerLogger.e("Repository: updateNote failure", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String): List<AccountOriginCount> =
        transactionDao.countByOriginSignature(packageName, cardTail).map { AccountOriginCount(it.accountId, it.count) }

    override suspend fun reassignTransactions(
        fromAccountId: Long,
        packageName: String,
        cardTail: String,
        toAccountId: Long,
    ): Int = transactionDao.reassignByOriginSignature(fromAccountId, packageName, cardTail, toAccountId)

    override suspend fun reassignUntailedTransactions(
        fromAccountId: Long,
        packageName: String,
        toAccountId: Long,
    ): Int = transactionDao.reassignUntailedByOrigin(fromAccountId, packageName, toAccountId)

    override suspend fun reassignAllTransactions(fromAccountId: Long, toAccountId: Long): Int =
        transactionDao.reassignAllByAccount(fromAccountId, toAccountId)
}



