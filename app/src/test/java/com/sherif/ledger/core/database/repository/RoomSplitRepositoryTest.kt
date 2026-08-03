package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.ParticipantDao
import com.sherif.ledger.core.database.dao.SplitDao
import com.sherif.ledger.core.database.entity.ParticipantEntity
import com.sherif.ledger.core.database.entity.SplitEntity
import com.sherif.ledger.core.database.entity.SplitShareEntity
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.SplitType
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.ShareInput
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.split.SplitCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * All tests use an AED 421 transaction specifically because it does NOT divide
 * evenly by 3, exercising the remainder-to-self rule on real repository code,
 * not just the calculator in isolation.
 */
class RoomSplitRepositoryTest {

    private val transactionId = 1L
    private val totalMinor = 42100L // AED 421.00

    private class FakeTransactionRepository(private val amountMinor: Long) : TransactionRepository {
        override fun observeRecentTransactions(limit: Int) = throw NotImplementedError()
        override fun observeAllTransactions() = throw NotImplementedError()
        override fun observeTransactionsForAccount(accountId: Long) = throw NotImplementedError()
        override fun observeTransactionsBetween(start: Instant, end: Instant) = throw NotImplementedError()
        override suspend fun getTransactionById(id: Long): LedgerResult<Transaction> = LedgerResult.Success(
            Transaction(
                id = id, accountId = 1L, brandId = null, categoryId = null,
                amount = Money(amountMinor, CurrencyCode.AED), type = TransactionType.EXPENSE,
                timestamp = Instant.now(), source = IngestionSource.NOTIFICATION,
                rawText = "Texas Roadhouse", fingerprint = "split-test-fp",
            )
        )
        override suspend fun insertTransaction(transaction: Transaction) = throw NotImplementedError()
        override suspend fun deleteTransaction(id: Long) = throw NotImplementedError()
        override suspend fun updateNote(id: Long, note: String?) = throw NotImplementedError()
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String) = throw NotImplementedError()
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long) = throw NotImplementedError()
        override suspend fun reassignUntailedTransactions(fromAccountId: Long, packageName: String, toAccountId: Long) = throw NotImplementedError()
    }

    private class InMemorySplitDao : SplitDao {
        val splits = mutableMapOf<String, SplitEntity>()
        val shares = mutableMapOf<String, SplitShareEntity>()

        override fun observeByTransaction(transactionId: Long): Flow<SplitEntity?> =
            flowOf(splits.values.find { it.transactionId == transactionId })
        override suspend fun getByTransaction(transactionId: Long): SplitEntity? =
            splits.values.find { it.transactionId == transactionId }
        override suspend fun getById(splitId: String): SplitEntity? = splits[splitId]
        override suspend fun insertSplit(split: SplitEntity) { splits[split.id] = split }
        override suspend fun updateSplitType(splitId: String, type: SplitType, timestamp: Long) {
            splits[splitId] = splits.getValue(splitId).copy(splitType = type, updatedAt = timestamp)
        }
        override suspend fun touch(splitId: String, timestamp: Long) {
            splits[splitId]?.let { splits[splitId] = it.copy(updatedAt = timestamp) }
        }
        override suspend fun deleteSplit(splitId: String) {
            splits.remove(splitId)
            shares.values.filter { it.splitId == splitId }.forEach { shares.remove(it.id) }
        }
        override fun observeShares(splitId: String): Flow<List<SplitShareEntity>> =
            flowOf(shares.values.filter { it.splitId == splitId })
        override suspend fun getShares(splitId: String): List<SplitShareEntity> =
            shares.values.filter { it.splitId == splitId }
        override suspend fun getShareById(shareId: String): SplitShareEntity? = shares[shareId]
        override suspend fun insertShare(share: SplitShareEntity) { shares[share.id] = share }
        override suspend fun insertShares(shares: List<SplitShareEntity>) { shares.forEach { this.shares[it.id] = it } }
        override suspend fun deleteShareForParticipant(splitId: String, participantId: String) {
            shares.values.find { it.splitId == splitId && it.participantId == participantId }?.let { shares.remove(it.id) }
        }
        override suspend fun updateShareAmount(shareId: String, amountMinor: Long, percentage: Double?) {
            shares[shareId]?.let { shares[shareId] = it.copy(shareAmountMinor = amountMinor, percentage = percentage) }
        }
        override suspend fun updateSettled(shareId: String, settled: Boolean, settledAt: Long?) {
            shares[shareId]?.let { shares[shareId] = it.copy(isSettled = settled, settledAt = settledAt) }
        }
        override fun observeOutstandingForParticipant(participantId: String): Flow<List<SplitShareEntity>> =
            flowOf(shares.values.filter { it.participantId == participantId && !it.isSettled })
    }

    private class InMemoryParticipantDao : ParticipantDao {
        val participants = mutableMapOf<String, ParticipantEntity>()
        override fun observeAll(): Flow<List<ParticipantEntity>> = flowOf(participants.values.toList())
        override suspend fun getById(id: String): ParticipantEntity? = participants[id]
        override suspend fun getSelf(): ParticipantEntity? = participants.values.find { it.isSelf }
        override suspend fun insert(participant: ParticipantEntity) { participants[participant.id] = participant }
        override suspend fun delete(id: String) { participants.remove(id) }
    }

    private fun participant(id: String) = ParticipantEntity(id, id, isSelf = false, createdAt = 0L)

    private fun repository(splitDao: InMemorySplitDao = InMemorySplitDao()): Triple<RoomSplitRepository, InMemorySplitDao, InMemoryParticipantDao> {
        val participantDao = InMemoryParticipantDao()
        val repo = RoomSplitRepository(splitDao, participantDao, FakeTransactionRepository(totalMinor), SplitCalculator())
        return Triple(repo, splitDao, participantDao)
    }

    @Test fun `equal split creation divides evenly with self absorbing the remainder`() = runBlocking {
        val (repo, splitDao, _) = repository()
        val result = repo.createSplit(transactionId, SplitType.EQUAL, mapOf("ahmed" to ShareInput.Auto, "ali" to ShareInput.Auto))
        assertTrue(result is LedgerResult.Success)
        val splitId = (result as LedgerResult.Success).data

        // 42100 / 3 participants (you + ahmed + ali) = 14033 each for ahmed/ali
        val shares = splitDao.getShares(splitId)
        assertEquals(2, shares.size)
        shares.forEach { assertEquals(14033L, it.shareAmountMinor) }
    }

    @Test fun `adding a participant to an equal split recalculates every existing share`() = runBlocking {
        val (repo, splitDao, _) = repository()
        val createResult = repo.createSplit(transactionId, SplitType.EQUAL, mapOf("ahmed" to ShareInput.Auto))
        val splitId = (createResult as LedgerResult.Success).data
        // Before adding: you + ahmed = 2 total participants -> ahmed gets half
        assertEquals(21050L, splitDao.getShares(splitId).single().shareAmountMinor)

        val addResult = repo.addParticipant(splitId, "ali", ShareInput.Auto)
        assertTrue(addResult is LedgerResult.Success)

        // After adding: you + ahmed + ali = 3 total participants -> both recalculated to 14033
        val shares = splitDao.getShares(splitId)
        assertEquals(2, shares.size)
        shares.forEach { assertEquals(14033L, it.shareAmountMinor) }
    }

    @Test fun `removing a participant from an equal split recalculates the remaining shares`() = runBlocking {
        val (repo, splitDao, _) = repository()
        val createResult = repo.createSplit(
            transactionId, SplitType.EQUAL,
            mapOf("ahmed" to ShareInput.Auto, "ali" to ShareInput.Auto),
        )
        val splitId = (createResult as LedgerResult.Success).data
        splitDao.getShares(splitId).forEach { assertEquals(14033L, it.shareAmountMinor) }

        val removeResult = repo.removeParticipant(splitId, "ali")
        assertTrue(removeResult is LedgerResult.Success)

        // Only ahmed remains -> you + ahmed = 2 total participants -> ahmed gets half
        val remaining = splitDao.getShares(splitId)
        assertEquals(1, remaining.size)
        assertEquals(21050L, remaining.single().shareAmountMinor)
    }

    @Test fun `exact split rejects shares that would exceed the transaction total`() = runBlocking {
        val (repo, _, _) = repository()
        val result = repo.createSplit(
            transactionId, SplitType.EXACT,
            mapOf("ahmed" to ShareInput.Exact(30000L), "ali" to ShareInput.Exact(20000L)), // 30000+20000 > 42100
        )
        assertTrue(result is LedgerResult.Failure)
    }

    @Test fun `exact split accepts shares that leave a remainder for self`() = runBlocking {
        val (repo, splitDao, _) = repository()
        val result = repo.createSplit(
            transactionId, SplitType.EXACT,
            mapOf("ahmed" to ShareInput.Exact(10000L), "ali" to ShareInput.Exact(10000L)),
        )
        assertTrue(result is LedgerResult.Success)
        val splitId = (result as LedgerResult.Success).data
        assertEquals(20000L, splitDao.getShares(splitId).sumOf { it.shareAmountMinor })
    }

    @Test fun `removing a participant from an EXACT split leaves other shares untouched`() = runBlocking {
        val (repo, splitDao, _) = repository()
        val createResult = repo.createSplit(
            transactionId, SplitType.EXACT,
            mapOf("ahmed" to ShareInput.Exact(10000L), "ali" to ShareInput.Exact(15000L)),
        )
        val splitId = (createResult as LedgerResult.Success).data

        repo.removeParticipant(splitId, "ali")

        // Ahmed's exact amount must be UNCHANGED — removing ali never
        // implicitly redistributes their share onto ahmed.
        val remaining = splitDao.getShares(splitId)
        assertEquals(1, remaining.size)
        assertEquals(10000L, remaining.single().shareAmountMinor)
    }

    @Test fun `a second split cannot be created for a transaction that already has one`() = runBlocking {
        val (repo, _, _) = repository()
        repo.createSplit(transactionId, SplitType.EQUAL, mapOf("ahmed" to ShareInput.Auto))
        val second = repo.createSplit(transactionId, SplitType.EQUAL, mapOf("ali" to ShareInput.Auto))
        assertTrue(second is LedgerResult.Failure)
    }

    @Test fun `percentage split rejects percentages exceeding 100`() = runBlocking {
        val (repo, _, _) = repository()
        val result = repo.createSplit(
            transactionId, SplitType.PERCENTAGE,
            mapOf("ahmed" to ShareInput.Percent(60.0), "ali" to ShareInput.Percent(50.0)),
        )
        assertTrue(result is LedgerResult.Failure)
    }
}


