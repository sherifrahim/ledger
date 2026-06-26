package com.sherif.ledger.core.model

/**
 * Strongly typed identifier for a Ledger transaction.
 */
@JvmInline
value class TransactionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Transaction id cannot be blank." }
    }

    override fun toString(): String = value
}
