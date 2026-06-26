package com.sherif.ledger.core.model

import java.time.Instant

/**
 * Audit metadata for a transaction independent of persistence or UI concerns.
 */
data class TransactionMetadata(
    val createdAt: Instant,
    val updatedAt: Instant,
    val note: String? = null,
    val parserMetadata: ParserMetadata? = null,
) {
    init {
        require(!updatedAt.isBefore(createdAt)) {
            "Transaction updatedAt cannot be before createdAt."
        }
        require(note == null || note.isNotBlank()) { "Transaction note cannot be blank." }
    }
}
