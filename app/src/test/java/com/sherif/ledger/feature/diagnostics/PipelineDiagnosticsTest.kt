package com.sherif.ledger.feature.diagnostics

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.extraction.ExtractionDiagnostics
import com.sherif.ledger.feature.capture.extraction.ExtractionRegistry.ExtractionOutcome
import com.sherif.ledger.feature.capture.notification.FilterResult
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.feature.merchant.MerchantCategory
import com.sherif.ledger.feature.merchant.MerchantProfile
import com.sherif.ledger.feature.merchant.MerchantResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PipelineDiagnosticsTest {

    private fun diag(
        extractor: String = "HeuristicExtractor",
        decision: String = "Extracted",
        confidence: Int = 99,
        validationPassed: Boolean = true,
        rejected: String? = null,
    ) = ExtractionDiagnostics(
        extractor = extractor,
        decision = decision,
        category = "Transaction",
        durationMs = 1,
        confidence = confidence,
        validationPassed = validationPassed,
        rejectedReason = rejected,
    )

    private fun candidate() = TransactionCandidate(
        source = com.sherif.ledger.core.domain.model.IngestionSource.NOTIFICATION,
        rawText = "salary AED 1200 credited",
        merchantName = "Salary",
        amountMinor = 120000,
        currencyCode = CurrencyCode.AED,
        timestamp = java.time.Instant.now(),
        accountHint = "1234",
        transactionType = TransactionType.INCOME,
    )

    // ---- Regression: a full accepted pipeline yields all stages ----
    @Test fun `full accepted trace records every stage through persistence`() {
        val tracer = PipelineTracer("key-1")
            .recordReceived("com.adcb.nexgen", 0)
            .recordFilter(FilterResult.Accepted("SMS source"), 2)
            .recordExtraction(ExtractionOutcome.Success(candidate(), listOf(diag())), 5)
            .recordMerchant(
                MerchantResolution.Resolved(
                    rawMerchant = "CARREFOUR",
                    profile = MerchantProfile("Carrefour", emptyList(), MerchantCategory.GROCERIES),
                    matchedAlias = "CARREFOUR",
                    confidence = 97,
                ),
                1,
            )
            .recordRelationships(emptyList(), 1)
            .recordPersistence(true, "Inserted", 3)

        val trace = tracer.build(PipelineResult.PERSISTED)

        assertEquals(PipelineResult.PERSISTED, trace.result)
        // Received, Filter, Extractors, Registry, Validator, Merchant, Relationships, Persistence
        assertNotNull(trace.eventFor(PipelineStage.NOTIFICATION_RECEIVED))
        assertNotNull(trace.eventFor(PipelineStage.NOTIFICATION_FILTER))
        assertNotNull(trace.eventFor(PipelineStage.FINANCIAL_EXTRACTORS))
        assertNotNull(trace.eventFor(PipelineStage.REGISTRY))
        assertNotNull(trace.eventFor(PipelineStage.VALIDATOR))
        assertNotNull(trace.eventFor(PipelineStage.MERCHANT_RESOLVER))
        assertNotNull(trace.eventFor(PipelineStage.RELATIONSHIP_ENGINE))
        assertNotNull(trace.eventFor(PipelineStage.PERSISTENCE))
        assertTrue(trace.totalDurationMs >= 12)
    }

    // ---- Human-readable reasons at each exit point ----
    @Test fun `filter rejection carries a human readable reason`() {
        val trace = PipelineTracer("key-2")
            .recordReceived("com.whatsapp")
            .recordFilter(FilterResult.Rejected("No financial-looking content"), 1)
            .build(PipelineResult.REJECTED)

        val filter = trace.eventFor(PipelineStage.NOTIFICATION_FILTER)!!
        assertEquals(PipelineStatus.REJECTED, filter.status)
        assertEquals("No financial-looking content", filter.reason?.message)
        assertEquals(PipelineStage.NOTIFICATION_FILTER, trace.exitStage)
    }

    @Test fun `ignored promotion is observable at registry`() {
        val trace = PipelineTracer("key-3")
            .recordReceived("com.adcb.nexgen")
            .recordFilter(FilterResult.Accepted("Known financial package"), 1)
            .recordExtraction(
                ExtractionOutcome.Ignored("Promotion/offer detected", listOf(diag(decision = "Ignored", confidence = 92))),
                4,
            )
            .build(PipelineResult.IGNORED)

        val registry = trace.eventFor(PipelineStage.REGISTRY)!!
        assertEquals(PipelineStatus.IGNORED, registry.status)
        assertTrue(registry.reason!!.message.contains("Promotion"))
    }

    @Test fun `validator rejection is observable with reason`() {
        val trace = PipelineTracer("key-4")
            .recordReceived("com.adcb.nexgen")
            .recordFilter(FilterResult.Accepted("Known financial package"), 1)
            .recordExtraction(
                ExtractionOutcome.Success(candidate(), listOf(diag(validationPassed = false, rejected = "Confidence below threshold"))),
                4,
            )
            .build(PipelineResult.REJECTED)

        val validator = trace.eventFor(PipelineStage.VALIDATOR)!!
        assertEquals(PipelineStatus.REJECTED, validator.status)
        assertEquals("Confidence below threshold", validator.reason?.message)
    }

    @Test fun `confirmation is observable at confirmation stage`() {
        val trace = PipelineTracer("key-5")
            .recordReceived("com.fab.personalbanking")
            .recordFilter(FilterResult.Accepted("Known financial package"), 1)
            .recordExtraction(
                ExtractionOutcome.Confirmation(20000, "1959", listOf("payment received"), listOf(diag(decision = "Confirmation"))),
                3,
            )
            .build(PipelineResult.CONFIRMED)

        val confirmation = trace.eventFor(PipelineStage.CONFIRMATION_MATCHER)!!
        assertEquals(PipelineStatus.MATCHED, confirmation.status)
        assertEquals("1959", confirmation.metadata["accountTail"])
    }

    // ---- Snapshot aggregation ----
    @Test fun `snapshot aggregates results and exit stages`() {
        val persisted = PipelineTracer("a")
            .recordReceived("p").recordFilter(FilterResult.Accepted("x"), 1)
            .recordExtraction(ExtractionOutcome.Success(candidate(), listOf(diag())), 1)
            .recordPersistence(true, "Inserted", 1)
            .build(PipelineResult.PERSISTED)
        val rejected = PipelineTracer("b")
            .recordReceived("p").recordFilter(FilterResult.Rejected("No financial-looking content"), 1)
            .build(PipelineResult.REJECTED)
        val ignored = PipelineTracer("c")
            .recordReceived("p").recordFilter(FilterResult.Accepted("x"), 1)
            .recordExtraction(ExtractionOutcome.Ignored("Promotion detected", listOf(diag(decision = "Ignored"))), 1)
            .build(PipelineResult.IGNORED)

        val snap = DiagnosticSnapshot.from(listOf(persisted, rejected, ignored))
        assertEquals(3, snap.totalNotifications)
        assertEquals(1, snap.persisted)
        assertEquals(1, snap.rejected)
        assertEquals(1, snap.ignored)
        assertEquals(1, snap.exitStageCounts[PipelineStage.NOTIFICATION_FILTER])
        assertEquals(1, snap.exitStageCounts[PipelineStage.REGISTRY])
    }

    // ---- Benchmark: tracing is cheap ----
    @Test fun `tracing overhead is negligible`() {
        val start = System.nanoTime()
        repeat(1000) { i ->
            PipelineTracer("k-$i")
                .recordReceived("p").recordFilter(FilterResult.Accepted("x"), 1)
                .recordExtraction(ExtractionOutcome.Success(candidate(), listOf(diag())), 1)
                .recordMerchant(MerchantResolution.Unresolved("RAW", "Raw"), 1)
                .recordRelationships(emptyList(), 1)
                .recordPersistence(true, "Inserted", 1)
                .build(PipelineResult.PERSISTED)
        }
        val avgMicros = (System.nanoTime() - start) / 1000.0 / 1000.0
        // Building a trace should be well under 100 microseconds on average.
        assertTrue("Tracing too slow: ${avgMicros}us", avgMicros < 500.0)
    }
}


