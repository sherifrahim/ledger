package com.sherif.ledger.feature.capture.reconciliation

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.service.transaction.FingerprintGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReconciliationEngineTest {

    private val fingerprintGenerator = FingerprintGenerator()
    private val engine = ReconciliationEngine(fingerprintGenerator)

    @Test
    fun `reconcile detects new unique transaction`() {
        val candidate = createCandidate("Amazon", 1000L)
        val result = engine.reconcile(candidate, emptyList())
        
        assertTrue(result is ReconciliationResult.New)
    }

    @Test
    fun `reconcile detects duplicate via fingerprint`() {
        val timestamp = Instant.now()
        val candidate = createCandidate("Amazon", 1000L, timestamp)
        val existing = listOf(createTransaction(1L, "Amazon", 1000L, timestamp))
        
        val result = engine.reconcile(candidate, existing)
        
        assertTrue(result is ReconciliationResult.Duplicate)
    }

    @Test
    fun `reconcile detects duplicate via fuzzy matching within 1 minute`() {
        val timestamp = Instant.now()
        val candidate = createCandidate("Amazon", 1000L, timestamp)
        val existing = listOf(createTransaction(1L, "Amazon", 1000L, timestamp.minus(30, ChronoUnit.SECONDS)))
        
        val result = engine.reconcile(candidate, existing)
        
        // 70 (Amount) + 30 (Time) + 10 (Type) = 100 -> Duplicate
        assertTrue(result is ReconciliationResult.Duplicate)
    }

    @Test
    fun `reconcile detects update for small time drift and matching amount`() {
        val timestamp = Instant.now()
        val candidate = createCandidate("Amazon", 1000L, timestamp)
        // Simulate a drift that yields ~95% confidence (e.g. 15 mins drift -> 15 points)
        // 70 (Amount) + 15 (Time) + 10 (Type) = 95
        val existing = listOf(createTransaction(1L, "Amazon", 1000L, timestamp.minus(15, ChronoUnit.MINUTES)))
        
        val result = engine.reconcile(candidate, existing)
        
        assertTrue(result is ReconciliationResult.Updated)
    }

    private fun createCandidate(merchant: String, amount: Long, timestamp: Instant = Instant.now()) = 
        TransactionCandidate(
            source = IngestionSource.MANUAL,
            rawText = merchant,
            merchantName = merchant,
            amountMinor = amount,
            currencyCode = CurrencyCode.AED,
            timestamp = timestamp,
            accountHint = null,
            transactionType = TransactionType.EXPENSE
        )

    private fun createTransaction(id: Long, merchant: String, amount: Long, timestamp: Instant) = 
        Transaction(
            id = id,
            accountId = 1L,
            brandId = null,
            categoryId = null,
            amount = Money(amount, CurrencyCode.AED),
            type = TransactionType.EXPENSE,
            timestamp = timestamp,
            source = IngestionSource.MANUAL,
            rawText = merchant,
            fingerprint = fingerprintGenerator.generate(com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase.Params(
                accountId = 1L,
                amountMinor = amount,
                currencyCode = CurrencyCode.AED,
                type = TransactionType.EXPENSE,
                timestamp = timestamp,
                source = IngestionSource.MANUAL,
                rawMerchantText = merchant
            ))
        )
}
