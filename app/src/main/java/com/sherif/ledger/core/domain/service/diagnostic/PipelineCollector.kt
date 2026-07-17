package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.feature.diagnostics.PipelineTraceSink
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class PipelineEventDto(
    val stage: String,
    val status: String,
    val durationMs: Long,
    val reason: String?,
    val confidence: Int?,
    val metadata: Map<String, String>,
    val timestampEpochMillis: Long,
)

@Serializable
data class PipelineTraceDto(
    val notificationKey: String,
    val result: String,
    val exitStage: String?,
    val totalDurationMs: Long,
    val events: List<PipelineEventDto>,
)

/**
 * The Pipeline Timeline, built entirely from [PipelineTraceSink] — already
 * eagerly populated by ProcessNotificationUseCase on every live capture (RC3
 * confirmed this, in contrast to the old PipelineTracker's lazy-init bug), so
 * this collector adds no new tracking, only a JSON view over what already
 * exists.
 *
 * Honest gap worth stating rather than papering over: the real pipeline's
 * stages (NOTIFICATION_RECEIVED through PERSISTENCE) do not include a
 * "Balance Calculator" or "Dashboard Update" stage, because balance
 * computation is not part of notification ingestion — it's a separate,
 * on-demand replay triggered whenever the Dashboard is viewed, not a step
 * that happens once per captured transaction. FinancialTraceCollector is
 * where that computation is actually observable; this collector only covers
 * ingestion, which is genuinely all PipelineTraceSink tracks.
 */
class PipelineCollector @Inject constructor(
    private val pipelineTraceSink: PipelineTraceSink,
) : DiagnosticCollector {

    override val id: String = "pipeline_timeline"

    override suspend fun collect(): DiagnosticSection {
        val traces = pipelineTraceSink.recent().map { trace ->
            PipelineTraceDto(
                notificationKey = trace.notificationKey,
                result = trace.result.name,
                exitStage = trace.exitStage?.name,
                totalDurationMs = trace.totalDurationMs,
                events = trace.events.map { e ->
                    PipelineEventDto(
                        stage = e.stage.name,
                        status = e.status.name,
                        durationMs = e.durationMs,
                        reason = e.reason?.message,
                        confidence = e.confidence,
                        metadata = e.metadata,
                        timestampEpochMillis = e.timestamp.toEpochMilli(),
                    )
                },
            )
        }
        val json = Json { prettyPrint = true }
        return DiagnosticSection.Json(id, json.encodeToString(traces))
    }
}



