package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.Transaction
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Heuristic confirmation matching — the TEMPORARY stand-in until the deferred
 * Evidence Model (ADR-0002) exists.
 *
 * Given a Confirmation outcome and the transactions the pipeline ALREADY fetched
 * for reconciliation, decides whether the confirmation refers to one of them.
 * Suggested match: same amount, within 24 hours, transfer/expense intent, and
 * account/card hints when available.
 *
 * This class never queries the database itself (the caller supplies the
 * transactions), never inserts, never modifies reconciliation. Its output is
 * diagnostics-only: matched (confirmation of an existing transaction — drop the
 * message) or unmatched (log and drop; never invent an expense).
 */
class ConfirmationMatcher @Inject constructor() {

    private val window: Duration = Duration.ofHours(24)

    sealed interface MatchResult {
        data class Matched(val transaction: Transaction, val confidence: Int) : MatchResult
        data class Unmatched(val reason: String) : MatchResult
    }

    fun match(
        amountMinor: Long?,
        accountTail: String?,
        confirmationTime: Instant,
        existingTransactions: List<Transaction>,
    ): MatchResult {
        if (amountMinor == null) {
            return MatchResult.Unmatched("confirmation carried no amount")
        }

        val candidates = existingTransactions.filter { txn ->
            txn.amount.minorUnits == amountMinor &&
                Duration.between(txn.timestamp, confirmationTime).abs() <= window
        }

        if (candidates.isEmpty()) {
            return MatchResult.Unmatched(
                "no transaction with amount $amountMinor within ${window.toHours()}h",
            )
        }

        // Prefer a tail match when the confirmation carries one; otherwise the
        // closest-in-time amount match.
        val best = candidates
            .sortedBy { Duration.between(it.timestamp, confirmationTime).abs() }
            .let { sorted ->
                if (accountTail != null) {
                    sorted.firstOrNull { it.cardTail == accountTail } ?: sorted.first()
                } else {
                    sorted.first()
                }
            }

        val tailMatched = accountTail != null && best.cardTail == accountTail
        val confidence = when {
            tailMatched -> 97
            accountTail == null -> 90
            else -> 80 // amount+time match but tails differ
        }

        LedgerLogger.pipeline(
            "Confirmation",
            "Matched txn #${best.id} amount=$amountMinor tailMatch=$tailMatched conf=$confidence",
        )
        return MatchResult.Matched(best, confidence)
    }
}
