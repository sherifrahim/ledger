package com.sherif.ledger.core.domain.model

/**
 * Domain-level result wrapper for operations that can fail.
 */
sealed interface LedgerResult<out T> {
    data class Success<out T>(val data: T) : LedgerResult<T>
    data class Failure(val error: LedgerError) : LedgerResult<Nothing>
}
