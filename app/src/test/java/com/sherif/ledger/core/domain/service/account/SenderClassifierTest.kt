package com.sherif.ledger.core.domain.service.account

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Every identifier here is one that actually appeared in the owner's captured data
 * (see the `origin_package_name` column of the device database) — these are the
 * real strings the classifier has to get right, not invented examples.
 */
class SenderClassifierTest {

    private val classifier = SenderClassifier()

    @Test
    fun `messaging apps are transport`() {
        listOf(
            "com.google.android.apps.messaging",
            "com.truecaller",
            "com.samsung.android.messaging",
            "com.whatsapp",
        ).forEach {
            assertEquals(it, SenderKind.TRANSPORT, classifier.classify(it))
        }
    }

    @Test
    fun `telecom and loyalty senders are non-financial, however the carrier decorated the header`() {
        // "AD-eand" is the same operator as "eand", routed through an aggregator;
        // "eandINF" and "eandUAE" are its own informational and marketing senders.
        listOf("Smiles", "eandINF", "eandUAE", "AD-eand", "eand").forEach {
            assertEquals(it, SenderKind.NON_FINANCIAL, classifier.classify(it))
        }
    }

    @Test
    fun `banks Ledger has not met yet stay unknown, not junk`() {
        // These are real financial senders. Refusing them an account would be
        // over-reach: they should keep a reviewable candidate account and be
        // promotable, which is what UNKNOWN preserves.
        listOf("MBANKAlert", "Tabby", "AX-iPaytm-S", "JK-SBIUPI-S", "ADCBAlert", "Mashreq", "eandmoney").forEach {
            assertEquals(it, SenderKind.UNKNOWN, classifier.classify(it))
        }
    }

    @Test
    fun `matching is on the whole identifier, never a substring`() {
        // "DU" is a UAE operator, so it is on the non-financial list — but it must
        // never be reachable from an identifier that merely contains those letters.
        assertEquals(SenderKind.NON_FINANCIAL, classifier.classify("du"))
        assertEquals(SenderKind.UNKNOWN, classifier.classify("DUBAI-ISLAMIC"))
        assertEquals(SenderKind.UNKNOWN, classifier.classify("HDUBK"))
        // Likewise a package that merely contains a messenger's name is not it.
        assertEquals(SenderKind.UNKNOWN, classifier.classify("com.truecaller.clone.bank"))
    }

    @Test
    fun `a missing sender is unknown rather than refused`() {
        assertEquals(SenderKind.UNKNOWN, classifier.classify(null))
        assertEquals(SenderKind.UNKNOWN, classifier.classify("   "))
    }

    @Test
    fun `canOwnAnAccount agrees with classify`() {
        assertEquals(false, classifier.canOwnAnAccount("com.truecaller"))
        assertEquals(false, classifier.canOwnAnAccount("Smiles"))
        assertEquals(true, classifier.canOwnAnAccount("ADCBAlert"))
    }
}
