package com.sherif.ledger.core.common.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealPipelineTracker @Inject constructor() : PipelineTracker {
    
    private val _events = MutableStateFlow<List<PipelineEvent>>(emptyList())
    val events: StateFlow<List<PipelineEvent>> = _events.asStateFlow()

    init {
        com.sherif.ledger.core.common.logging.LedgerLogger.eventTracker = { track(it) }
    }

    override fun track(event: PipelineEvent) {
        _events.update { (it + event).takeLast(200) }
    }

    override fun clear() {
        _events.value = emptyList()
    }
}
