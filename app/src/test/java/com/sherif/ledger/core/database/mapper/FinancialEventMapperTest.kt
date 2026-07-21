package com.sherif.ledger.core.database.mapper

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.FinancialEvent
import com.sherif.ledger.core.domain.model.FinancialEventStatus
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FinancialEventMapperTest {

    private fun sample() = FinancialEvent(
        id = "evt-123",
        transactionId = 42L,
        accountId = 7L,
        brandId = 3L,
        categoryId = 9L,
        amount = Money(12_345L, CurrencyCode.AED),
        type = TransactionType.EXPENSE,
        timestamp = Instant.ofEpochMilli(1_700_000_000_000L),
        source = IngestionSource.SMS,
        confidence = 92,
        status = FinancialEventStatus.ACTIVE,
        supersedesEventId = null,
        fingerprint = "fp-abc",
        rawText = "AED 123.45 spent at BURGER KING",
        createdAt = Instant.ofEpochMilli(1_700_000_500_000L),
    )

    @Test
    fun `domain to entity to domain round trips exactly`() {
        val original = sample()
        val restored = original.toEntity().toDomain()
        assertEquals(original, restored)
    }

    @Test
    fun `entity preserves money as minor units and currency`() {
        val entity = sample().toEntity()
        assertEquals(12_345L, entity.amountMinor)
        assertEquals(CurrencyCode.AED, entity.currencyCode)
        // Timestamps persist as epoch millis, not Instants.
        assertEquals(1_700_000_000_000L, entity.timestampEpochMillis)
    }

    @Test
    fun `correction linkage and superseded status survive mapping`() {
        val correction = sample().copy(
            id = "evt-124",
            status = FinancialEventStatus.ACTIVE,
            supersedesEventId = "evt-123",
        )
        val restored = correction.toEntity().toDomain()
        assertEquals("evt-123", restored.supersedesEventId)
        assertEquals(FinancialEventStatus.ACTIVE, restored.status)
    }

    @Test
    fun `event-first record with no originating transaction maps null`() {
        val eventFirst = sample().copy(transactionId = null)
        assertNull(eventFirst.toEntity().transactionId)
        assertNull(eventFirst.toEntity().toDomain().transactionId)
    }
}
