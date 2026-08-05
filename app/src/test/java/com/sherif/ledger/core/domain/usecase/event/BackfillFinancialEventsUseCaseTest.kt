package com.sherif.ledger.core.domain.usecase.event

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountOriginCount
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.event.FinancialEventFactory
import com.sherif.ledger.testsupport.FakeFinancialEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BackfillFinancialEventsUseCaseTest {

    private fun txn(id: Long, fp: String) = Transaction(
        id = id, accountId = 1, brandId = null, categoryId = null,
        amount = Money(100L * id, CurrencyCode.AED), type = TransactionType.EXPENSE,
        timestamp = Instant.ofEpochMilli(id * 1000), source = com.sherif.ledger.core.domain.model.IngestionSource.SMS,
        rawText = "merchant-$id", fingerprint = fp,
    )

    private fun useCase(txns: List<Transaction>, events: FakeFinancialEventRepository) =
        BackfillFinancialEventsUseCase(FakeTransactionRepo(txns), events)

    @Test
    fun `creates a mirror event for every transaction`() = runBlocking {
        val events = FakeFinancialEventRepository()
        val report = useCase(listOf(txn(1, "a"), txn(2, "b"), txn(3, "c")), events).execute()

        assertEquals(3, report.scanned)
        assertEquals(3, report.created)
        assertEquals(0, report.skipped)
        assertEquals(0, report.failures)
        assertEquals(3, events.events.size)
        assertTrue(report.verified)
    }

    @Test
    fun `is idempotent and resumable — a second run creates nothing`() = runBlocking {
        val events = FakeFinancialEventRepository()
        val txns = listOf(txn(1, "a"), txn(2, "b"))
        useCase(txns, events).execute()

        val second = useCase(txns, events).execute()
        assertEquals(2, second.scanned)
        assertEquals(0, second.created)
        assertEquals(2, second.skipped)
        assertEquals(2, events.events.size)
        assertTrue(second.verified)
    }

    @Test
    fun `only backfills the transactions that are missing a mirror`() = runBlocking {
        val events = FakeFinancialEventRepository()
        // Pretend dual-write already mirrored tx 1.
        events.record(FinancialEventFactory.mirrorOf(txn(1, "a")))

        val report = useCase(listOf(txn(1, "a"), txn(2, "b"), txn(3, "c")), events).execute()
        assertEquals(1, report.skipped)
        assertEquals(2, report.created)
        assertEquals(3, events.events.size)
        assertTrue(report.verified)
    }

    @Test
    fun `no transactions is a verified no-op`() = runBlocking {
        val report = useCase(emptyList(), FakeFinancialEventRepository()).execute()
        assertEquals(0, report.scanned)
        assertEquals(0, report.created)
        assertTrue(report.verified)
    }
}

private class FakeTransactionRepo(private val txns: List<Transaction>) : TransactionRepository {
    override fun observeAllTransactions(): Flow<LedgerResult<List<Transaction>>> =
        flowOf(LedgerResult.Success(txns))
    override fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>> =
        flowOf(LedgerResult.Success(txns.take(limit)))
    override fun observeTransactionsForAccount(accountId: Long): Flow<LedgerResult<List<Transaction>>> =
        flowOf(LedgerResult.Success(emptyList()))
    override fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>> =
        flowOf(LedgerResult.Success(emptyList()))
    override suspend fun getTransactionById(id: Long): LedgerResult<Transaction> =
        txns.firstOrNull { it.id == id }?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(com.sherif.ledger.core.domain.model.LedgerError.Unknown("not found"))
    override suspend fun insertTransaction(transaction: Transaction): LedgerResult<Long> = LedgerResult.Success(transaction.id)
    override suspend fun deleteTransaction(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
    override suspend fun updateNote(id: Long, note: String?): LedgerResult<Unit> = LedgerResult.Success(Unit)
    override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String): List<AccountOriginCount> = emptyList()
    override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long): Int = 0
    override suspend fun reassignUntailedTransactions(fromAccountId: Long, packageName: String, toAccountId: Long): Int = 0
    override suspend fun reassignAllTransactions(fromAccountId: Long, toAccountId: Long): Int = 0
}
