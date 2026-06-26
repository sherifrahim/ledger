package com.sherif.ledger.core.model

import java.util.Locale

/**
 * Strongly typed transaction tag with a canonical normalized value.
 */
@JvmInline
value class TransactionTag(val value: String) {
    init {
        require(value.isNotBlank()) { "Transaction tag cannot be blank." }
    }

    override fun toString(): String = value

    companion object {
        /** Creates a tag by trimming whitespace and normalizing case. */
        fun of(value: String): TransactionTag = TransactionTag(
            value = value.trim().lowercase(Locale.US),
        )
    }
}
