package com.sherif.ledger.core.domain.model

import java.time.Instant

/**
 * One participant's portion of a split — never created for the self
 * participant, whose share is always the implicit remainder (total minus the
 * sum of everyone else's shares), so it's never tracked as "owed."
 *
 * [shareAmountMinor] is the single source of truth for what's owed, computed
 * and stored at creation/edit time — a fact, not a live derivation, consistent
 * with how this app treats every other financial record. [percentage] is kept
 * alongside it only for PERCENTAGE-type splits, purely so the amount can be
 * re-displayed as "33%" without recomputing; it plays no role in EQUAL or EXACT
 * splits and is null for both.
 */
data class SplitShare(
    val id: String,
    val splitId: String,
    val participantId: String,
    val shareAmountMinor: Long,
    val percentage: Double? = null,
    val isSettled: Boolean = false,
    val settledAt: Instant? = null,
)


