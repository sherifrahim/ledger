package com.sherif.ledger.feature.capture.parsing.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The message text here is copied verbatim from the owner's device database (the
 * `raw_text` column, which only holds the real message because extraction stopped
 * overwriting it). Both UAE card issuers phrase the clause differently, and one of
 * them puts a full stop inside the abbreviation.
 */
class AvailableCreditExtractionTest {

    @Test
    fun `reads Mashreq's available limit`() {
        val sms = "Mashreq Credit Card ending 1959 was used for a transaction of AED 19.00 " +
            "at TEA TRUST CAFTERIA on Sunday, 2 August 2026, 10:54 pm. Available limit: AED 8,115.13"

        assertEquals(811_513L, ExtractionHelpers.extractAvailableCreditMinor(sms))
    }

    @Test
    fun `reads Emirates NBD's abbreviated form`() {
        val sms = "Purchase of AED 62.00 with Credit Card ending 8165 at LULU HYPERMARKET WAHDA, " +
            "ABUDHABI. Avl Cr. Limit is AED 12,344.16"

        assertEquals(1_234_416L, ExtractionHelpers.extractAvailableCreditMinor(sms))
    }

    @Test
    fun `a current account's available balance is not card headroom`() {
        // The single most important negative case. "Available balance" is a checking
        // account's balance; reading it as a credit limit would compute an
        // outstanding figure for an account that has no limit at all.
        val sms = "AED2770.00 transferred via ADCB Personal Internet Banking / Mobile App " +
            "from acc. no. XXX920001 on Jul 23 2026 3:56PM. Avl. bal. AED 1493.52."

        assertNull(ExtractionHelpers.extractAvailableCreditMinor(sms))
    }

    @Test
    fun `a message with no limit clause yields nothing`() {
        assertNull(ExtractionHelpers.extractAvailableCreditMinor("Purchase of AED 50.00 at AMAZON AE."))
    }

    @Test
    fun `the limit clause is still kept out of the transaction amount`() {
        // Guards the original bug this whole area exists for: the card's remaining
        // limit was once booked as an AED 8,225.16 purchase at KFC. Capturing the
        // clause must not re-open that door.
        val sms = "Mashreq Credit Card ending 1959 was used for a transaction of AED 19.00 " +
            "at TEA TRUST CAFTERIA. Available limit: AED 8,115.13"

        assertEquals(1_900L, ExtractionHelpers.extractAmountMinor(sms))
        assertEquals(811_513L, ExtractionHelpers.extractAvailableCreditMinor(sms))
    }
}
