package com.sherif.ledger.feature.diagnostics

import java.time.Instant

/**
 * Phase 6 — Runtime Diagnostics & Pipeline Observability.
 *
 * A PASSIVE observability layer. It does NOT run, alter, or re-decide any pipeline
 * behavior. It is a set of plain data types plus a tracer that records what each
 * stage's public output already reports, producing a unified [PipelineTrace] the
 * Developer Console can consume later.
 *
 * No Compose, no ViewModels, no navigation, no UI. Pure JVM data + assembly.
 */

/** The ordered stages of the ingestion pipeline. */
enum class PipelineStage {
    NOTIFICATION_RECEIVED,
    NOTIFICATION_FILTER,
    FINANCIAL_EXTRACTORS,
    REGISTRY,
    VALIDATOR,
    CONFIRMATION_MATCHER,
    MERCHANT_RESOLVER,
    RELATIONSHIP_ENGINE,
    PERSISTENCE,
}

/** The outcome of a single stage. */
enum class PipelineStatus {
    /** Stage ran and passed the message onward. */
    PASSED,

    /** Stage produced a positive match (e.g. confirmation, merchant resolved). */
    MATCHED,

    /** Stage deliberately dropped the message as non-transactional (e.g. promo). */
    IGNORED,

    /** Stage rejected the message (e.g. validator failed, duplicate). */
    REJECTED,

    /** Stage did not apply to this message. */
    SKIPPED,
}

/**
 * A human-readable reason for a stage outcome, with an optional machine code so the
 * Developer Console can group/filter without string-matching.
 */
data class PipelineReason(
    val message: String,
    val code: String? = null,
) {
    override fun toString(): String = message
}

/**
 * One observed stage event. Carries everything the console needs to render a row:
 * stage, status, duration, reason, confidence, and free-form metadata.
 */
data class PipelineEvent(
    val stage: PipelineStage,
    val status: PipelineStatus,
    val durationMs: Long,
    val reason: PipelineReason?,
    val confidence: Int?,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Instant = Instant.now(),
)

/** Where the pipeline ended for a given notification. */
enum class PipelineResult {
    PERSISTED,
    CONFIRMED,
    IGNORED,
    REJECTED,
    NOT_APPLICABLE,
}

/**
 * The complete trace for one notification: the ordered events plus the terminal
 * result. Immutable snapshot for the console.
 */
data class PipelineTrace(
    val notificationKey: String,
    val events: List<PipelineEvent>,
    val result: PipelineResult,
    val startedAt: Instant,
    val finishedAt: Instant,
) {
    val totalDurationMs: Long get() = events.sumOf { it.durationMs }

    /** The stage where the pipeline exited, if it did not reach persistence. */
    val exitStage: PipelineStage?
        get() = events.lastOrNull {
            it.status == PipelineStatus.REJECTED || it.status == PipelineStatus.IGNORED
        }?.stage

    fun eventFor(stage: PipelineStage): PipelineEvent? = events.firstOrNull { it.stage == stage }
}

/**
 * An aggregate view over many traces, for the console's summary panel. Pure
 * derivation; holds no live state.
 */
data class DiagnosticSnapshot(
    val totalNotifications: Int,
    val persisted: Int,
    val confirmed: Int,
    val ignored: Int,
    val rejected: Int,
    val notApplicable: Int,
    val exitStageCounts: Map<PipelineStage, Int>,
    val avgTotalDurationMs: Double,
) {
    companion object {
        fun from(traces: List<PipelineTrace>): DiagnosticSnapshot {
            val exitCounts = traces.mapNotNull { it.exitStage }
                .groupingBy { it }.eachCount()
            val avg = if (traces.isEmpty()) 0.0
            else traces.map { it.totalDurationMs }.average()
            return DiagnosticSnapshot(
                totalNotifications = traces.size,
                persisted = traces.count { it.result == PipelineResult.PERSISTED },
                confirmed = traces.count { it.result == PipelineResult.CONFIRMED },
                ignored = traces.count { it.result == PipelineResult.IGNORED },
                rejected = traces.count { it.result == PipelineResult.REJECTED },
                notApplicable = traces.count { it.result == PipelineResult.NOT_APPLICABLE },
                exitStageCounts = exitCounts,
                avgTotalDurationMs = avg,
            )
        }
    }
}

