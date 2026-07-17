package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Phase 4B: intent-aware extraction. Transactions must persist (Extracted);
 * promotions, OTP, statements must Ignore. Tests the HeuristicExtractor directly
 * so the financial-intent decision is isolated from the known-bank path.
 */
class HeuristicIntentTest {

    private val extractor = HeuristicExtractor(
        TextNormalizer(),
        MerchantNormalizer(),
        FinancialPhraseLibrary(),
    )

    private fun run(text: String): ExtractionResult = runBlocking {
        extractor.extract(
            NotificationEnvelope(
                packageName = "com.google.android.apps.messaging",
                title = "",
                text = text,
                subText = null,
                timestamp = Instant.now(),
                notificationKey = "t",
            ),
        )
    }

    private fun assertExtracted(text: String): ExtractionResult.Extracted {
        val r = run(text)
        assertTrue("Expected Extracted, got $r", r is ExtractionResult.Extracted)
        return r as ExtractionResult.Extracted
    }

    private fun assertIgnored(text: String, category: String): ExtractionResult.Ignore {
        val r = run(text)
        assertTrue("Expected Ignore, got $r", r is ExtractionResult.Ignore)
        val ig = r as ExtractionResult.Ignore
        assertEquals(category, ig.category)
        return ig
    }

    // ---- MUST PERSIST ----

    @Test fun `transaction persists`() {
        val e = assertExtracted("AED 52.40 spent at Carrefour using card ending 4582.")
        assertEquals(5240L, e.candidate.amountMinor)
        assertEquals("4582", e.candidate.accountHint)
    }

    @Test fun `salary persists`() {
        val e = assertExtracted("Salary of AED 6,000 credited to your account.")
        assertEquals(600000L, e.candidate.amountMinor)
    }

    @Test fun `refund persists`() {
        val e = assertExtracted("Refund of AED 25.00 credited to your card ending 1234.")
        assertEquals(2500L, e.candidate.amountMinor)
    }

    @Test fun `atm persists`() {
        val e = assertExtracted("AED3000.00 withdrawn from acc. XXX920001 at ATM.")
        assertEquals(300000L, e.candidate.amountMinor)
    }

    @Test fun `amount extraction ignores an account digit run that precedes the currency amount`() {
        val e = assertExtracted("Your card ending in 1234 was used for a purchase of AED 1,250.50 at Amazon.ae")
        assertEquals(125050L, e.candidate.amountMinor)
        assertEquals("1234", e.candidate.accountHint)
    }

    // ---- MUST IGNORE ----

    @Test fun `emi offer ignored`() {
        assertIgnored("Convert your purchase into easy EMI.", "EMI Offer")
    }

    @Test fun `loan offer ignored`() {
        val ig = assertIgnored("You're eligible for a personal loan of AED 50,000. Apply now.", "Loan Offer")
        assertTrue(ig.matchedPhrases.isNotEmpty())
    }

    @Test fun `cashback ignored`() {
        assertIgnored("Get 10% cashback on your next purchase.", "Offer")
    }

    @Test fun `credit limit offer ignored`() {
        assertIgnored("Increase your credit limit instantly. You are pre-approved.", "Credit Limit Offer")
    }

    @Test fun `marketing ignored`() {
        assertIgnored("Exclusive offer! Upgrade your card and win rewards. Limited time.", "Offer")
    }

    @Test fun `promotion with amount still ignored`() {
        // Has a real currency amount but promotional intent must win.
        assertIgnored("You are eligible for AED 100000 loan. Apply now for instant approval.", "Loan Offer")
    }

    @Test fun `otp ignored`() {
        assertIgnored("Your OTP for login is 445566. Do not share.", "OTP")
    }

    @Test fun `statement ignored`() {
        assertIgnored("Your monthly statement is ready. Total due AED 500.", "Statement")
    }

    @Test fun `ignore carries diagnostics phrases`() {
        val ig = assertIgnored("Get 10% cashback now, limited time offer.", "Offer")
        assertTrue("cashback" in ig.matchedPhrases || ig.matchedPhrases.any { it.contains("cashback") })
    }
}
