package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.SplitType
import com.sherif.ledger.core.domain.model.SplitWithShares
import kotlinx.coroutines.flow.Flow

/** How one participant's share was specified. [Auto] is only valid for EQUAL
 *  splits — the repository computes the amount from the participant count. */
sealed interface ShareInput {
    data class Exact(val amountMinor: Long) : ShareInput
    data class Percent(val percentage: Double) : ShareInput
    data object Auto : ShareInput
}

/**
 * Split creation and editing. Completely isolated from Financial Truth: reads a
 * transaction's own amount (to know the total to divide, never duplicating it)
 * but never calls BalanceCalculator, RelationshipEngine, or
 * GetFinancialAnalyticsUseCase, and never writes anything that affects a
 * balance or analytics computation.
 */
interface SplitRepository {
    fun observeSplitForTransaction(transactionId: Long): Flow<LedgerResult<SplitWithShares?>>
    suspend fun getSplitForTransaction(transactionId: Long): LedgerResult<SplitWithShares?>

    /** [participantShares] excludes self — self's portion is always the
     *  implicit remainder (total minus everyone else's shares), never stored.
     *  For EQUAL, every value should be [ShareInput.Auto]. */
    suspend fun createSplit(
        transactionId: Long,
        splitType: SplitType,
        participantShares: Map<String, ShareInput>,
    ): LedgerResult<String>

    /** Adds one participant to an existing split. EQUAL splits recalculate
     *  every non-self share to the new even amount; EXACT/PERCENTAGE require
     *  [share] to be explicit and leave every other share untouched. */
    suspend fun addParticipant(splitId: String, participantId: String, share: ShareInput): LedgerResult<Unit>

    /** Removes one participant. EQUAL splits recalculate the remaining
     *  non-self shares; EXACT/PERCENTAGE leave the rest untouched — removing
     *  someone never implicitly redistributes their amount. */
    suspend fun removeParticipant(splitId: String, participantId: String): LedgerResult<Unit>

    /** Direct edit of one share — EXACT/PERCENTAGE only. EQUAL shares only
     *  change via add/removeParticipant, since an individual EQUAL share
     *  isn't independently meaningful. */
    suspend fun updateShare(shareId: String, share: ShareInput): LedgerResult<Unit>

    suspend fun markSettled(shareId: String, settled: Boolean): LedgerResult<Unit>

    suspend fun deleteSplit(splitId: String): LedgerResult<Unit>

    /** Sum of this participant's unsettled shares, across every split. */
    fun observeOutstandingBalance(participantId: String): Flow<LedgerResult<Long>>
}


