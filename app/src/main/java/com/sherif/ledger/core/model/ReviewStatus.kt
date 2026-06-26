package com.sherif.ledger.core.model

/**
 * Strongly typed review lifecycle state for a transaction.
 */
sealed interface ReviewStatus {
    /**
     * Transaction was created by the user or has already been accepted.
     */
    data object Approved : ReviewStatus

    /**
     * Transaction was created automatically and needs user confirmation.
     */
    data object NeedsReview : ReviewStatus

    /**
     * Transaction was rejected by the user and should not affect ledgers.
     */
    data object Rejected : ReviewStatus
}
