package com.sherif.ledger.core.common.diagnostics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealPipelineTracker @Inject constructor() : PipelineTracker {
    override fun track(event: PipelineEvent) {
        // No-op in release
    }
    override fun clear() {
        // No-op in release
    }
}
