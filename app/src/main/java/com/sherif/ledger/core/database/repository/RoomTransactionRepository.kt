package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.TransactionDao
import com.sherif.ledger.core.database.mapper.toDomain
import com.sherif.ledger.core.database.mapper.toEntity
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>> =
        transactionDao.observeRecentTransactions(limit)
            .map { entities -> 
                val result: LedgerResult<List<Transaction>> = LedgerResult.Success(entities.map { it.toDomain() })
                result
            }
            .catch { e -> 
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

    override fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>> =
        transactionDao.observeTransactionsBetween(start.toEpochMilli(), end.toEpochMilli())
            .map { entities -> 
                val result: LedgerResult<List<Transaction>> = LedgerResult.Success(entities.map { it.toDomain() })
                result
            }
            .catch { e -> 
                emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error"))) 
            }

    override suspend fun insertTransaction(transaction: Transaction): LedgerResult<Long> = try {
        val id = transactionDao.insertTransaction(transaction.toEntity())
        if (id == -1L) {
            LedgerResult.Failure(LedgerError.DuplicateTransaction)
        } else {
            LedgerResult.Success(id)
        }
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun deleteTransaction(id: Long): LedgerResult<Unit> = try {
        transactionDao.softDeleteTransaction(id)
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }
}
