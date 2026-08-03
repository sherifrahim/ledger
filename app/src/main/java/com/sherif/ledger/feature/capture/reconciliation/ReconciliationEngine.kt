package com.sherif.ledger.feature.capture.reconciliation

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.isOutflowOf
import com.sherif.ledger.core.domain.service.transaction.FingerprintGenerator
import com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase
import java.time.Duration
import javax.inject.Inject

/**
 * Engine responsible for reconciling transaction candidates against existing data.
 * Implements multi-signal confidence scoring to detect duplicates and updates.
 *
 * RC1 stabilization: amount, account/card tail, and temporal proximity now
 * outweigh exact merchant wording — merchant text is supporting evidence, not
 * the primary identity signal. This fixes real duplicate insertion when the
 * same bank event arrives through independent channels (SMS + push
 * notification) with slightly different extracted merchant text.
 *
 * No longer checks candidate.accountId — extraction never populates it
 * (account identity is resolved separately, after reconciliation, by
 * AccountIdentityResolver), so that check was always a no-op.
 */
class ReconciliationEngine @Inject constructor(
    private val fingerprintGenerator: FingerprintGenerator
) {
    companion object {
        /**
         * How far apart two records of the same real-world event can be. Independent
         * channels mirror one another in seconds (the observed cross-channel pairs on
         * the owner's device were 6s, 41s, 85s and 91s apart); five minutes is
         * generous headroom without reaching into genuinely separate purchases.
         */
        const val SAME_EVENT_WINDOW_MINUTES = 5L
    }

    /**
     * Reconciles a candidate against a list of nearby existing transactions.
     */
    fun reconcile(candidate: TransactionCandidate, existingTransactions: List<Transaction>): ReconciliationResult {
        val candidateFingerprint = generateFingerprint(candidate)
        LedgerLogger.pipeline("Reconciliation", "Fingerprint: $candidateFingerprint")
        
        // 1. Exact Fingerprint Match (100% confidence)
        val exactMatch = existingTransactions.find { it.fingerprint == candidateFingerprint }
        if (exactMatch != null) {
            LedgerLogger.pipeline("Reconciliation", "Exact match found (Fingerprint)")
            return ReconciliationResult.Duplicate(exactMatch.id)
        }

        // 2. Structural same-event match. Independent of the scoring below and
        // deliberately ahead of it: this is the "(amount, ~timestamp, card tail)"
        // identity of a real-world money movement, which does not depend on how
        // any particular channel worded the message.
        val sameEvent = existingTransactions.firstOrNull { describesSameEvent(candidate, it) }
        if (sameEvent != null) {
            LedgerLogger.pipeline(
                "Reconciliation",
                "Same-event match to #${sameEvent.id} (amount + <=${SAME_EVENT_WINDOW_MINUTES}min + compatible tail/direction)",
            )
            return ReconciliationResult.Duplicate(sameEvent.id)
        }

        // 3. Fuzzy Matching Signals
        // RC3: log every comparison, not only the winner — if an unrelated
        // transaction is scoring high enough to cause a false Duplicate/Updated
        // match (e.g. two genuinely separate transfers sharing the same ACCOUNT
        // tail, which — unlike a card tail — is identical across every message
        // from that account), this makes it directly visible instead of silent.
        val scored = existingTransactions.map { existing ->
            calculateConfidenceWithDetails(candidate, existing) to existing
        }
        scored.forEach { (score, existing) ->
            LedgerLogger.d(
                "Reconciliation: candidate vs existing#${existing.id} " +
                    "(rawText='${existing.rawText}', tail=${existing.cardTail}, amount=${existing.amount.minorUnits}) " +
                    "-> score=${score.score} [${score.details}]"
            )
        }
        val matches = scored
            .filter { it.first.score >= 90 } // Min threshold for reconciliation
            .sortedByDescending { it.first.score }

        val bestMatch = matches.firstOrNull()

        return when {
            bestMatch == null -> {
                LedgerLogger.pipeline("Reconciliation", "No match found. Score < 90")
                ReconciliationResult.New(candidate)
            }
            
            // Logic for Updated vs Duplicate (e.g. amount change or status change)
            bestMatch.first.score >= 98 -> {
                LedgerLogger.pipeline("Reconciliation", "Match Score: ${bestMatch.first.score}. Reason: ${bestMatch.first.details}")
                ReconciliationResult.Duplicate(bestMatch.second.id)
            }
            
            else -> {
                LedgerLogger.pipeline("Reconciliation", "Match Score: ${bestMatch.first.score}. Reason: ${bestMatch.first.details}")
                ReconciliationResult.Updated(bestMatch.second.id, candidate)
            }
        }
    }

    /** RC9 Phase C: exposed (was private) so Developer Console diagnostics can show duplicate-detection reasoning. Never consumed by [reconcile] logic itself — read-only. */
    data class ScoreResult(val score: Int, val details: String)

    /**
     * Whether [candidate] and [existing] describe the SAME real-world money
     * movement, judged only on structure — never on wording.
     *
     * This exists because the confidence scoring below cannot reach its own
     * threshold for the most common duplicate shape in real captured data. Verified
     * on the owner's device: one AED 3,000.00 ATM withdrawal produced two rows 85
     * seconds apart from the same ADCB sender — one carrying the account tail and
     * the merchant "Atm-index Exc Hamdaan", the other carrying no tail and no
     * merchant at all ("Unknown"). It scored amount 40 + time 20 + type 10 = 70,
     * under the 90 threshold, so both were persisted, and because account identity
     * is resolved afterwards they landed on two DIFFERENT accounts — one purchase
     * counted twice, on two accounts that could never reconcile against each other.
     *
     * The four conditions are all necessary, and each rejects a real non-duplicate
     * found in that same database:
     *  - **Same amount and currency** — the structural identity of the movement.
     *  - **Within [SAME_EVENT_WINDOW_MINUTES]** — channels mirror each other in
     *    seconds to a couple of minutes, not hours.
     *  - **Compatible tails** — equal, or at least one side did not quote one. Two
     *    DIFFERENT tails mean two different accounts and therefore two different
     *    events: an AED 900.00 pair with tails 920001 and 000001 is a transfer
     *    between the owner's own accounts, and merging it would erase one leg.
     *  - **Same direction of travel** — an inflow never duplicates an outflow, for
     *    the same reason. Uses the shared [isOutflowOf] rule, not a local copy, so
     *    a merged row can never render with the opposite sign to the one it
     *    absorbed.
     *
     * Note this is strictly narrower in TIME than the scoring path it precedes:
     * matching merchant text already merges at up to 30 minutes today. So this adds
     * no duplicate-merging risk that the engine did not already accept — it only
     * stops requiring the two channels to have chosen the same words.
     */
    private fun describesSameEvent(candidate: TransactionCandidate, existing: Transaction): Boolean {
        val amount = candidate.amountMinor ?: return false
        if (candidate.currencyCode != existing.amount.currencyCode) return false
        if (amount != existing.amount.minorUnits) return false

        val drift = Duration.between(candidate.timestamp, existing.timestamp).abs()
        if (drift > Duration.ofMinutes(SAME_EVENT_WINDOW_MINUTES)) return false

        val candidateTail = candidate.accountHint
        val existingTail = existing.cardTail
        if (candidateTail != null && existingTail != null && candidateTail != existingTail) return false

        return isOutflowOf(candidate.transactionType, candidate.transferDirection) ==
            isOutflowOf(existing.type, existing.transferDirection)
    }

    /**
     * RC9 Phase C — Explainability. Purely additive, read-only: reproduces
     * the EXACT SAME scoring [reconcile] already computes internally, just
     * returns the per-candidate breakdown instead of discarding it after
     * picking a winner. Never used by [reconcile] itself, never changes a
     * score or a threshold — this is diagnostics visibility for the
     * Intelligence Inspector, not new duplicate-detection logic.
     */
    fun explainScoring(candidate: TransactionCandidate, existingTransactions: List<Transaction>): List<Pair<Transaction, ScoreResult>> =
        existingTransactions.map { it to calculateConfidenceWithDetails(candidate, it) }
            .sortedByDescending { it.second.score }

    /**
     * XDR-inspired: amount, account/card tail, and temporal proximity are the
     * primary identity signals for "is this the same real-world event" —
     * merchant text is supporting evidence, never the sole determinant. This
     * matters because Ledger captures the same bank event through independent
     * channels (push notification and SMS), which can extract slightly
     * different merchant wording for what is genuinely one transaction.
     *
     * Merchant match and tail match are INDEPENDENT paths to high confidence —
     * either alone (combined with amount + time + type) can reach the
     * reconciliation threshold, since most captured messages have a merchant
     * string but not all have a parseable tail. A mismatch on either is
     * recorded but never zeroes the score outright; only amount and currency
     * mismatches are absolute, structural rejects.
     *
     * Known trade-off, accepted deliberately: two genuinely different
     * transactions on the same card, for the same amount, within a minute of
     * each other, can score high enough to be merged. This is judged rarer and
     * less harmful than the duplicate-insertion bug this fixes.
     */
    private fun calculateConfidenceWithDetails(candidate: TransactionCandidate, existing: Transaction): ScoreResult {
        if (candidate.currencyCode != existing.amount.currencyCode) return ScoreResult(0, "Currency mismatch")
        if (candidate.amountMinor != existing.amount.minorUnits) return ScoreResult(0, "Amount mismatch")

        var score = 40 // amount already confirmed equal above
        val details = mutableListOf("Amount: 40")

        if (candidate.merchantName != null && candidate.merchantName == existing.rawText) {
            score += 30
            details.add("Merchant: 30")
        }

        val tailMatch = candidate.accountHint != null && existing.cardTail != null && candidate.accountHint == existing.cardTail
        if (tailMatch) {
            score += 30
            details.add("Tail: 30")
        }

        val timeDrift = Duration.between(candidate.timestamp, existing.timestamp).abs().toMinutes()
        val timeScore = when {
            timeDrift <= 1 -> 20
            timeDrift <= 5 -> 15
            timeDrift <= 30 -> 10
            timeDrift <= 1440 -> 5
            else -> 0
        }
        if (timeScore > 0) {
            score += timeScore
            details.add("Time: $timeScore")
        }

        if (candidate.transactionType == existing.type) {
            score += 10
            details.add("Type: 10")
        }

        return ScoreResult(score.coerceIn(0, 100), details.joinToString(", "))
    }

    private fun generateFingerprint(candidate: TransactionCandidate): String {
        // Helper to map Candidate to UseCase Params for fingerprinting
        val params = InsertTransactionUseCase.Params(
            accountId = candidate.accountId ?: 0L,
            amountMinor = candidate.amountMinor ?: 0L,
            currencyCode = candidate.currencyCode ?: com.sherif.ledger.core.domain.model.CurrencyCode.AED,
            type = candidate.transactionType ?: com.sherif.ledger.core.domain.model.TransactionType.EXPENSE,
            timestamp = candidate.timestamp,
            source = candidate.source,
            rawMerchantText = candidate.merchantName ?: ""
        )
        return fingerprintGenerator.generate(params)
    }
}





