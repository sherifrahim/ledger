package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Merchant names, against the owner's real captured messages.
 *
 * Every string here is copied from the `raw_text` column of the device database —
 * which only holds the real message because extraction stopped overwriting it.
 * Each case is a name that was actually wrong on screen.
 */
class MerchantExtractionTest {

    private val extractor = HeuristicExtractor(
        normalizer = TextNormalizer(),
        merchantNormalizer = MerchantNormalizer(),
        phrases = com.sherif.ledger.feature.capture.extraction.FinancialPhraseLibrary(),
    )

    private fun merchantOf(body: String): String? = runBlocking {
        val result = extractor.extract(
            NotificationEnvelope(
                packageName = "Mashreq",
                title = "",
                text = body,
                subText = null,
                timestamp = Instant.now(),
                notificationKey = "k",
                source = IngestionSource.SMS,
            ),
        )
        (result as? ExtractionResult.Extracted)?.candidate?.merchantName
    }

    @Test
    fun `a merchant whose own name contains "to" is not cut at it`() {
        // Rendered as "Day". The stop-word list treated the "to" inside DAY TO DAY
        // as the end of the name, so the row read as a single meaningless word.
        val merchant = merchantOf(
            "Purchase of AED 58.97 was made with Card ending XX852 at DAY TO DAY HYPMKT BR O. " +
                "Available Balance: AED 85.21",
        )

        assertEquals("Day To Day Hypmkt Br O", merchant)
    }

    @Test
    fun `a merchant is not run on into the sentence that follows it`() {
        // Rendered as "Ounass Uae Is Confirmed" — the capture ran past the name into
        // the verb, because "is" was not a boundary.
        val merchant = merchantOf(
            "Your AED 160.00 purchase at Ounass UAE is confirmed. To pay as low as 12.76/month, " +
                "extend your payment plan in the app https://s.tabby.ai/JTyNOJ4",
        )

        assertEquals("Ounass Uae", merchant)
    }

    @Test
    fun `a merchant with a bracket in it is still found`() {
        // Rendered as "Unknown" — the name is plainly in the message, but a "(" was
        // not an allowed character, so the pattern failed and the row lost its
        // merchant entirely. Normalises to the known brand.
        val merchant = merchantOf(
            "Purchase of AED 78.75 was made with Card ending XX852 at ETISALAT HEAD OFFICE (PAY. " +
                "Available Balance: AED 912.79",
        )

        assertEquals("Etisalat", merchant)
    }

    @Test
    fun `an ordinary merchant is unaffected`() {
        // Guards the fix: loosening the boundary must not start swallowing the rest
        // of a normal message.
        assertEquals(
            "Tea Trust Cafteria",
            merchantOf(
                "Mashreq Credit Card ending 1959 was used for a transaction of AED 19.00 " +
                    "at TEA TRUST CAFTERIA on Sunday, 2 August 2026, 10:54 pm. Available limit: AED 8,115.13",
            ),
        )
    }

    @Test
    fun `a known brand still normalises`() {
        assertEquals(
            "Lulu Hypermarket",
            merchantOf("Purchase of AED 62.00 with Credit Card ending 8165 at LULU HYPERMARKET WAHDA, ABUDHABI."),
        )
    }
}
