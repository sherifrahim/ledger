package com.sherif.ledger.core.domain.service.split

import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Owns the single rule for how a split's total divides into shares. Mirrors
 * [com.sherif.ledger.core.domain.service.transaction.BalanceCalculator]'s role:
 * one place this arithmetic is defined, everything else (the repository) calls
 * it rather than reimplementing it.
 *
 * EQUAL: every non-self participant gets floor(total / participantCount),
 * where participantCount includes self. The self participant implicitly
 * absorbs whatever remainder that floor division leaves — since self's portion
 * is never stored as an owed share, there is nothing to distribute unevenly
 * among the tracked participants; every one of them gets the exact same amount.
 *
 * EXACT: the caller already has the amount — this only validates it.
 *
 * PERCENTAGE: amount = round(total * percentage / 100).
 */
class SplitCalculator @Inject constructor() {

    /**
     * The equal per-participant share. [totalParticipantCount] must include the
     * self participant — the caller is responsible for creating SplitShare rows
     * for every participant EXCEPT self using this same value.
     */
    fun equalShare(totalMinor: Long, totalParticipantCount: Int): Long {
        require(totalParticipantCount > 0) { "totalParticipantCount must be positive" }
        return totalMinor / totalParticipantCount
    }

    fun percentageShare(totalMinor: Long, percentage: Double): Long {
        require(percentage in 0.0..100.0) { "percentage must be within 0..100, was $percentage" }
        return (totalMinor * percentage / 100.0).roundToLong()
    }

    /** True when the given non-self shares (their sum) do not exceed the total —
     *  the one invariant this app enforces for EXACT and PERCENTAGE splits: you
     *  can allocate less than the total (self covers the rest) but never more. */
    fun isWithinTotal(shareAmountsMinor: List<Long>, totalMinor: Long): Boolean =
        shareAmountsMinor.sum() <= totalMinor

    fun isWithinTotalPercentage(percentages: List<Double>): Boolean =
        percentages.sum() <= 100.0
}

