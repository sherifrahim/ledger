package com.sherif.ledger.feature.capture.parsing.extraction

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {

    private val normalizer = MerchantNormalizer()

    @Test
    fun `normalizes Amazon spelling variations`() {
        assertEquals("Amazon", normalizer.normalize("Amazon Grocery Dubai ARE"))
        assertEquals("Amazon", normalizer.normalize("AMAZON AE"))
    }

    @Test
    fun `normalizes Botim`() {
        assertEquals("Botim", normalizer.normalize("BOTIM MONEY"))
    }

    @Test
    fun `normalizes Noon`() {
        assertEquals("Noon", normalizer.normalize("NOON FOOD"))
    }

    @Test
    fun `fallback to title case for unknown merchant`() {
        assertEquals("Pathayapura Restaurant", normalizer.normalize("PATHAYAPURA RESTAURANT"))
    }
}
