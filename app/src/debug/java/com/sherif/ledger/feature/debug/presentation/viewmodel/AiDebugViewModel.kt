package com.sherif.ledger.feature.debug.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.sherif.ledger.feature.ai.audit.AiDebugTrace
import com.sherif.ledger.feature.ai.audit.AiDebugTraceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** RC6's Developer Console AI debug view — full context/prompt/response/validation per request, in-memory only. See AiDebugTraceStore. */
@HiltViewModel
class AiDebugViewModel @Inject constructor(
    private val traceStore: AiDebugTraceStore,
) : ViewModel() {
    val traces: StateFlow<List<AiDebugTrace>> = traceStore.traces

    fun clear() = traceStore.clear()
}
