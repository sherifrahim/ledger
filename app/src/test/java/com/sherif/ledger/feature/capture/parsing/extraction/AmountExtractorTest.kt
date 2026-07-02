package com.sherif.ledger.feature.capture.parsing.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountExtractorTest {

    private val extractor = AmountExtractor()

    @Test
    fun `extracts decimal amount`() {
        assertEquals(125050L, extractor.extract("Spent AED 1,250.50 at Amazon"))
    }

    @Test
    fun `extracts integer amount`() {
        assertEquals(100000L, extractor.extract("Paid INR 1000 to merchant"))
    }

    @Test
    fun `extracts amount with commas`() {
        assertEquals(10000000L, extractor.extract("AED 100,000 received"))
    }

    @Test
    fun `returns null if no number found`() {
        assertNull(extractor.extract("Welcome to your bank app"))
    }
}
