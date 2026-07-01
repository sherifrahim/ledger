package com.sherif.ledger.core.domain.model

/**
 * Common error hierarchy for Ledger domain operations.
 */
sealed interface LedgerError {
    data object DatabaseFailure : LedgerError
    data object DuplicateTransaction : LedgerError
    data object InvalidCurrency : LedgerError
    data object AccountNotFound : LedgerError
    data object CategoryNotFound : LedgerError
    data class Unknown(val message: String) : LedgerError
}
