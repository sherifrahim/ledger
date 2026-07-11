package com.sherif.ledger.feature.semantic

import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.feature.capture.extraction.ConfirmationMatcher

/**
 * A tri-state interpretation of confirmation matching, so a confirmation is no
 * longer a binary matched/unmatched outcome:
 *
 *  - ConfirmedMatch — high-confidence tie to a specific transaction.
 *  - LikelyMatch    — a plausible tie that should enrich the timeline but be
 *                     flagged for review rather than asserted.
 *  - Unmatched      — no transaction to attach to. NEVER invents an expense.
 *
 * This lives in the routing layer, NOT inside [ConfirmationMatcher]: the matcher
 * stays a pure matcher (frozen), and the tri-state interpretation of its confidence
 * belongs to intent routing. [ConfirmationInterpreter] maps the matcher's existing
 * binary result + confidence into this band.
 */
sealed interface ConfirmationOutcome {
    data class ConfirmedMatch(val transaction: Transaction, val confidence: Int) : ConfirmationOutcome
    data class LikelyMatch(val transaction: Transaction, val confidence: Int) : ConfirmationOutcome
    data class Unmatched(val reason: String) : ConfirmationOutcome
}

/**
 * Maps the frozen [ConfirmationMatcher.MatchResult] (binary Matched/Unmatched with a
 * confidence score) into the tri-state [ConfirmationOutcome]. A future model could
 * replace this interpretation without changing the matcher.
 */
object ConfirmationInterpreter {

    /** Confidence at/above this is a confirmed match; below it (but matched) is likely. */
    const val CONFIRMED_THRESHOLD = 85

    fun interpret(result: ConfirmationMatcher.MatchResult): ConfirmationOutcome = when (result) {
        is ConfirmationMatcher.MatchResult.Matched ->
            if (result.confidence >= CONFIRMED_THRESHOLD) {
                ConfirmationOutcome.ConfirmedMatch(result.transaction, result.confidence)
            } else {
                ConfirmationOutcome.LikelyMatch(result.transaction, result.confidence)
            }
        is ConfirmationMatcher.MatchResult.Unmatched ->
            ConfirmationOutcome.Unmatched(result.reason)
    }
}

