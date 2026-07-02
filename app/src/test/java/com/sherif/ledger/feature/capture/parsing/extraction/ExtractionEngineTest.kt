package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant

class ExtractionEngineTest {

    private val extractor = TransactionExtractor(
        TextNormalizer(),
        AmountExtractor(),
        CurrencyExtractor(),
        MerchantExtractor(),
        TransactionTypeResolver()
    )

    @Test
    fun `extracts full transaction facts from typical notification`() {
        val envelope = NotificationEnvelope(
            packageName = "com.adcb.mobileapp",
            title = "Transaction Alert",
            text = "Your card ending in 1234 was used for a purchase of AED 1,250.50 at Amazon.ae",
            subText = null,
            timestamp = Instant.now(),
            notificationKey = "key"
        )

        val candidate = extractor.extract(envelope)

        assertEquals(125050L, candidate.amountMinor)
        assertEquals(CurrencyCode.AED, candidate.currencyCode)
        assertEquals("Amazon.ae", candidate.merchantName)
        assertEquals("1234", candidate.accountHint)
        assertEquals(TransactionType.EXPENSE, candidate.transactionType)
    }

    @Test
    fun `handles income notification`() {
        val envelope = NotificationEnvelope(
            packageName = "com.bank.app",
            title = "Credit Alert",
            text = "Salary of INR 50,000 credited to account XX5678",
            subText = null,
            timestamp = Instant.now(),
            notificationKey = "key"
        )

        val candidate = extractor.extract(envelope)

        assertEquals(5000000L, candidate.amountMinor)
        assertEquals(CurrencyCode.INR, candidate.currencyCode)
        assertEquals(TransactionType.INCOME, candidate.transactionType)
        assertEquals("5678", candidate.accountHint)
    }
}
