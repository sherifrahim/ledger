package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L7 regression: a transfer message describing a credit TO the account must be
 * INCOMING, not the OUTGOING default. Genuinely outward transfers must stay
 * OUTGOING. inferTransferDirection is only called once the caller has already
 * decided the message is a transfer, so these inputs assume that.
 */
class ExtractionHelpersDirectionTest {

    @Test
    fun `credit to account is INCOMING`() {
        assertEquals(TransferDirection.INCOMING, ExtractionHelpers.inferTransferDirection("rs 500 credited to a/c xx1234"))
        assertEquals(TransferDirection.INCOMING, ExtractionHelpers.inferTransferDirection("amount credited to your account"))
        assertEquals(TransferDirection.INCOMING, ExtractionHelpers.inferTransferDirection("inr 1000 a/c credited on 21-07"))
    }

    @Test
    fun `received transfers are INCOMING`() {
        assertEquals(TransferDirection.INCOMING, ExtractionHelpers.inferTransferDirection("aed 300 received from jane"))
    }

    @Test
    fun `outward transfers stay OUTGOING`() {
        assertEquals(TransferDirection.OUTGOING, ExtractionHelpers.inferTransferDirection("aed 200 transferred to john via upi"))
        assertEquals(TransferDirection.OUTGOING, ExtractionHelpers.inferTransferDirection("paid towards your card"))
        assertEquals(TransferDirection.OUTGOING, ExtractionHelpers.inferTransferDirection("sent to merchant xyz"))
    }
}
