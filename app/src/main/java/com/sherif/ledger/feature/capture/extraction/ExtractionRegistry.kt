package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single entry point for extraction. Owns the set of [FinancialExtractor]s,
 * runs them, validates output, ranks by extractor-owned confidence, and returns
 * one outcome plus per-attempt [ExtractionDiagnostics] for the Developer Console.
 *
 * Never persists, reconciles, or fingerprints. Diagnostics never affect any
 * financial decision.
 */
@Singleton
class ExtractionRegistry @Inject constructor(
    private val extractors: Set<@JvmSuppressWildcards FinancialExtractor>,
    private val validator: ExtractionValidator,
) {

    private val disagreementPenalty = 30

    suspend fun extract(envelope: NotificationEnvelope): ExtractionOutcome {
        val diagnostics = mutableListOf<ExtractionDiagnostics>()

        val applicable = extractors.filter {
            runCatching { it.canAttempt(envelope) }.getOrDefault(false)
        }
        if (applicable.isEmpty()) {
            return ExtractionOutcome.Failed("No extractor applicable for ${envelope.packageName}", diagnostics)
        }

        val timed = applicable.map { extractor ->
            val start = System.currentTimeMillis()
            val result = runCatching { extractor.extract(envelope) }
                .getOrElse {
                    LedgerLogger.e("Extractor '${extractor.name}' threw; NotApplicable", it)
                    ExtractionResult.NotApplicable("threw: ${it.message}", extractor.name)
                }
            result to (System.currentTimeMillis() - start)
        }

        // Terminal ignore (OTP/statement/promotion) wins immediately.
        timed.firstOrNull { it.first is ExtractionResult.Ignore }?.let { (res, ms) ->
            val ig = res as ExtractionResult.Ignore
            diagnostics += ExtractionDiagnostics(
                extractor = ig.extractorName,
                decision = ExtractionDiagnostics.DECISION_IGNORED,
                category = ig.category,
                durationMs = ms,
                confidence = ig.confidence,
                validationPassed = false,
                rejectedReason = ig.reason,
                negativeEvidence = ig.matchedPhrases,
                reasoning = listOf(ig.reason),
            )
            emit(diagnostics)
            return ExtractionOutcome.Ignored(ig.reason, diagnostics)
        }

        timed.firstOrNull { it.first is ExtractionResult.Confirmation }?.let { (res, ms) ->
            val cf = res as ExtractionResult.Confirmation
            diagnostics += ExtractionDiagnostics(
                extractor = cf.extractorName,
                decision = "Confirmation",
                category = "Confirmation",
                durationMs = ms,
                confidence = cf.confidence,
                validationPassed = true,
                negativeEvidence = cf.matchedPhrases,
                reasoning = listOf("Confirmation of an existing payment"),
                detectedIntent = "Confirmation",
                matchedLibraryEntries = cf.matchedPhrases,
            )
            emit(diagnostics)
            return ExtractionOutcome.Confirmation(cf.amountMinor, cf.accountTail, cf.matchedPhrases, diagnostics)
        }

        data class Ranked(val result: ExtractionResult.Extracted, val valid: Boolean)
        val ranked = mutableListOf<Ranked>()

        for ((res, ms) in timed) {
            when (res) {
                is ExtractionResult.Extracted -> {
                    val outcome = validator.validate(res)
                    val valid = outcome is ExtractionValidator.ValidationOutcome.Valid
                    val rejected = (outcome as? ExtractionValidator.ValidationOutcome.Invalid)
                        ?.reasons?.joinToString()
                    diagnostics += ExtractionDiagnostics(
                        extractor = res.extractorName,
                        decision = if (valid) ExtractionDiagnostics.DECISION_EXTRACTED else ExtractionDiagnostics.DECISION_IGNORED,
                        category = "Transaction",
                        durationMs = ms,
                        confidence = res.confidence.value,
                        validationPassed = valid,
                        rejectedReason = rejected,
                        positiveEvidence = res.positiveEvidence,
                        reasoning = res.reasoning,
                        detectedIntent = if (valid) "Transaction" else "Rejected",
                        detectedType = res.candidate.transactionType?.name ?: "",
                        confidenceBreakdown = "score=${res.confidence.value}",
                    )
                    ranked += Ranked(res, valid)
                }
                is ExtractionResult.NotApplicable -> {
                    diagnostics += ExtractionDiagnostics(
                        extractor = res.extractorName,
                        decision = ExtractionDiagnostics.DECISION_NOT_APPLICABLE,
                        category = "Unknown",
                        durationMs = ms,
                        confidence = 0,
                        validationPassed = false,
                        rejectedReason = res.reason,
                    )
                }
                is ExtractionResult.Ignore -> Unit
                is ExtractionResult.Confirmation -> Unit
            }
        }

        val valid = ranked.filter { it.valid }.map { it.result }
            .sortedByDescending { it.confidence.value }
        if (valid.isEmpty()) {
            emit(diagnostics)
            return ExtractionOutcome.Failed("All extractions failed validation.", diagnostics)
        }

        val winner = valid.first()
        val runnerUp = valid.getOrNull(1)
        val contested = runnerUp != null && disagreesOnKeyField(winner, runnerUp)
        val effective = if (contested) {
            (winner.confidence.value - disagreementPenalty).coerceAtLeast(0)
        } else {
            winner.confidence.value
        }

        val recheck = validator.validate(winner.copy(confidence = ExtractionConfidence(effective)))
        emit(diagnostics)
        return when (recheck) {
            is ExtractionValidator.ValidationOutcome.Valid -> {
                LedgerLogger.pipeline("Extraction", "Selected '${winner.extractorName}' conf=$effective")
                ExtractionOutcome.Success(winner.candidate, diagnostics)
            }
            is ExtractionValidator.ValidationOutcome.Invalid ->
                ExtractionOutcome.Failed("Contested below threshold: ${recheck.reasons.joinToString()}", diagnostics)
        }
    }

    private fun emit(diagnostics: List<ExtractionDiagnostics>) {
        diagnostics.forEach {
            LedgerLogger.pipeline(
                "ExtractionDiag",
                "extractor=${it.extractor} decision=${it.decision} category=${it.category} " +
                    "conf=${it.confidence} valid=${it.validationPassed} ms=${it.durationMs}" +
                    (it.rejectedReason?.let { r -> " reason=$r" } ?: "") +
                    (if (it.negativeEvidence.isNotEmpty()) " matched=${it.negativeEvidence.joinToString()}" else ""),
            )
        }
    }

    private fun disagreesOnKeyField(a: ExtractionResult.Extracted, b: ExtractionResult.Extracted): Boolean {
        val amountDiff = a.candidate.amountMinor != null && b.candidate.amountMinor != null &&
            a.candidate.amountMinor != b.candidate.amountMinor
        val typeDiff = a.candidate.transactionType != null && b.candidate.transactionType != null &&
            a.candidate.transactionType != b.candidate.transactionType
        return amountDiff || typeDiff
    }

    sealed interface ExtractionOutcome {
        val diagnostics: List<ExtractionDiagnostics>
        data class Success(val candidate: TransactionCandidate, override val diagnostics: List<ExtractionDiagnostics>) : ExtractionOutcome
        data class Ignored(val reason: String, override val diagnostics: List<ExtractionDiagnostics>) : ExtractionOutcome
        data class Failed(val reason: String, override val diagnostics: List<ExtractionDiagnostics>) : ExtractionOutcome

        data class Confirmation(
            val amountMinor: Long?,
            val accountTail: String?,
            val matchedPhrases: List<String>,
            override val diagnostics: List<ExtractionDiagnostics>,
        ) : ExtractionOutcome
    }
}
