package com.sherif.ledger.feature.diagnostics

import com.sherif.ledger.feature.capture.extraction.ExtractionRegistry.ExtractionOutcome
import com.sherif.ledger.feature.capture.notification.FilterResult
import com.sherif.ledger.feature.merchant.MerchantResolution
import com.sherif.ledger.feature.relationship.FinancialRelationship
import java.time.Instant

/**
 * A passive recorder of pipeline stage events for one notification.
 *
 * The tracer NEVER calls the pipeline and NEVER decides anything. A caller (the
 * Developer Console or a test) runs the real pipeline's public methods and hands
 * each result to the matching `record*` method here. The tracer maps that output
 * into a [PipelineEvent]. This keeps the pipeline byte-identical and behavior
 * unchanged: the trace is built entirely from outputs the stages already produce.
 *
 * Not thread-safe; one tracer per notification.
 */
class PipelineTracer(private val notificationKey: String) {

    private val events = mutableListOf<PipelineEvent>()
    private val startedAt: Instant = Instant.now()

    /** Stage 1: the notification entered the pipeline. */
    fun recordReceived(packageName: String, durationMs: Long = 0): PipelineTracer = apply {
        events += PipelineEvent(
            stage = PipelineStage.NOTIFICATION_RECEIVED,
            status = PipelineStatus.PASSED,
            durationMs = durationMs,
            reason = PipelineReason("Notification received", "received"),
            confidence = null,
            metadata = mapOf("package" to packageName),
        )
    }

    /** Stage 2: NotificationFilter.evaluate() result. */
    fun recordFilter(result: FilterResult, durationMs: Long): PipelineTracer = apply {
        val status = if (result.isAccepted) PipelineStatus.PASSED else PipelineStatus.REJECTED
        events += PipelineEvent(
            stage = PipelineStage.NOTIFICATION_FILTER,
            status = status,
            durationMs = durationMs,
            reason = PipelineReason(result.reason, if (result.isAccepted) "filter_accepted" else "filter_rejected"),
            confidence = null,
        )
    }

    /**
     * Stages 3-6: the registry outcome. A single [ExtractionOutcome] already encodes
     * the combined extractor + registry + validator + confirmation decision, so this
     * records the extractor and registry stages, and (when present) the validator and
     * confirmation stages from the same outcome.
     */
    fun recordExtraction(outcome: ExtractionOutcome, durationMs: Long): PipelineTracer = apply {
        val best = outcome.diagnostics.maxByOrNull { it.confidence }
        val extractorMeta = buildMap {
            best?.let {
                put("extractor", it.extractor)
                put("decision", it.decision)
                put("category", it.category)
            }
        }
        // Financial extractors stage
        events += PipelineEvent(
            stage = PipelineStage.FINANCIAL_EXTRACTORS,
            status = if (outcome is ExtractionOutcome.Failed) PipelineStatus.SKIPPED else PipelineStatus.PASSED,
            durationMs = durationMs,
            reason = PipelineReason(
                when (outcome) {
                    is ExtractionOutcome.Success -> "Candidate extracted"
                    is ExtractionOutcome.Ignored -> "No transaction extracted"
                    is ExtractionOutcome.Failed -> "No extractor produced a candidate"
                    is ExtractionOutcome.Confirmation -> "Confirmation candidate detected"
                },
                "extractors",
            ),
            confidence = best?.confidence,
            metadata = extractorMeta,
        )
        // Registry stage — the terminal decision
        val (status, reason, code) = when (outcome) {
            is ExtractionOutcome.Success -> Triple(PipelineStatus.PASSED, "Extraction selected", "registry_extracted")
            is ExtractionOutcome.Ignored -> Triple(PipelineStatus.IGNORED, outcome.reason, "registry_ignored")
            is ExtractionOutcome.Failed -> Triple(PipelineStatus.REJECTED, outcome.reason, "registry_failed")
            is ExtractionOutcome.Confirmation -> Triple(PipelineStatus.MATCHED, "Confirmation recognized", "registry_confirmation")
        }
        events += PipelineEvent(
            stage = PipelineStage.REGISTRY,
            status = status,
            durationMs = 0,
            reason = PipelineReason(reason, code),
            confidence = best?.confidence,
        )
        // Validator stage — derived from diagnostics (observed, not re-run)
        best?.let { d ->
            events += PipelineEvent(
                stage = PipelineStage.VALIDATOR,
                status = if (d.validationPassed) PipelineStatus.PASSED else PipelineStatus.REJECTED,
                durationMs = 0,
                reason = PipelineReason(
                    if (d.validationPassed) "Validation passed" else (d.rejectedReason ?: "Validation failed"),
                    if (d.validationPassed) "validator_passed" else "validator_rejected",
                ),
                confidence = d.confidence,
            )
        }
        // Confirmation stage — only when the outcome is a confirmation
        if (outcome is ExtractionOutcome.Confirmation) {
            events += PipelineEvent(
                stage = PipelineStage.CONFIRMATION_MATCHER,
                status = PipelineStatus.MATCHED,
                durationMs = 0,
                reason = PipelineReason("Credit card / payment confirmation", "confirmation_matched"),
                confidence = null,
                metadata = buildMap {
                    outcome.amountMinor?.let { put("amountMinor", it.toString()) }
                    outcome.accountTail?.let { put("accountTail", it) }
                },
            )
        }
    }

    /** Stage 7: MerchantResolver.resolve() result. */
    fun recordMerchant(resolution: MerchantResolution, durationMs: Long): PipelineTracer = apply {
        val (status, reason, meta) = when (resolution) {
            is MerchantResolution.Resolved -> Triple(
                PipelineStatus.MATCHED,
                "Merchant resolved: ${resolution.canonicalName}",
                mapOf("canonical" to resolution.canonicalName, "alias" to resolution.matchedAlias),
            )
            is MerchantResolution.Unresolved -> Triple(
                PipelineStatus.SKIPPED,
                "Merchant not resolved",
                mapOf("raw" to resolution.rawMerchant),
            )
        }
        events += PipelineEvent(
            stage = PipelineStage.MERCHANT_RESOLVER,
            status = status,
            durationMs = durationMs,
            reason = PipelineReason(reason, "merchant"),
            confidence = (resolution as? MerchantResolution.Resolved)?.confidence,
            metadata = meta,
        )
    }

    /** Stage 8: RelationshipEngine.analyze() result (relationships for this txn). */
    fun recordRelationships(relationships: List<FinancialRelationship>, durationMs: Long): PipelineTracer = apply {
        events += PipelineEvent(
            stage = PipelineStage.RELATIONSHIP_ENGINE,
            status = if (relationships.isEmpty()) PipelineStatus.SKIPPED else PipelineStatus.MATCHED,
            durationMs = durationMs,
            reason = PipelineReason(
                if (relationships.isEmpty()) "No relationships found"
                else "${relationships.size} relationship(s) found",
                "relationships",
            ),
            confidence = relationships.maxOfOrNull { it.confidence.value },
            metadata = mapOf("types" to relationships.joinToString(",") { it.type.name }),
        )
    }

    /** Stage 9: persistence result. */
    fun recordPersistence(persisted: Boolean, reason: String, durationMs: Long): PipelineTracer = apply {
        events += PipelineEvent(
            stage = PipelineStage.PERSISTENCE,
            status = if (persisted) PipelineStatus.PASSED else PipelineStatus.REJECTED,
            durationMs = durationMs,
            reason = PipelineReason(reason, if (persisted) "persisted" else "not_persisted"),
            confidence = null,
        )
    }

    /**
     * Records a stage that exists in the model but is not part of this execution
     * path (e.g. Merchant Resolver / Relationship Engine during live ingestion).
     * Emitted so the timeline faithfully shows the full pipeline shape with these
     * stages marked NOT_EXECUTED rather than silently absent.
     */
    fun recordStageNotExecuted(stage: PipelineStage, reason: String): PipelineTracer = apply {
        events += PipelineEvent(
            stage = stage,
            status = PipelineStatus.NOT_EXECUTED,
            durationMs = 0,
            reason = PipelineReason(reason, "not_executed"),
            confidence = null,
        )
    }

    /** Records a persistence stage that did not run because the pipeline exited earlier. */
    fun recordPersistenceNotReached(reason: String): PipelineTracer = apply {
        events += PipelineEvent(
            stage = PipelineStage.PERSISTENCE,
            status = PipelineStatus.NOT_EXECUTED,
            durationMs = 0,
            reason = PipelineReason(reason, "not_reached"),
            confidence = null,
        )
    }

    /** Assemble the immutable trace. [result] is the terminal classification. */
    fun build(result: PipelineResult): PipelineTrace = PipelineTrace(
        notificationKey = notificationKey,
        events = events.toList(),
        result = result,
        startedAt = startedAt,
        finishedAt = Instant.now(),
    )
}


