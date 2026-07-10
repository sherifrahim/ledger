package com.sherif.ledger.feature.semantic

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionType
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

class SemanticClassifierTest {

    private val classifier = DeterministicSemanticClassifier()

    private fun envelope(text: String) = NotificationEnvelope(
        packageName = "com.bank.app",
        title = "",
        text = text,
        subText = null,
        timestamp = Instant.now(),
        notificationKey = "k",
    )

    private fun candidateFor(text: String) = TransactionCandidate(
        source = IngestionSource.NOTIFICATION,
        rawText = text,
        merchantName = "Merchant",
        amountMinor = 15000,
        currencyCode = CurrencyCode.AED,
        timestamp = Instant.now(),
        accountHint = "6989",
        transactionType = TransactionType.EXPENSE,
    )

    // ---- The exact production bug ----
    @Test fun `FAB payment processed is a confirmation not an event`() {
        val text = "Dear Customer, Your Payment of AED 150.00 for card 5492XXXXXXXX6989 has been processed on 09/07/2026"
        val result = classifier.classify(envelope(text), candidateFor(text))
        assertEquals(SemanticClass.FINANCIAL_CONFIRMATION, result.semanticClass)
        assertTrue("Confidence should be high", result.confidence >= 85)
    }

    @Test fun `ADCB debit remains a financial event`() {
        val text = "AED 200 debited from ADCB account XXX920001. Avl. bal. AED 7955.36."
        val result = classifier.classify(envelope(text), candidateFor(text))
        assertEquals(SemanticClass.FINANCIAL_EVENT, result.semanticClass)
    }

    // ---- A full duplicate-confirmation chain never yields two events ----
    @Test fun `duplicate confirmation chain produces one event and confirmations`() {
        val debit = "AED 150 debited from ADCB account XXX920001"
        val fabConfirm = "Your Payment of AED 150.00 for card 5492XXXXXXXX6989 has been processed on 09/07/2026"
        val secondConfirm = "Payment received. Outstanding balance updated. Thank you for your payment."

        val events = listOf(debit, fabConfirm, secondConfirm).map {
            classifier.classify(envelope(it), candidateFor(it)).semanticClass
        }
        assertEquals(
            listOf(
                SemanticClass.FINANCIAL_EVENT,
                SemanticClass.FINANCIAL_CONFIRMATION,
                SemanticClass.FINANCIAL_CONFIRMATION,
            ),
            events,
        )
        // Exactly one event in the chain — no double-count.
        assertEquals(1, events.count { it == SemanticClass.FINANCIAL_EVENT })
    }

    // ---- Corpus regression ----
    @Test fun `semantic corpus classifies every fixture correctly`() {
        val file = listOf(
            File("src/test/resources/semantic-corpus/classifications.json"),
            File("app/src/test/resources/semantic-corpus/classifications.json"),
        ).firstOrNull { it.isFile } ?: error("semantic-corpus not found")

        val fixtures = Json { ignoreUnknownKeys = true }
            .parseToJsonElement(file.readText()).jsonArray

        val failures = mutableListOf<String>()
        fixtures.forEach { el ->
            val o = el.jsonObject
            val text = o["text"]!!.jsonPrimitive.content
            val expected = SemanticClass.valueOf(o["expected"]!!.jsonPrimitive.content)
            // Candidate provided only when the message would extract; for INFORMATION
            // and CONFIRMATION we still pass a candidate to prove the classifier
            // overrides extraction rather than depending on its absence.
            val got = classifier.classify(envelope(text), candidateFor(text)).semanticClass
            if (got != expected) failures += "[$expected != $got] ${text.take(60)}"
        }
        assertTrue("Semantic misclassifications:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test fun `corpus is non-trivial and covers all classes`() {
        val file = File("app/src/test/resources/semantic-corpus/classifications.json")
            .takeIf { it.isFile } ?: File("src/test/resources/semantic-corpus/classifications.json")
        val fixtures = Json { ignoreUnknownKeys = true }.parseToJsonElement(file.readText()).jsonArray
        assertTrue("Corpus too small", fixtures.size >= 15)
    }
}

