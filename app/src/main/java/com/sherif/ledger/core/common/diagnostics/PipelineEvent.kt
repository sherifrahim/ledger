package com.sherif.ledger.core.common.diagnostics

/**
 * Stages of the Ledger Ingestion Pipeline.
 */
enum class PipelineStage {
    CAPTURE,
    FILTER,
    PARSER,
    NORMALIZATION,
    RECONCILIATION,
    PERSISTENCE,
    UI_REFRESH
}

/**
 * Status of a single pipeline stage.
 */
sealed interface StageStatus {
    data object Success : StageStatus
    data class SuccessWithDetails(val details: String) : StageStatus
    data object Ignored : StageStatus
    data class Failed(val reason: String, val stackTrace: String? = null) : StageStatus
}

/**
 * A diagnostic event emitted by the pipeline.
 */
data class PipelineEvent(
    val stage: PipelineStage,
    val status: StageStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val traceId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
