package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.feature.capture.source.SourceChannel
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.usecase.account.EnsureDefaultAccountUseCase
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.notification.NotificationFilter
import com.sherif.ledger.feature.capture.extraction.ConfirmationMatcher
import com.sherif.ledger.feature.capture.extraction.ExtractionRegistry
import com.sherif.ledger.feature.capture.reconciliation.ReconciliationEngine
import com.sherif.ledger.feature.capture.reconciliation.ReconciliationResult
import com.sherif.ledger.feature.diagnostics.PipelineResult
import com.sherif.ledger.feature.diagnostics.PipelineStage
import com.sherif.ledger.feature.diagnostics.PipelineTraceSink
import com.sherif.ledger.feature.diagnostics.PipelineTracer
import com.sherif.ledger.feature.semantic.SemanticClass
import com.sherif.ledger.feature.semantic.SemanticEventClassifier
import kotlinx.coroutines.flow.first
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Orchestrates the end-to-end ingestion flow from a raw notification to a persisted transaction.
 */
class ProcessNotificationUseCase @Inject constructor(
    private val filter: NotificationFilter,
    private val extractionRegistry: ExtractionRegistry,
    private val confirmationMatcher: ConfirmationMatcher,
    private val reconciliationEngine: ReconciliationEngine,
    private val transactionRepository: TransactionRepository,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val ensureDefaultAccountUseCase: EnsureDefaultAccountUseCase,
    private val pipelineTraceSink: PipelineTraceSink,
    private val semanticEventClassifier: SemanticEventClassifier,
) {
    init {
        com.sherif.ledger.core.common.logging.LedgerLogger.d("EXECUTING: ProcessNotificationUseCase")
    }

    suspend fun execute(
        envelope: NotificationEnvelope,
        channel: SourceChannel = SourceChannel.NOTIFICATION,
    ) {
        val traceId = envelope.notificationKey
        LedgerLogger.setTraceId(traceId)

        LedgerLogger.d("ProcessNotificationUseCase.execute(envelope=$envelope)")
        LedgerLogger.pipeline("Capture", "channel=$channel source=${envelope.packageName}")

        // Passive observability (Phase 6B). Emits a PipelineTrace of the real
        // execution. Records values the pipeline already computes; never alters
        // control flow, decisions, or timing-sensitive behavior.
        val tracer = PipelineTracer(traceId)
        tracer.recordReceived(envelope.packageName)

        // 1. Filter
        val filterResult = filter.evaluate(envelope)
        if (!filterResult.isAccepted) {
            LedgerLogger.d("ProcessNotificationUseCase: REJECTED by Filter (Sender=${envelope.packageName}, Source=${envelope.source})")
            LedgerLogger.pipeline("Filter", "Ignored: ${envelope.packageName}")
            tracer.recordFilter(filterResult, 0)
            emitTrace(tracer, PipelineResult.REJECTED)
            return
        }
        LedgerLogger.d("ProcessNotificationUseCase: ACCEPTED by Filter")
        LedgerLogger.pipeline("Filter", "Accepted: ${envelope.packageName}")
        tracer.recordFilter(filterResult, 0)

        // 2. Extract (deterministic + intent-aware heuristic extractors, ranked and
        // validated behind one registry). Everything from the candidate onward is unchanged.
        val extractionOutcome = extractionRegistry.extract(envelope)
        // Extractor + Registry + Validator stages, derived from the outcome's
        // per-extractor diagnostics (no instrumentation inside the registry).
        tracer.recordExtraction(extractionOutcome, 0)
        val candidate = when (extractionOutcome) {
            is ExtractionRegistry.ExtractionOutcome.Success -> {
                LedgerLogger.d("ProcessNotificationUseCase: EXTRACTED candidate=${extractionOutcome.candidate}")
                LedgerLogger.pipeline("Parser", "Matched: ${extractionOutcome.candidate.merchantName}")
                extractionOutcome.candidate
            }
            is ExtractionRegistry.ExtractionOutcome.Ignored -> {
                LedgerLogger.d("ProcessNotificationUseCase: IGNORED (${extractionOutcome.reason})")
                LedgerLogger.pipeline("Parser", "Intentionally ignored: ${extractionOutcome.reason}")
                emitTrace(tracer, PipelineResult.IGNORED)
                return
            }
            is ExtractionRegistry.ExtractionOutcome.Failed -> {
                LedgerLogger.d("ProcessNotificationUseCase: EXTRACTION FAILED reason=${extractionOutcome.reason}")
                LedgerLogger.pipeline("Parser", "Failed: ${extractionOutcome.reason}")
                emitTrace(tracer, PipelineResult.REJECTED)
                return
            }
            is ExtractionRegistry.ExtractionOutcome.Confirmation -> {
                val cStart = envelope.timestamp.minus(24, ChronoUnit.HOURS)
                val cEnd = envelope.timestamp.plus(24, ChronoUnit.HOURS)
                val nearbyResult = transactionRepository.observeTransactionsBetween(cStart, cEnd).first()
                val nearby = if (nearbyResult is LedgerResult.Success) nearbyResult.data else emptyList()
                when (val match = confirmationMatcher.match(
                    amountMinor = extractionOutcome.amountMinor,
                    accountTail = extractionOutcome.accountTail,
                    confirmationTime = envelope.timestamp,
                    existingTransactions = nearby,
                )) {
                    is ConfirmationMatcher.MatchResult.Matched ->
                        LedgerLogger.pipeline("Confirmation", "Confirmed existing txn #${match.transaction.id}; no insert")
                    is ConfirmationMatcher.MatchResult.Unmatched ->
                        LedgerLogger.pipeline("Confirmation", "Unmatched: ${match.reason}; dropped")
                }
                emitTrace(tracer, PipelineResult.CONFIRMED)
                return
            }
        }

        // 2b. Semantic Event Resolution (Phase 7). The extractor said "transaction",
        // but a message can be an acknowledgement of an EARLIER event (a bank saying
        // "payment received/processed") rather than new money movement. Classify the
        // real-world meaning; a future local model replaces only this classifier.
        val semantic = semanticEventClassifier.classify(envelope, candidate)
        when (semantic.semanticClass) {
            SemanticClass.FINANCIAL_CONFIRMATION -> {
                // Not a new event. Route to confirmation matching against an existing
                // transaction; never persist. Reconstructs one financial action from
                // multiple notifications (ADCB debit + FAB "payment received").
                LedgerLogger.pipeline("Semantic", "Confirmation (conf=${semantic.confidence}): ${semantic.reasoning.firstOrNull()}")
                val cStart = envelope.timestamp.minus(24, ChronoUnit.HOURS)
                val cEnd = envelope.timestamp.plus(24, ChronoUnit.HOURS)
                val nearbyResult = transactionRepository.observeTransactionsBetween(cStart, cEnd).first()
                val nearby = if (nearbyResult is LedgerResult.Success) nearbyResult.data else emptyList()
                when (val match = confirmationMatcher.match(
                    amountMinor = candidate.amountMinor,
                    accountTail = candidate.accountHint,
                    confirmationTime = envelope.timestamp,
                    existingTransactions = nearby,
                )) {
                    is ConfirmationMatcher.MatchResult.Matched ->
                        LedgerLogger.pipeline("Semantic", "Confirmation attached to txn #${match.transaction.id}; no insert")
                    is ConfirmationMatcher.MatchResult.Unmatched ->
                        // Do NOT invent an expense. Record as unmatched confirmation.
                        LedgerLogger.pipeline("Semantic", "Unmatched confirmation: ${match.reason}; no insert")
                }
                emitTrace(tracer, PipelineResult.CONFIRMED)
                return
            }
            SemanticClass.FINANCIAL_INFORMATION -> {
                // Statement / balance / limit notice. Non-transactional.
                LedgerLogger.pipeline("Semantic", "Information (conf=${semantic.confidence}); no insert")
                emitTrace(tracer, PipelineResult.IGNORED)
                return
            }
            SemanticClass.FINANCIAL_EVENT, SemanticClass.UNKNOWN -> {
                // Money moved (or no reason to reclassify). Proceed to reconcile + persist.
                LedgerLogger.pipeline("Semantic", "Event (conf=${semantic.confidence}); proceeding")
            }
        }

        // 3. Reconcile
        val start = candidate.timestamp.minus(24, ChronoUnit.HOURS)
        val end = candidate.timestamp.plus(24, ChronoUnit.HOURS)
        
        val existingTransactionsResult = transactionRepository.observeTransactionsBetween(start, end).first()
        val existingTransactions = if (existingTransactionsResult is LedgerResult.Success) {
            existingTransactionsResult.data
        } else emptyList()

        val reconciliationResult = reconciliationEngine.reconcile(candidate, existingTransactions)
        LedgerLogger.d("ProcessNotificationUseCase: RECONCILIATION RESULT=${reconciliationResult::class.simpleName}")

        // Merchant Resolver and Relationship Engine are part of the diagnostics
        // model but are NOT invoked during live ingestion (they are downstream
        // layers). Emit them as present-but-inactive so the timeline is faithful.
        tracer.recordStageNotExecuted(PipelineStage.MERCHANT_RESOLVER, "Not invoked during live ingestion")
        tracer.recordStageNotExecuted(PipelineStage.RELATIONSHIP_ENGINE, "Not invoked during live ingestion")

        // 4. Persistence
        var pipelineResult = PipelineResult.NOT_APPLICABLE
        when (reconciliationResult) {
            is ReconciliationResult.New -> {
                LedgerLogger.pipeline("Reconciliation", "Classified as NEW transaction")
                
                // Ensure a valid account exists before insertion
                val accountId = candidate.accountId ?: ensureDefaultAccountUseCase.execute()
                
                val params = InsertTransactionUseCase.Params(
                    accountId = accountId,
                    amountMinor = candidate.amountMinor ?: 0L,
                    currencyCode = candidate.currencyCode ?: CurrencyCode.AED,
                    type = candidate.transactionType ?: TransactionType.EXPENSE,
                    timestamp = candidate.timestamp,
                    source = envelope.source,
                    rawMerchantText = candidate.merchantName ?: "Unknown",
                    cardTail = candidate.accountHint
                )
                LedgerLogger.d("ProcessNotificationUseCase: PERSISTING params=$params")
                val result = insertTransactionUseCase.execute(params)
                if (result is LedgerResult.Success) {
                    LedgerLogger.pipeline("Persistence", "Transaction inserted successfully: ${result.data.id}")
                    tracer.recordPersistence(true, "Transaction inserted: ${result.data.id}", 0)
                    pipelineResult = PipelineResult.PERSISTED
                } else if (result is LedgerResult.Failure) {
                    LedgerLogger.e("Persistence failed: ${result.error}")
                    tracer.recordPersistence(false, "Persistence failed: ${result.error}", 0)
                    pipelineResult = PipelineResult.REJECTED
                }
            }
            is ReconciliationResult.Updated -> {
                LedgerLogger.pipeline("Reconciliation", "Classified as UPDATE for ${reconciliationResult.existingTransactionId}")
                tracer.recordPersistence(true, "Updated existing #${reconciliationResult.existingTransactionId}", 0)
                pipelineResult = PipelineResult.PERSISTED
            }
            is ReconciliationResult.Duplicate -> {
                LedgerLogger.pipeline("Reconciliation", "Ignored: Duplicate of ${reconciliationResult.existingTransactionId}")
                tracer.recordPersistenceNotReached("Duplicate of #${reconciliationResult.existingTransactionId}")
                pipelineResult = PipelineResult.IGNORED
            }
            ReconciliationResult.Ignored -> {
                LedgerLogger.pipeline("Reconciliation", "Ignored by engine")
                tracer.recordPersistenceNotReached("Ignored by reconciliation engine")
                pipelineResult = PipelineResult.IGNORED
            }
        }

        emitTrace(tracer, pipelineResult)
        LedgerLogger.setTraceId(null)
    }

    /**
     * Passive: hands the completed trace to the sink. Wrapped so any diagnostics
     * error can never affect ingestion (telemetry must not break the pipeline).
     */
    private fun emitTrace(tracer: PipelineTracer, result: PipelineResult) {
        runCatching { pipelineTraceSink.record(tracer.build(result)) }
    }
}


