package com.sherif.ledger.core.common.diagnostics

/**
 * Interface for tracking pipeline events.
 * Implemented only in debug builds.
 */
interface PipelineTracker {
    fun track(event: PipelineEvent)
    fun clear()
}
