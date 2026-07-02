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

    private fun createParams(timestamp: Instant, amount: Long = 1000L) = 
        InsertTransactionUseCase.Params(
            accountId = 1L,
            amountMinor = amount,
            currencyCode = CurrencyCode.AED,
            type = TransactionType.EXPENSE,
            timestamp = timestamp,
            source = IngestionSource.MANUAL,
            rawMerchantText = "Amazon"
        )
}
