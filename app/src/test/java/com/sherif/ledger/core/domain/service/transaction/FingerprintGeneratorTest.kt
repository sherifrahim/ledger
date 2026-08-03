package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant

class FingerprintGeneratorTest {

    private val generator = FingerprintGenerator()

    @Test
    fun `generates identical fingerprint for same data`() {
        val timestamp = Instant.ofEpochMilli(1719878400000L)
        val params1 = createParams(timestamp)
        val params2 = createParams(timestamp)

        assertEquals(generator.generate(params1), generator.generate(params2))
    }

    @Test
    fun `generates different fingerprint for different data`() {
        val timestamp = Instant.now()
        val params1 = createParams(timestamp, amount = 1000L)
        val params2 = createParams(timestamp, amount = 2000L)

        assertNotEquals(generator.generate(params1), generator.generate(params2))
    }

    @Test
    fun `fingerprint identifies the event, not the account it was attributed to`() {
        // The same purchase captured through the bank's own app and through an SMS
        // mirror resolves to two different accounts, because account identity is
        // decided AFTER reconciliation. If the account took part in the fingerprint,
        // the unique index would let both rows in and the reconciliation engine's
        // exact-match branch could never fire at all — it fingerprints candidates
        // with no account (0) while every persisted row has a real one.
        val timestamp = Instant.now()

        assertEquals(
            generator.generate(createParams(timestamp, accountId = 2L)),
            generator.generate(createParams(timestamp, accountId = 7L)),
        )
    }

    @Test
    fun `a candidate fingerprinted before attribution matches the row it becomes`() {
        val timestamp = Instant.now()
        val beforeAccountIsKnown = generator.generate(createParams(timestamp, accountId = 0L))
        val asPersisted = generator.generate(createParams(timestamp, accountId = 5L))

        assertEquals(beforeAccountIsKnown, asPersisted)
    }

    private fun createParams(timestamp: Instant, amount: Long = 1000L, accountId: Long = 1L) =
        InsertTransactionUseCase.Params(
            accountId = accountId,
            amountMinor = amount,
            currencyCode = CurrencyCode.AED,
            type = TransactionType.EXPENSE,
            timestamp = timestamp,
            source = IngestionSource.MANUAL,
            rawMerchantText = "Amazon"
        )
}
