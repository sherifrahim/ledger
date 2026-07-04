package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.PatternEngine
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AdcbSalaryAuditTest {

    private val parser = AdcbParser(
        PatternEngine(TextNormalizer()),
        MerchantNormalizer()
    )

    @Test
    fun `reproduces salary credit issue`() {
        val text = "Your salary AED6000.00 has been credited to your account no. XXX920001 on Jul 3 2026 2:12PM. The available balance is AED9079.30."
        val title = "Credit Alert"
        
        val envelope = NotificationEnvelope(
            packageName = "com.adcb.mobileapp",
            title = title,
            text = text,
            subText = null,
            timestamp = Instant.now(),
            notificationKey = "test_key"
        )

        val result = parser.parse(envelope)
        assertTrue("Parser failed: $result", result is ParseResult.Success)
        
        val candidate = (result as ParseResult.Success).candidate
        assertEquals(600000L, candidate.amountMinor)
        assertEquals(CurrencyCode.AED, candidate.currencyCode)
        assertEquals("Salary", candidate.merchantName)
        assertEquals(TransactionType.INCOME, candidate.transactionType)
    }
}
