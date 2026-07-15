package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.feature.capture.source.SourceChannel
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.account.AccountIdentityResolver
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
import com.sherif.ledger.feature.semantic.ConfirmationInterpreter
import com.sherif.ledger.feature.semantic.ConfirmationOutcome
import com.sherif.ledger.feature.semantic.FinancialIntent
import com.sherif.ledger.feature.semantic.FinancialIntentClassifier
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.notification.TransactionNotifier
import kotlinx.coroutines.flow.first
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Orchestrates the end-to-end ingestion flow from a raw notification to a persisted transaction.
 *
 * Phase 7 (refined) shape:
 *   Notification -> Filter -> ExtractionRegistry (ALWAYS runs, produces data only)
 *                -> FinancialIntentClassifier (consumes the full ExtractionOutcome,
 *                   sole routing authority) -> Router (the only place a behavioral
 *                   decision is made).
 *
 * The extractor never decides routing, including when it ignores or fails a
 * message — extraction outcome is data the classifier consumes, not a gate.
 * Nothing returns early between extraction and classification.
 *
 * Phase 9: which ACCOUNT a persisted transaction belongs to is resolved by
 * [AccountIdentityResolver] from multiple deterministic signals — never a bare
 * accountId carried on the candidate (extraction never resolves accounts) and
 * never a silent default-account fallback with no evidence trail.
 */
class ProcessNotificationUseCase @Inject constructor(
    private val filter: NotificationFilter,
    private val extractionRegistry: ExtractionRegistry,
    private val confirmationMatcher: ConfirmationMatcher,
    private val reconciliationEngine: ReconciliationEngine,
    private val transactionRepository: TransactionRepository,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val accountIdentityResolver: AccountIdentityResolver,
    private val pipelineTraceSink: PipelineTraceSink,
    private val financialIntentClassifier: FinancialIntentClassifier,
    private val transactionNotifier: TransactionNotifier,
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

        // 1. Filter. The one gate upstream of everything else — content that never
        // looks financial at all does not proceed to extraction or classification.
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

        // 2. Extract. ALWAYS runs. Produces structured DATA only (ExtractionOutcome)
        // and decides nothing about routing — including when it ignores or fails
        // the message. No early return here.
        val extractionOutcome = extractionRegistry.extract(envelope)
        tracer.recordExtraction(extractionOutcome, 0)
        when (extractionOutcome) {
            is ExtractionRegistry.ExtractionOutcome.Success ->
                LedgerLogger.d("ProcessNotificationUseCase: EXTRACTED candidate=${extractionOutcome.candidate}")
            is ExtractionRegistry.ExtractionOutcome.Ignored ->
                LedgerLogger.d("ProcessNotificationUseCase: extraction IGNORED (${extractionOutcome.reason}) - still proceeding to intent classification")
            is ExtractionRegistry.ExtractionOutcome.Failed ->
                LedgerLogger.d("ProcessNotificationUseCase: extraction FAILED (${extractionOutcome.reason}) - still proceeding to intent classification")
            is ExtractionRegistry.ExtractionOutcome.Confirmation ->
                LedgerLogger.d("ProcessNotificationUseCase: extraction matched its own confirmation pattern - still proceeding to intent classification")
        }

        // 3. Classify. The SOLE routing authority. Consumes the notification AND the
        // complete extraction outcome as context - never gated by extractor success.
        // A future local model (Gemma/Phi) replaces only this call.
        val intent = financialIntentClassifier.classify(envelope, extractionOutcome)
        tracer.recordIntent(intent.intent.name, intent.confidence, intent.reasoning.firstOrNull())
        LedgerLogger.pipeline("Intent", "${intent.intent} (conf=${intent.confidence}): ${intent.reasoning.firstOrNull()}")

        // 4. Router. The ONLY place a behavioral decision is made, and it is made
        // from intent alone.
        val candidate: TransactionCandidate
        when (intent.intent) {
            FinancialIntent.FINANCIAL_CONFIRMATION -> {
                // Acknowledgement of an earlier event. Never persist. Amount/tail are
                // read from whatever data extraction happened to produce - used
                // purely as matching data, not as the reason for this route.
                val (amountMinor, accountTail) = amountAndTailFrom(extractionOutcome)
                val cStart = envelope.timestamp.minus(24, ChronoUnit.HOURS)
                val cEnd = envelope.timestamp.plus(24, ChronoUnit.HOURS)
                val nearbyResult = transactionRepository.observeTransactionsBetween(cStart, cEnd).first()
                val nearby = if (nearbyResult is LedgerResult.Success) nearbyResult.data else emptyList()
                val outcome = ConfirmationInterpreter.interpret(
                    confirmationMatcher.match(
                        amountMinor = amountMinor,
                        accountTail = accountTail,
                        confirmationTime = envelope.timestamp,
                        existingTransactions = nearby,
                    ),
                )
                when (outcome) {
                    is ConfirmationOutcome.ConfirmedMatch ->
                        LedgerLogger.pipeline("Confirmation", "Confirmed match to txn #${outcome.transaction.id} (${outcome.confidence}%); no insert")
                    is ConfirmationOutcome.LikelyMatch ->
                        LedgerLogger.pipeline("Confirmation", "Likely match to txn #${outcome.transaction.id} (${outcome.confidence}%); no insert, flagged for review")
                    is ConfirmationOutcome.Unmatched ->
                        // Do NOT invent an expense. Record as unmatched confirmation.
                        LedgerLogger.pipeline("Confirmation", "Unmatched confirmation: ${outcome.reason}; no insert")
                }
                emitTrace(tracer, PipelineResult.CONFIRMED)
                return
            }
            FinancialIntent.FINANCIAL_INFORMATION -> {
                LedgerLogger.pipeline("Intent", "Information; ignored after diagnostics")
                emitTrace(tracer, PipelineResult.IGNORED)
                return
            }
            FinancialIntent.UNKNOWN -> {
                LedgerLogger.pipeline("Intent", "Unknown intent; diagnostics only, no insert")
                emitTrace(tracer, PipelineResult.NOT_APPLICABLE)
                return
            }
            FinancialIntent.FINANCIAL_EVENT -> {
                // Money moved. This is the ONLY class that reaches persistence, and
                // it requires the extractor to have actually produced data.
                val extracted = (extractionOutcome as? ExtractionRegistry.ExtractionOutcome.Success)?.candidate
                if (extracted == null) {
                    // Intent said EVENT but there is no structured data to persist.
                    // Never fabricate a candidate. Surfaced in diagnostics.
                    LedgerLogger.pipeline("Router", "Event intent but no extracted candidate; no insert")
                    emitTrace(tracer, PipelineResult.REJECTED)
                    return
                }
                candidate = extracted
            }
        }

        // 5. Reconcile (only reached for a FINANCIAL_EVENT with a valid candidate).
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

        // 6. Persistence
        var pipelineResult = PipelineResult.NOT_APPLICABLE
        when (reconciliationResult) {
            is ReconciliationResult.New -> {
                LedgerLogger.pipeline("Reconciliation", "Classified as NEW transaction")

                // Which account this belongs to — resolved from multiple
                // deterministic signals, never a bare candidate field and never a
                // silent guess. See AccountIdentityResolver.
                val identity = accountIdentityResolver.resolve(envelope, candidate)
                LedgerLogger.pipeline(
                    "AccountIdentity",
                    "${identity.decision} accountId=${identity.accountId} confidence=${identity.confidence}: ${identity.evidence.joinToString("; ")}",
                )

                val params = InsertTransactionUseCase.Params(
                    accountId = identity.accountId,
                    amountMinor = candidate.amountMinor ?: 0L,
                    currencyCode = candidate.currencyCode ?: CurrencyCode.AED,
                    type = candidate.transactionType ?: TransactionType.EXPENSE,
                    timestamp = candidate.timestamp,
                    source = envelope.source,
                    rawMerchantText = candidate.merchantName ?: "Unknown",
                    cardTail = candidate.accountHint,
                    transferDirection = candidate.transferDirection,
                    origin = candidate.origin,
                )
                LedgerLogger.d("ProcessNotificationUseCase: PERSISTING params=$params")
                val result = insertTransactionUseCase.execute(params)
                if (result is LedgerResult.Success) {
                    LedgerLogger.pipeline("Persistence", "Transaction inserted successfully: ${result.data.id}")
                    tracer.recordPersistence(true, "Transaction inserted: ${result.data.id}", 0)
                    pipelineResult = PipelineResult.PERSISTED

                    // Never let a notification-posting issue undermine an
                    // already-successful persist — this is purely a UX
                    // convenience layered on top of a transaction that's
                    // already safely written.
                    try {
                        transactionNotifier.notifyCaptured(
                            transaction = result.data,
                            merchantOrDescription = candidate.merchantName ?: "Transaction",
                            formattedAmount = MoneyFormatter.format(result.data.amount, includeSymbol = true),
                        )
                    } catch (e: Exception) {
                        LedgerLogger.e("ProcessNotificationUseCase: notifyCaptured failed", e)
                    }
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

    /**
     * Reads amount/tail from whatever the extractor produced, for confirmation
     * matching. Pure data lookup - never a routing decision.
     */
    private fun amountAndTailFrom(outcome: ExtractionRegistry.ExtractionOutcome): Pair<Long?, String?> =
        when (outcome) {
            is ExtractionRegistry.ExtractionOutcome.Success ->
                outcome.candidate.amountMinor to outcome.candidate.accountHint
            is ExtractionRegistry.ExtractionOutcome.Confirmation ->
                outcome.amountMinor to outcome.accountTail
            else -> null to null
        }
}




