package com.sherif.ledger.feature.semantic

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.extraction.ExtractionDiagnostics
import com.sherif.ledger.feature.capture.extraction.ExtractionRegistry.ExtractionOutcome
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

class FinancialIntentClassifierTest {

    private val classifier = DeterministicFinancialIntentClassifier()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Reads the corpus as a STREAM from the classpath, never converting a
         * classpath URL into a java.io.File. Converting via `url.file` and wrapping
         * it in File(String) is a known Windows pitfall: on Windows, a classpath
         * file:// URL commonly stringifies with a leading slash before the drive
         * letter (e.g. "/C:/ledger/app/build/resources/test/..."), which
         * File(String) does not reliably resolve, so the lookup silently fails even
         * though the resource genuinely exists. Reading as a stream sidesteps this
         * entirely and works identically across OS and whether resources are on
         * disk or packaged in a jar. Relative File paths are kept only as a last
         * resort for environments where the working directory happens to match.
         */
        private fun corpusText(): String {
            val stream = FinancialIntentClassifierTest::class.java.classLoader
                ?.getResourceAsStream("semantic-corpus/classifications.json")
            if (stream != null) {
                return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            val candidates = listOf(
                File("src/test/resources/semantic-corpus/classifications.json"),
                File("app/src/test/resources/semantic-corpus/classifications.json"),
            )
            val file = candidates.firstOrNull { it.isFile }
                ?: error(
                    "semantic-corpus/classifications.json not found via classpath stream " +
                        "or relative paths; looked in $candidates",
                )
            return file.readText()
        }
    }

    private fun envelope(text: String) = NotificationEnvelope(
        packageName = "com.bank.app",
        title = "",
        text = text,
        subText = null,
        timestamp = Instant.now(),
        notificationKey = "k",
    )

    /** A neutral outcome carrying no evidence, so a classification can only come
     *  from the classifier's own text analysis (tiers 1-3). Used for pure phrase
     *  logic tests; the extraction-outcome fallback tiers (4-5) are tested
     *  separately below with deliberately crafted outcomes. */
    private fun neutralOutcome() = ExtractionOutcome.Failed("neutral - test placeholder", emptyList())

    private fun candidate(text: String) = TransactionCandidate(
        source = IngestionSource.NOTIFICATION,
        rawText = text,
        merchantName = "Merchant",
        amountMinor = 15000,
        currencyCode = CurrencyCode.AED,
        timestamp = Instant.now(),
        accountHint = "6989",
        transactionType = TransactionType.EXPENSE,
    )

    // ---- The exact production bug: our own text analysis wins even when
    //      extraction succeeded (proves the classifier is NOT gated by success) ----
    @Test fun `FAB payment processed is a confirmation regardless of extraction success`() {
        val text = "Dear Customer, Your Payment of AED 150.00 for card 5492XXXXXXXX6989 has been processed on 09/07/2026"
        val successOutcome = ExtractionOutcome.Success(candidate(text), emptyList())
        val result = classifier.classify(envelope(text), successOutcome)
        assertEquals(FinancialIntent.FINANCIAL_CONFIRMATION, result.intent)
        assertTrue(result.confidence >= 85)
    }

    @Test fun `ADCB debit remains a financial event`() {
        val text = "AED 200 debited from ADCB account XXX920001. Avl. bal. AED 7955.36."
        val result = classifier.classify(envelope(text), ExtractionOutcome.Success(candidate(text), emptyList()))
        assertEquals(FinancialIntent.FINANCIAL_EVENT, result.intent)
    }

    // ---- Even Ignored/Failed extraction outcomes must be correctly classified,
    //      never gate the decision ----
    @Test fun `a confirmation with no anchored amount is still classified from text alone`() {
        val text = "Thank you for your payment. Your account is now up to date."
        val result = classifier.classify(envelope(text), ExtractionOutcome.Failed("No anchored amount", emptyList()))
        assertEquals(FinancialIntent.FINANCIAL_CONFIRMATION, result.intent)
    }

    @Test fun `an ignored promotion with no financial content is unknown, not fabricated`() {
        val text = "Get 20% cashback on your next purchase! Limited time offer."
        val result = classifier.classify(envelope(text), ExtractionOutcome.Ignored("Promotion detected", emptyList()))
        assertEquals(FinancialIntent.UNKNOWN, result.intent)
    }

    // ---- Tier 4: extraction's own Confirmation outcome as fallback evidence,
    //      used only when our own text analysis is silent ----
    @Test fun `extraction registry confirmation is used as fallback when text analysis is silent`() {
        val text = "Your recent remittance has been reconciled with your account."
        val outcome = ExtractionOutcome.Confirmation(15000, "6989", listOf("reconciled"), emptyList())
        val result = classifier.classify(envelope(text), outcome)
        assertEquals(FinancialIntent.FINANCIAL_CONFIRMATION, result.intent)
    }

    // ---- Tier 5: extraction Success as a cautious fallback when no phrase
    //      signal fired at all ----
    @Test fun `extraction success is a cautious fallback event when text is silent`() {
        val text = "AED 45.00 STARBUCKS COFFEE DXB"
        val result = classifier.classify(envelope(text), ExtractionOutcome.Success(candidate(text), emptyList()))
        assertEquals(FinancialIntent.FINANCIAL_EVENT, result.intent)
        assertTrue("Fallback confidence should be lower than a direct phrase match", result.confidence < 90)
    }

    @Test fun `true unknown when neither text nor extraction offer any signal`() {
        val text = "Please verify your identity to continue using our services."
        val result = classifier.classify(envelope(text), ExtractionOutcome.Failed("No financial vocabulary", emptyList()))
        assertEquals(FinancialIntent.UNKNOWN, result.intent)
    }

    // ---- Intent is independent of the extractor: real credits without an
    //      obvious debit verb must still be events ----
    @Test fun `credit transaction is an event even without a debit verb`() {
        val text = "A Cr. transaction of AED 1200.00 on your account no. XXX920001 was successful."
        val result = classifier.classify(envelope(text), neutralOutcome())
        assertEquals(FinancialIntent.FINANCIAL_EVENT, result.intent)
    }

    @Test fun `salary credit is an event`() {
        val text = "INR 45000 has been credited to your account as salary"
        val result = classifier.classify(envelope(text), neutralOutcome())
        assertEquals(FinancialIntent.FINANCIAL_EVENT, result.intent)
    }

    // ---- Collision: "credited to your card" is a confirmation, not an income event ----
    @Test fun `payment credited to card is a confirmation`() {
        val text = "AED 200 payment credited to your card ending 6989. Thank you."
        val result = classifier.classify(envelope(text), neutralOutcome())
        assertEquals(FinancialIntent.FINANCIAL_CONFIRMATION, result.intent)
    }

    // ---- Bug report 2026-08-05: Tabby (BNPL) sends its OWN "payment received"
    //      confirmation after a purchase is already correctly debited from the
    //      user's real bank account. Real-world wording puts "received" BEFORE
    //      "payment" ("we've received your payment"), which none of the
    //      confirmationSignals phrases matched (they all require "payment"
    //      first, or the un-contracted "we have received"), so "payment of" -- a
    //      movementVerb -- fell through to branch 3 and asserted a brand-new
    //      FINANCIAL_EVENT, turning Tabby's receipt acknowledgement into a
    //      phantom credit transaction. ----
    @Test fun `BNPL payment-received confirmation with received-before-payment wording is not a new event`() {
        val text = "We've received your payment of AED 375.00 for your Tabby plan. Thank you!"
        val result = classifier.classify(envelope(text), neutralOutcome())
        assertEquals(FinancialIntent.FINANCIAL_CONFIRMATION, result.intent)
    }

    @Test fun `payment of alone with no other movement or confirmation signal is not asserted as an event`() {
        val text = "Payment of AED 100.00 is being reviewed."
        val result = classifier.classify(envelope(text), neutralOutcome())
        assertTrue(
            "Bare 'payment of' with no corroborating signal must not reach FINANCIAL_EVENT at 90% confidence",
            result.intent != FinancialIntent.FINANCIAL_EVENT || result.confidence < 90,
        )
    }

    // ---- Duplicate confirmation chain: exactly one event ----
    @Test fun `duplicate confirmation chain yields one event`() {
        val chain = listOf(
            "AED 150 debited from ADCB account XXX920001",
            "Your Payment of AED 150.00 for card 5492XXXXXXXX6989 has been processed on 09/07/2026",
            "Payment received. Outstanding balance updated. Thank you for your payment.",
        ).map { classifier.classify(envelope(it), neutralOutcome()).intent }

        assertEquals(1, chain.count { it == FinancialIntent.FINANCIAL_EVENT })
        assertEquals(2, chain.count { it == FinancialIntent.FINANCIAL_CONFIRMATION })
    }

    // ---- Corpus regression (text-tier logic; neutral extraction outcome) ----
    @Test fun `intent corpus classifies every fixture correctly`() {
        val fixtures = json.parseToJsonElement(corpusText()).jsonArray

        val failures = mutableListOf<String>()
        fixtures.forEach { el ->
            val o = el.jsonObject
            val text = o["text"]!!.jsonPrimitive.content
            val expected = FinancialIntent.valueOf(o["expected"]!!.jsonPrimitive.content)
            val got = classifier.classify(envelope(text), neutralOutcome()).intent
            if (got != expected) failures += "[$expected != $got] ${text.take(60)}"
        }
        assertTrue("Intent misclassifications:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test fun `corpus covers all classes and is non-trivial`() {
        val fixtures = json.parseToJsonElement(corpusText()).jsonArray
        assertTrue(fixtures.size >= 15)
    }
}

