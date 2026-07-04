package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.feature.capture.parsing.extraction.ExtractionHelpers
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtractionHelpersAuditTest {

    @Test
    fun `extracts amount from salary notification`() {
        val text = "Your salary AED6000.00 has been credited to your account no. XXX920001 on Jul 3 2026 2:12PM. The available balance is AED9079.30."
        val amount = ExtractionHelpers.extractAmountMinor(text)
        System.err.println("DEBUG: Extracted amount: $amount")
        assertEquals(600000L, amount)
    }
}
