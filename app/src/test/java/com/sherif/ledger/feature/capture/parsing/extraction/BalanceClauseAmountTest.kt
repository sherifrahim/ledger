package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.CurrencyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests built from REAL notification text captured off a physical
 * device (2026-08-03). These reproduce a live trust failure: four "purchases" of
 * ~AED 8,2xx were recorded (a KFC charge of AED 8,225.16), which were actually the
 * card's *Available limit*. Root cause: the transaction was in a foreign currency
 * the anchored-amount pattern didn't recognise, so extraction skipped it and took
 * the first AED figure in the message — the running balance.
 */
class BalanceClauseAmountTest {

    @Test
    fun `mashreq purchase takes the transacted amount not the available limit`() {
        val text = "Mashreq Credit Card ending 1959 was used for a transaction of AED 21.50 " +
            "at CARS TAXI on Monday, 3 August 2026, 10:06 am. Available limit: AED 8,093.63"
        assertEquals(2150L, ExtractionHelpers.extractAmountMinor(text))
        assertEquals(CurrencyCode.AED, ExtractionHelpers.extractCurrency(text))
    }

    @Test
    fun `adcb credit takes the transacted amount not the available balance`() {
        val text = "A Cr. transaction of AED 500.00 on your account no. XXX920001 was " +
            "successful.Available balance is AED1568.52."
        assertEquals(50000L, ExtractionHelpers.extractAmountMinor(text))
    }

    @Test
    fun `adcb transfer takes the transferred amount not the closing balance`() {
        val text = "AED2770.00 transferred via ADCB Personal Internet Banking / Mobile App " +
            "from acc. no. XXX920001 on Jul 23 2026 3:56PM. Avl. bal. AED 1493.52."
        assertEquals(277000L, ExtractionHelpers.extractAmountMinor(text))
    }

    @Test
    fun `foreign-currency purchase is not recorded as the AED available limit`() {
        // The exact failure class that produced the phantom "AED 8,225.16 at KFC".
        val text = "Mashreq Credit Card ending 1959 was used for a transaction of KZT 7500.00 " +
            "at KFC on Friday, 1 August 2026. Available limit: AED 8,225.16"
        // The transacted amount, in its own currency — never the AED limit. The
        // balance guard keeps a KZT charge from moving an AED account's balance.
        assertEquals(750000L, ExtractionHelpers.extractAmountMinor(text))
        assertEquals(CurrencyCode.KZT, ExtractionHelpers.extractCurrency(text))
    }

    @Test
    fun `card tail is never read as the amount`() {
        // Without a recognised currency anchor the un-anchored fallback used to take
        // the first number in the text — the card tail — inventing a charge.
        val text = "Card ending 1959 used for a transaction at SHOP. Available limit: AED 8,225.16"
        val amount = ExtractionHelpers.extractAmountMinor(text)
        assertNull("card tail 1959 must not become an amount", amount?.takeIf { it == 195900L })
    }

    @Test
    fun `usd purchase keeps its own currency instead of the quoted AED balance`() {
        val text = "Your debit card XXX5986 linked to acc. XXX920001 was used for USD21.00 " +
            "on Jul 3 2026 3:25PM at ANTHROPIC CLAUD,US. Avl.Bal AED 8999.38."
        assertEquals(2100L, ExtractionHelpers.extractAmountMinor(text))
        assertEquals(CurrencyCode.USD, ExtractionHelpers.extractCurrency(text))
    }

    @Test
    fun `plain purchase without any balance clause still parses`() {
        val text = "Purchase of AED 50.00 at AMAZON AE with card ending 1234."
        assertEquals(5000L, ExtractionHelpers.extractAmountMinor(text))
        assertEquals(CurrencyCode.AED, ExtractionHelpers.extractCurrency(text))
    }
}
