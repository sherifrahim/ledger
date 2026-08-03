package com.sherif.ledger.feature.capture.reconciliation

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
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
        // 40 (Amount) + 30 (Tail) + 10 (Time, <=30min) + 10 (Type) = 90 -> Updated.
        // Merchant wording differs, so the fingerprint does not match; 15 minutes is
        // outside the same-event window, so only confidence scoring can decide.
        val candidate = createCandidate("Amazon Ae", 1000L, timestamp, tail = "1959")
        val existing = listOf(
            createTransaction(1L, "Amazon", 1000L, timestamp.minus(15, ChronoUnit.MINUTES), tail = "1959"),
        )

        val result = engine.reconcile(candidate, existing)

        assertTrue("Expected Updated, was: $result", result is ReconciliationResult.Updated)
    }

    @Test
    fun `identical wording in the same hour is an exact fingerprint match`() {
        // Guards the branch that was dead until the account id left the fingerprint:
        // the candidate is fingerprinted before it has an account, every persisted
        // row after it has one, so the two could never be equal.
        val timestamp = Instant.now()
        val candidate = createCandidate("Amazon", 1000L, timestamp)
        val existing = listOf(createTransaction(1L, "Amazon", 1000L, timestamp.minus(15, ChronoUnit.MINUTES)))

        val result = engine.reconcile(candidate, existing)

        assertTrue("Expected Duplicate, was: $result", result is ReconciliationResult.Duplicate)
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

    // ---- Structural same-event dedup, from the owner's real device database ----
    //
    // Every case below is a row pair that actually exists (or actually does NOT
    // exist as a duplicate) in the database pulled from the phone, not an invented
    // scenario. Amounts, timestamps, tails and merchant strings are the real ones.

    @Test
    fun `the real AED 3000 pair, tailed and untailed 85 seconds apart, is one event`() {
        // ids 238/239 on the device: one ADCB ATM withdrawal that produced a tailed
        // row ("Atm-index Exc Hamdaan", tail 920001) and an untailed row with no
        // merchant at all ("Unknown"). Confidence scoring reaches only
        // 40 + 20 + 10 = 70, under the 90 threshold, so BOTH were persisted — and
        // because account identity is resolved after this point they landed on two
        // different accounts.
        val existingTime = Instant.parse("2026-06-24T10:34:25.833Z")
        val existing = listOf(
            createTransaction(238L, "Atm-index Exc Hamdaan", 300_000L, existingTime, tail = "920001"),
        )
        val candidate = createCandidate(
            "Unknown", 300_000L, existingTime.plus(85, ChronoUnit.SECONDS), tail = null,
        )

        val result = engine.reconcile(candidate, existing)

        assertTrue(
            "One withdrawal captured twice must not become two transactions (was: $result)",
            result is ReconciliationResult.Duplicate,
        )
    }

    @Test
    fun `the real AED 900 pair with two different tails is a transfer, not a duplicate`() {
        // ids 141/142 on the device: AED 900.00 leaving the MBank account (tail
        // 000001) and arriving in the ADCB account (tail 920001) 41 seconds later.
        // Two legs of one transfer between the owner's own accounts. Merging them
        // would erase a real leg and silently change the balance of both accounts.
        val existingTime = Instant.parse("2026-06-03T04:45:08.786Z")
        val existing = listOf(
            createTransaction(
                142L, "Unknown", 90_000L, existingTime, tail = "000001",
                type = TransactionType.EXPENSE,
            ),
        )
        val candidate = createCandidate(
            "Income", 90_000L, existingTime.plus(41, ChronoUnit.SECONDS), tail = "920001",
            type = TransactionType.INCOME,
        )

        val result = engine.reconcile(candidate, existing)

        assertTrue("Two different tails are two different accounts (was: $result)", result is ReconciliationResult.New)
    }

    @Test
    fun `an inflow never duplicates an outflow of the same amount at the same moment`() {
        // Same untailed sender on both sides, so only direction distinguishes them —
        // the eandINF 5.00 pair (ids 341/342), 6 seconds apart.
        val existingTime = Instant.now()
        val existing = listOf(
            createTransaction(341L, "Your", 500L, existingTime, type = TransactionType.INCOME),
        )
        val candidate = createCandidate(
            "Subscribe", 500L, existingTime.plus(6, ChronoUnit.SECONDS), type = TransactionType.EXPENSE,
        )

        val result = engine.reconcile(candidate, existing)

        assertTrue("Opposite directions are not one event (was: $result)", result is ReconciliationResult.New)
    }

    @Test
    fun `an outgoing transfer duplicates an expense of the same amount and moment`() {
        // The genuine cross-channel shape: the bank app calls it a card purchase
        // while the SMS mirror is parsed as an outgoing transfer. Both move the same
        // money the same way, so they are one event despite the differing type.
        val existingTime = Instant.now()
        val existing = listOf(
            createTransaction(1L, "Adidas", 29_610L, existingTime, tail = "1959"),
        )
        val candidate = createCandidate(
            "Ounass Uae", 29_610L, existingTime.plus(70, ChronoUnit.SECONDS), tail = "1959",
            type = TransactionType.TRANSFER, direction = TransferDirection.OUTGOING,
        )

        val result = engine.reconcile(candidate, existing)

        assertTrue("Same direction of travel, same amount, same tail (was: $result)", result is ReconciliationResult.Duplicate)
    }

    @Test
    fun `two identical purchases six minutes apart stay separate`() {
        // Guards the window itself: outside it, an untailed candidate with different
        // merchant wording must fall through to ordinary scoring and stay New.
        val existingTime = Instant.now()
        val existing = listOf(createTransaction(1L, "Costa Coffee", 2_100L, existingTime, tail = "1959"))
        val candidate = createCandidate("Cars Taxi", 2_100L, existingTime.plus(6, ChronoUnit.MINUTES))

        val result = engine.reconcile(candidate, existing)

        assertTrue("Outside the same-event window (was: $result)", result is ReconciliationResult.New)
    }

    @Test
    fun `different currencies of the same numeric amount are never one event`() {
        val existingTime = Instant.now()
        val existing = listOf(createTransaction(1L, "Swiggy", 50_000L, existingTime, currency = CurrencyCode.INR))
        val candidate = createCandidate("Swiggy", 50_000L, existingTime, currency = CurrencyCode.AED)

        val result = engine.reconcile(candidate, existing)

        assertTrue("INR 500 is not AED 500 (was: $result)", result is ReconciliationResult.New)
    }

    private fun createCandidate(
        merchant: String,
        amount: Long,
        timestamp: Instant = Instant.now(),
        tail: String? = null,
        type: TransactionType = TransactionType.EXPENSE,
        direction: TransferDirection? = null,
        currency: CurrencyCode = CurrencyCode.AED,
    ) =
        TransactionCandidate(
            source = IngestionSource.MANUAL,
            rawText = merchant,
            merchantName = merchant,
            amountMinor = amount,
            currencyCode = currency,
            timestamp = timestamp,
            accountHint = tail,
            transactionType = type,
            transferDirection = direction,
        )

    private fun createTransaction(
        id: Long,
        merchant: String,
        amount: Long,
        timestamp: Instant,
        tail: String? = null,
        type: TransactionType = TransactionType.EXPENSE,
        direction: TransferDirection? = null,
        currency: CurrencyCode = CurrencyCode.AED,
    ) =
        Transaction(
            id = id,
            accountId = 1L,
            brandId = null,
            categoryId = null,
            amount = Money(amount, currency),
            type = type,
            timestamp = timestamp,
            source = IngestionSource.MANUAL,
            rawText = merchant,
            cardTail = tail,
            transferDirection = direction,
            fingerprint = fingerprintGenerator.generate(com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase.Params(
                accountId = 1L,
                amountMinor = amount,
                currencyCode = currency,
                type = type,
                timestamp = timestamp,
                source = IngestionSource.MANUAL,
                rawMerchantText = merchant
            ))
        )
}





