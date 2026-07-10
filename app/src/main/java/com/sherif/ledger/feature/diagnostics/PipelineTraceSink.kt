package com.sherif.ledger.feature.diagnostics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A lightweight collector for completed [PipelineTrace]s, so the live pipeline can
 * emit a trace per notification and the Developer Console can read them later.
 *
 * Passive by construction: recording a trace is an append to a bounded in-memory
 * ring. When [enabled] is false the sink does no work at all, so instrumentation
 * overhead is effectively negligible when diagnostics are off. Holds no business
 * state and influences no pipeline decision.
 *
 * Not a persistence layer — this is transient, developer-facing observability.
 */
@Singleton
class PipelineTraceSink @Inject constructor() {

    @Volatile
    var enabled: Boolean = true

    private val lock = Any()
    private val maxTraces = 200
    private val traces = ArrayDeque<PipelineTrace>(maxTraces)

    /** Append a completed trace. No-op when disabled. */
    fun record(trace: PipelineTrace) {
        if (!enabled) return
        synchronized(lock) {
            if (traces.size >= maxTraces) traces.removeFirst()
            traces.addLast(trace)
        }
    }

    /** Snapshot of recorded traces (most recent last). */
    fun recent(): List<PipelineTrace> = synchronized(lock) { traces.toList() }

    /** Aggregate view for the console summary panel. */
    fun snapshot(): DiagnosticSnapshot = DiagnosticSnapshot.from(recent())

    fun clear() = synchronized(lock) { traces.clear() }
}

