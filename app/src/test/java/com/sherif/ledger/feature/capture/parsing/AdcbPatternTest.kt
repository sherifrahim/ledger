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

class AdcbPatternTest {

    private val normalizer = MerchantNormalizer()
    private val parser = AdcbParser(PatternEngine(TextNormalizer()), normalizer)

    @Test
    fun `parses purchase notification`() {
        val envelope = createEnvelope("Purchase of AED 50.00 at COSTA COFFEE with card ending 1234.")
        val result = parser.parse(envelope)
        
        assertTrue(result is ParseResult.Success)
        val candidate = (result as ParseResult.Success).candidate
        assertEquals(5000L, candidate.amountMinor)
        assertEquals(CurrencyCode.AED, candidate.currencyCode)
        assertEquals("Costa Coffee", candidate.merchantName)
        assertEquals(TransactionType.EXPENSE, candidate.transactionType)
    }

    @Test
    fun `parses credit notification`() {
        val envelope = createEnvelope("You received AED 1,000.00 from MASHREQ BANK.")
        val result = parser.parse(envelope)
        
        assertTrue(result is ParseResult.Success)
        val candidate = (result as ParseResult.Success).candidate
        assertEquals(100000L, candidate.amountMinor)
        assertEquals(TransactionType.INCOME, candidate.transactionType)
        assertEquals("Mashreq", candidate.merchantName)
    }

    @Test
    fun `ignores OTP notification`() {
        val envelope = createEnvelope("Your OTP is 123456. Do not share this code.")
        val result = parser.parse(envelope)
        assertTrue(result is ParseResult.Ignore)
    }

    @Test
    fun `ignores billing notification`() {
        val envelope = createEnvelope("Your Etisalat bill is due. Amount: AED 250.")
        val result = parser.parse(envelope)
        assertTrue(result is ParseResult.Ignore)
    }

    private fun createEnvelope(text: String) = NotificationEnvelope(
        packageName = "com.adcb.mobileapp",
        title = "ADCB Alert",
        text = text,
        subText = null,
        timestamp = Instant.now(),
        notificationKey = "key"
    )
}
