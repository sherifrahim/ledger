package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.service.event.FinancialEventFactory
import com.sherif.ledger.testsupport.FakeFinancialEventRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class EventSourcedTransactionReadSourceTest {

    private fun txn(id: Long, epochMs: Long) = Transaction(
        id = id, accountId = 1, brandId = null, categoryId = null,
        amount = Money(100L * id, CurrencyCode.AED), type = TransactionType.EXPENSE,
        timestamp = Instant.ofEpochMilli(epochMs), source = IngestionSource.SMS,
        rawText = "m$id", fingerprint = "fp$id",
    )

    private suspend fun seed(): FakeFinancialEventRepository {
        val fake = FakeFinancialEventRepository()
        fake.record(FinancialEventFactory.mirrorOf(txn(1, 1_000)))
        fake.record(FinancialEventFactory.mirrorOf(txn(2, 3_000)))
        fake.record(FinancialEventFactory.mirrorOf(txn(3, 2_000)))
        return fake
    }

    @Test
    fun `observeAll returns active events as transactions, ascending by time`() = runBlocking {
        val src = EventSourcedTransactionReadSource(seed())
        val ids = (src.observeAllTransactions().first() as LedgerResult.Success).data.map { it.id }
        assertEquals(listOf(1L, 3L, 2L), ids) // 1000, 2000, 3000
    }

    @Test
    fun `voided (soft-deleted) events are excluded, matching is_deleted filter`() = runBlocking {
        val fake = seed()
        fake.voidByTransactionId(2) // soft-delete tx 2
        val src = EventSourcedTransactionReadSource(fake)
        val ids = (src.observeAllTransactions().first() as LedgerResult.Success).data.map { it.id }
        assertEquals(listOf(1L, 3L), ids)
    }

    @Test
    fun `observeRecent is descending and limited`() = runBlocking {
        val src = EventSourcedTransactionReadSource(seed())
        val ids = (src.observeRecentTransactions(2).first() as LedgerResult.Success).data.map { it.id }
        assertEquals(listOf(2L, 3L), ids) // 3000, 2000
    }

    @Test
    fun `observeBetween filters by the time window`() = runBlocking {
        val src = EventSourcedTransactionReadSource(seed())
        val data = (src.observeTransactionsBetween(Instant.ofEpochMilli(1_500), Instant.ofEpochMilli(2_500))
            .first() as LedgerResult.Success).data
        assertEquals(listOf(3L), data.map { it.id })
    }
}
