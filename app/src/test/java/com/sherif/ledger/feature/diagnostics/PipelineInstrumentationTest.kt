package com.sherif.ledger.feature.diagnostics

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.notification.FilterResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6B regression: the passive instrumentation must not change behavior. These
 * tests exercise the tracer and sink directly (the live wiring is covered by the
 * existing ProcessNotificationUseCase tests, which still pass unchanged with the
 * added sink argument). The assertions here prove the trace faithfully represents
 * the execution states, including the new NOT_EXECUTED stages.
 */
class PipelineInstrumentationTest {

    private val sink = PipelineTraceSink()

    // ---- The new richer statuses ----
    @Test fun `merchant and relationship stages are marked NOT_EXECUTED in ingestion`() {
        val trace = PipelineTracer("k")
            .recordReceived("com.adcb.nexgen")
            .recordFilter(FilterResult.Accepted("SMS source"), 0)
            .recordStageNotExecuted(PipelineStage.MERCHANT_RESOLVER, "Not invoked during live ingestion")
            .recordStageNotExecuted(PipelineStage.RELATIONSHIP_ENGINE, "Not invoked during live ingestion")
            .recordPersistence(true, "Inserted", 0)
            .build(PipelineResult.PERSISTED)

        assertEquals(PipelineStatus.NOT_EXECUTED, trace.eventFor(PipelineStage.MERCHANT_RESOLVER)?.status)
        assertEquals(PipelineStatus.NOT_EXECUTED, trace.eventFor(PipelineStage.RELATIONSHIP_ENGINE)?.status)
        // NOT_EXECUTED must be distinguishable from a real exit (not counted as exit stage).
        assertEquals(null, trace.exitStage)
    }

    @Test fun `not_executed is distinct from skipped and rejected`() {
        // The console must tell these apart.
        assertNotNull(PipelineStatus.NOT_EXECUTED)
        assertNotNull(PipelineStatus.NOT_APPLICABLE)
        assertNotNull(PipelineStatus.FAILED)
        assertFalse(PipelineStatus.NOT_EXECUTED == PipelineStatus.SKIPPED)
        assertFalse(PipelineStatus.NOT_EXECUTED == PipelineStatus.REJECTED)
    }

    // ---- The sink is passive and bounded ----
    @Test fun `sink records traces and can be disabled with no effect`() {
        sink.clear()
        sink.enabled = true
        val trace = PipelineTracer("k1")
            .recordReceived("p")
            .recordFilter(FilterResult.Rejected("No financial-looking content"), 0)
            .build(PipelineResult.REJECTED)
        sink.record(trace)
        assertEquals(1, sink.recent().size)

        sink.enabled = false
        sink.record(
            PipelineTracer("k2").recordReceived("p").build(PipelineResult.NOT_APPLICABLE),
        )
        // Disabled: no new trace recorded.
        assertEquals(1, sink.recent().size)
    }

    @Test fun `sink is bounded and does not grow without limit`() {
        sink.clear()
        sink.enabled = true
        repeat(500) { i ->
            sink.record(
                PipelineTracer("k-$i").recordReceived("p").build(PipelineResult.PERSISTED),
            )
        }
        // Bounded ring (max 200).
        assertTrue("Sink unbounded: ${sink.recent().size}", sink.recent().size <= 200)
    }

    // ---- Trace existence is the ONLY observable difference ----
    @Test fun `a complete trace exists after a persisted run`() {
        sink.clear()
        sink.enabled = true
        val trace = PipelineTracer("persist")
            .recordReceived("com.adcb.nexgen")
            .recordFilter(FilterResult.Accepted("Known financial package"), 0)
            .recordStageNotExecuted(PipelineStage.MERCHANT_RESOLVER, "Not invoked during live ingestion")
            .recordStageNotExecuted(PipelineStage.RELATIONSHIP_ENGINE, "Not invoked during live ingestion")
            .recordPersistence(true, "Inserted #7", 0)
            .build(PipelineResult.PERSISTED)
        sink.record(trace)

        val got = sink.recent().last()
        assertEquals(PipelineResult.PERSISTED, got.result)
        assertNotNull(got.eventFor(PipelineStage.NOTIFICATION_RECEIVED))
        assertNotNull(got.eventFor(PipelineStage.PERSISTENCE))
        assertEquals("persist", got.notificationKey)
    }

    @Test fun `snapshot aggregates persisted ignored and rejected outcomes`() {
        sink.clear()
        sink.enabled = true
        sink.record(PipelineTracer("a").recordReceived("p").build(PipelineResult.PERSISTED))
        sink.record(PipelineTracer("b").recordReceived("p").build(PipelineResult.IGNORED))
        sink.record(PipelineTracer("c").recordReceived("p").build(PipelineResult.REJECTED))
        val snap = sink.snapshot()
        assertEquals(3, snap.totalNotifications)
        assertEquals(1, snap.persisted)
        assertEquals(1, snap.ignored)
        assertEquals(1, snap.rejected)
    }
}


