package com.sherif.ledger.feature.ai.audit

import com.sherif.ledger.feature.ai.domain.AICapability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class CacheStatus { HIT, MISS, NOT_APPLICABLE }

/**
 * RC6's "Developer Console" debug view — Context Preview, Prompt Preview,
 * Response Preview, Validation Result, Execution Time, Errors, Cache Status.
 *
 * Deliberately DIFFERENT from [AiAuditLogger]/`ai_audit_log`: this holds the
 * FULL prompt and raw response text, which the DB-backed audit log
 * intentionally never stores (see AiAuditLogEntity's doc comment — a table
 * that could end up in a future diagnostic collector must not carry prompt/
 * response content). This store is in-memory only, capped, never written to
 * disk, never exported, and only ever read by Developer Console screens
 * (debug builds only, by the app's existing convention).
 */
data class AiDebugTrace(
    val timestampMillis: Long,
    val capability: AICapability,
    val providerId: String,
    val model: String,
    val renderedContext: String,
    val prompt: String,
    val rawResponse: String?,
    val validationResult: String,
    val executionTimeMs: Long,
    val cacheStatus: CacheStatus,
    val error: String?,
)

@Singleton
class AiDebugTraceStore @Inject constructor() {
    private val _traces = MutableStateFlow<List<AiDebugTrace>>(emptyList())
    val traces: StateFlow<List<AiDebugTrace>> = _traces.asStateFlow()

    fun record(trace: AiDebugTrace) {
        _traces.value = (listOf(trace) + _traces.value).take(MAX_TRACES)
    }

    fun clear() {
        _traces.value = emptyList()
    }

    companion object {
        private const val MAX_TRACES = 20
    }
}
