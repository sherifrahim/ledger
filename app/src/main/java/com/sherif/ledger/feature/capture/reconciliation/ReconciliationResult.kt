package com.sherif.ledger.feature.capture.reconciliation

import com.sherif.ledger.core.domain.model.TransactionCandidate

/**
 * Result of the reconciliation process.
 */
sealed interface ReconciliationResult {
    /** Candidate is a new unique transaction. */
    data class New(val candidate: TransactionCandidate) : ReconciliationResult

    /** Candidate is a direct duplicate of an existing transaction (e.g. re-sent notification). */
    data class Duplicate(val existingTransactionId: Long) : ReconciliationResult

    /** Candidate represents an update to an existing transaction (e.g. Pending -> Posted). */
    data class Updated(val existingTransactionId: Long, val candidate: TransactionCandidate) : ReconciliationResult

    /** Candidate should be ignored (e.g. system correction already handled). */
    data object Ignored : ReconciliationResult
}
