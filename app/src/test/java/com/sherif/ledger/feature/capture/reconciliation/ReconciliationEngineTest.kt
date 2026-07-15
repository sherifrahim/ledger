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
        
        // 40 (Amount) + 30 (Merchant) + 20 (Time, <=1min) + 10 (Type) = 100 -> Duplicate
        assertTrue(result is ReconciliationResult.Duplicate)
    }

    @Test
    fun `reconcile detects update for small time drift and matching amount`() {
        val timestamp = Instant.now()
        val candidate = createCandidate("Amazon", 1000L, timestamp)
        // 40 (Amount) + 30 (Merchant) + 10 (Time, <=30min) + 10 (Type) = 90 -> Updated
        val existing = listOf(createTransaction(1L, "Amazon", 1000L, timestamp.minus(15, ChronoUnit.MINUTES)))
        
        val result = engine.reconcile(candidate, existing)
        
        assertTrue(result is ReconciliationResult.Updated)
    }

    // ---- RC1: amount/tail/time outweigh exact merchant wording ----

    @Test
    fun `reconcile catches a cross-channel duplicate even when merchant text differs, via matching tail`() {
        // Simulates the real bug: the same bank event captured once via push
        // notification and once via SMS, with slightly different extracted
        // merchant text, but the same card tail.
        val timestamp = Instant.now()
        val candidate = createCandidate("AED 150.00 debited", 15_000L, timestamp, tail = "6989")
        val existing = listOf(
            createTransaction(1L, "Payment of AED 150.00 processed", 15_000L, timestamp.minus(2, ChronoUnit.MINUTES), tail = "6989")
        )

        val result = engine.reconcile(candidate, existing)

        // 40 (Amount) + 0 (Merchant mismatch) + 30 (Tail) + 15 (Time, <=5min) + 10 (Type) = 95 -> Updated (no duplicate insert)
        assertTrue("Cross-channel duplicate must not be classified New", result !is ReconciliationResult.New)
    }

    @Test
    fun `reconcile never merges a different amount regardless of any other signal`() {
        val timestamp = Instant.now()
        val candidate = createCandidate("Amazon", 1000L, timestamp, tail = "1234")
        val existing = listOf(createTransaction(1L, "Amazon", 2000L, timestamp, tail = "1234"))

        val result = engine.reconcile(candidate, existing)

        assertTrue(result is ReconciliationResult.New)
    }

    @Test
    fun `reconcile does not merge two genuinely different transactions with no tail and different merchants far apart`() {
        val timestamp = Instant.now()
        val candidate = createCandidate("Carrefour", 1000L, timestamp)
        val existing = listOf(createTransaction(1L, "Amazon", 1000L, timestamp.minus(10, ChronoUnit.HOURS)))

        val result = engine.reconcile(candidate, existing)

        assertTrue(result is ReconciliationResult.New)
    }

    private fun createCandidate(merchant: String, amount: Long, timestamp: Instant = Instant.now(), tail: String? = null) = 
        TransactionCandidate(
            source = IngestionSource.MANUAL,
            rawText = merchant,
            merchantName = merchant,
            amountMinor = amount,
            currencyCode = CurrencyCode.AED,
            timestamp = timestamp,
            accountHint = tail,
            transactionType = TransactionType.EXPENSE
        )

    private fun createTransaction(id: Long, merchant: String, amount: Long, timestamp: Instant, tail: String? = null) = 
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
            cardTail = tail,
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




