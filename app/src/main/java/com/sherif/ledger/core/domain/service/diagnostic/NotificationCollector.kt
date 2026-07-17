package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.feature.diagnostics.PipelineTraceSink
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class NotificationHistoryEntryDto(
    val notificationKey: String,
    val result: String,
    val exitStage: String?,
    val startedAtEpochMillis: Long,
    val totalDurationMs: Long,
)

/**
 * "What happened to each captured message" at a glance — one row per
 * notification, not the full per-stage detail PipelineCollector exposes.
 * Same underlying source (PipelineTraceSink), different level of detail for
 * different questions: this answers "how many notifications came in and what
 * became of them," PipelineCollector answers "why, stage by stage."
 */
class NotificationCollector @Inject constructor(
    private val pipelineTraceSink: PipelineTraceSink,
) : DiagnosticCollector {

    override val id: String = "notification_history"

    override suspend fun collect(): DiagnosticSection {
        val entries = pipelineTraceSink.recent().map { trace ->
            NotificationHistoryEntryDto(
                notificationKey = trace.notificationKey,
                result = trace.result.name,
                exitStage = trace.exitStage?.name,
                startedAtEpochMillis = trace.startedAt.toEpochMilli(),
                totalDurationMs = trace.totalDurationMs,
            )
        }
        val json = Json { prettyPrint = true }
        return DiagnosticSection.Json(id, json.encodeToString(entries))
    }
}



