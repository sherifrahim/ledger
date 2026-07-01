package com.sherif.ledger.core.database.mapper

import com.sherif.ledger.core.database.entity.TransactionEntity
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class MapperTest {

    @Test
    fun `TransactionEntity toDomain maps correctly`() {
        val entity = TransactionEntity(
            id = 1,
            accountId = 2,
            brandId = 3,
            categoryId = 4,
            amountMinor = 1500,
            currencyCode = CurrencyCode.AED,
            type = TransactionType.EXPENSE,
            timestampEpochMillis = 1719878400000L, // 2024-07-02
            source = IngestionSource.SMS,
            rawText = "AMZN",
            fingerprint = "hash"
        )
        
        val domain = entity.toDomain()
        
        assertEquals(1L, domain.id)
        assertEquals(1500L, domain.amount.minorUnits)
        assertEquals(CurrencyCode.AED, domain.amount.currencyCode)
        assertEquals(1719878400000L, domain.timestamp.toEpochMilli())
    }
}
