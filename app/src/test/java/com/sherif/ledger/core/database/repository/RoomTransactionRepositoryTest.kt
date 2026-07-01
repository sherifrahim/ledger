package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.TransactionDao
import com.sherif.ledger.core.database.entity.TransactionEntity
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RoomTransactionRepositoryTest {

    private val fakeDao = object : TransactionDao {
        override fun observeRecentTransactions(limit: Int): Flow<List<TransactionEntity>> {
            return flowOf(listOf(
                TransactionEntity(
                    id = 1,
                    accountId = 1,
                    brandId = null,
                    categoryId = null,
                    amountMinor = 1000,
                    currencyCode = CurrencyCode.AED,
                    type = TransactionType.EXPENSE,
                    timestampEpochMillis = 1000L,
                    source = IngestionSource.MANUAL,
                    rawText = null,
                    fingerprint = "f1"
                )
            ))
        }

        override fun observeTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun observeTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun insertTransaction(transaction: TransactionEntity): Long = 1L
        override suspend fun softDeleteTransaction(id: Long, timestamp: Long) {}
    }

    private val repository = RoomTransactionRepository(fakeDao)

    @Test
    fun `observeRecentTransactions maps entities correctly`() = runBlocking {
        val result = repository.observeRecentTransactions(10).first()
        
        assertTrue(result is LedgerResult.Success)
        val transactions = (result as LedgerResult.Success).data
        assertEquals(1, transactions.size)
        assertEquals(1000L, transactions[0].amount.minorUnits)
        assertEquals(CurrencyCode.AED, transactions[0].amount.currencyCode)
    }

    @Test
    fun `insertTransaction returns Success on valid insertion`() = runBlocking {
        val transaction = Transaction(
            id = 0,
            accountId = 1,
            brandId = null,
            categoryId = null,
            amount = Money(1000L, CurrencyCode.AED),
            type = TransactionType.EXPENSE,
            timestamp = Instant.now(),
            source = IngestionSource.MANUAL,
            rawText = null,
            fingerprint = "new_f"
        )
        
        val result = repository.insertTransaction(transaction)
        
        assertTrue(result is LedgerResult.Success)
        assertEquals(1L, (result as LedgerResult.Success).data)
    }
}
