package com.sherif.ledger.core.model

/**
 * Common result wrapper for pure domain operations that can succeed or fail.
 */
sealed interface LedgerResult<out T> {
    /**
     * Successful domain operation result.
     */
    data class Success<T>(val value: T) : LedgerResult<T>

    /**
     * Failed domain operation result with one or more domain error messages.
     */
    data class Failure(val errors: List<String>) : LedgerResult<Nothing> {
        init {
            require(errors.isNotEmpty()) { "LedgerResult.Failure requires at least one error." }
            require(errors.all { it.isNotBlank() }) { "LedgerResult.Failure errors cannot be blank." }
        }
    }
}
