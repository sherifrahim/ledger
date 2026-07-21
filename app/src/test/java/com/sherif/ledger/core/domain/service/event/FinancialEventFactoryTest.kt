package com.sherif.ledger.core.domain.service.event

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.FinancialEventStatus
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FinancialEventFactoryTest {

    private fun txn() = Transaction(
        id = 55L,
        accountId = 4L,
        brandId = 8L,
        categoryId = 2L,
        amount = Money(9_900L, CurrencyCode.AED),
        type = TransactionType.EXPENSE,
        timestamp = Instant.ofEpochMilli(1_700_000_000_000L),
        source = IngestionSource.SMS,
        rawText = "Purchase of AED 99.00 at COSTA COFFEE",
        fingerprint = "fp-costa",
    )

    @Test
    fun `mirror carries the transaction's financial facts verbatim (parity)`() {
        val e = FinancialEventFactory.mirrorOf(txn())
        assertEquals(55L, e.transactionId)
        assertEquals(4L, e.accountId)
        assertEquals(8L, e.brandId)
        assertEquals(2L, e.categoryId)
        assertEquals(Money(9_900L, CurrencyCode.AED), e.amount)
        assertEquals(TransactionType.EXPENSE, e.type)
        assertEquals(Instant.ofEpochMilli(1_700_000_000_000L), e.timestamp)
        assertEquals(IngestionSource.SMS, e.source)
        assertEquals("fp-costa", e.fingerprint)
    }

    @Test
    fun `a mirror of an accepted transaction is active and deterministic`() {
        val e = FinancialEventFactory.mirrorOf(txn())
        assertEquals(FinancialEventStatus.ACTIVE, e.status)
        assertEquals(FinancialEventFactory.DETERMINISTIC_CONFIDENCE, e.confidence)
        assertNull(e.supersedesEventId)
    }

    @Test
    fun `distinct ids are generated per call so corrections never collide`() {
        val a = FinancialEventFactory.mirrorOf(txn())
        val b = FinancialEventFactory.mirrorOf(txn())
        // Same transaction, but each event is a distinct record (unique id).
        assert(a.id != b.id)
        // Fingerprint is shared, which is what makes recording idempotent downstream.
        assertEquals(a.fingerprint, b.fingerprint)
    }
}
