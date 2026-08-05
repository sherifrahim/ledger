package com.sherif.ledger.core.domain.usecase.intelligence

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.merchantOrRawText
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.domain.FalsePositiveReviewContext
import com.sherif.ledger.feature.ai.orchestrator.AIOrchestrator
import com.sherif.ledger.feature.ai.orchestrator.AIOrchestratorResult
import com.sherif.ledger.feature.ai.settings.AiSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional AI second opinion on a capture the DETERMINISTIC pipeline already
 * flagged as uncertain — see the two call sites in
 * [com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase]:
 * an event persisted at fallback-tier confidence (<90, see
 * [com.sherif.ledger.feature.semantic.DeterministicFinancialIntentClassifier])
 * or bound to an unrecognized-institution Candidate Account. This never runs
 * against an ordinary, confidently-classified capture (a plain "AED 200
 * debited from ADCB" never reaches this class) — the deterministic engine's
 * own uncertainty is the gate, matching [FalsePositiveReviewContext]'s own
 * doc: AI resolves an already-open question, never overrides a confident one.
 *
 * Real user testing (2026-08-05): promotional/loyalty SMS and a BNPL
 * provider's own "payment received" receipt both cleared the deterministic
 * extraction bar and became real, visible transactions — two DIFFERENT root
 * causes, each independently fixed at the deterministic layer once found
 * (SenderClassifier discard; DeterministicFinancialIntentClassifier phrase
 * gap). This class is the general-purpose net for the NEXT such case that
 * hard-coded phrase/sender lists have not been taught yet — "context
 * understanding" the user explicitly asked for, not a replacement for fixing
 * root causes deterministically when one is found.
 *
 * No-op unless [AiSettingsRepository.isAiEnabled] (default off — the user's
 * own API key, opted in). A confident "this is not a real transaction"
 * verdict removes the just-inserted row; anything else (AI disabled, no
 * provider, request failure, low confidence, or "yes it's real") leaves the
 * transaction exactly as the deterministic engine already persisted it —
 * this class only ever REMOVES a low-trust capture, never invents, edits, or
 * reclassifies one, and every removal is logged (see [LedgerLogger.pipeline])
 * so it stays auditable from Pipeline/Ledger Diagnostics.
 */
@Singleton
class AiFalsePositiveGuardUseCase @Inject constructor(
    private val aiOrchestrator: AIOrchestrator,
    private val aiSettingsRepository: AiSettingsRepository,
    private val transactionRepository: TransactionRepository,
) {
    data class Report(val checked: Boolean, val removed: Boolean, val reason: String?)

    companion object {
        /** Below this, "not a real transaction" is treated as inconclusive — never acted on. */
        const val REMOVE_CONFIDENCE_THRESHOLD = 85
    }

    suspend fun review(
        transaction: Transaction,
        senderIdentifier: String,
        deterministicReasoning: List<String>,
    ): Report {
        if (!aiSettingsRepository.isAiEnabled.first()) return Report(checked = false, removed = false, reason = null)

        val context = FalsePositiveReviewContext(
            rawMessageText = transaction.rawText ?: transaction.merchantOrRawText ?: "",
            amountMinor = transaction.amount.minorUnits,
            currencyCode = transaction.amount.currencyCode.name,
            merchant = transaction.merchantOrRawText,
            transactionType = transaction.type.name,
            senderIdentifier = senderIdentifier,
            deterministicReasoning = deterministicReasoning,
        )

        val result = aiOrchestrator.requestSuggestion(AICapability.FALSE_POSITIVE_REVIEW, context)
        val suggestion = (result as? AIOrchestratorResult.Suggested)?.suggestion
            ?: return Report(checked = true, removed = false, reason = null)

        val isReal = suggestion.fields["isRealTransaction"]?.equals("true", ignoreCase = true) ?: true
        if (isReal || suggestion.confidencePercent < REMOVE_CONFIDENCE_THRESHOLD) {
            return Report(checked = true, removed = false, reason = suggestion.reason)
        }

        val deleted = transactionRepository.deleteTransaction(transaction.id)
        val removed = deleted is com.sherif.ledger.core.domain.model.LedgerResult.Success
        if (removed) {
            LedgerLogger.pipeline(
                "AiFalsePositiveGuard",
                "Removed txn #${transaction.id} (conf=${suggestion.confidencePercent}): ${suggestion.reason}",
            )
        }
        return Report(checked = true, removed = removed, reason = suggestion.reason)
    }
}
