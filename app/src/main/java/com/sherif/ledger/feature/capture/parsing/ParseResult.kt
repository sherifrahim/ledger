package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.core.domain.model.TransactionCandidate

/**
 * Result of a notification parsing attempt.
 */
sealed interface ParseResult {
    /** Successful extraction of a transaction. */
    data class Success(val candidate: TransactionCandidate) : ParseResult

    /** Notification was identified but intentionally ignored (e.g. OTP, Statement). */
    data object Ignore : ParseResult

    /** Parsing failed or no matching pattern was found. */
    data class Failed(val reason: String) : ParseResult
}
