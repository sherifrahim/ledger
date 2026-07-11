package com.sherif.ledger.core.domain.model

/**
 * Immutable provenance evidence for a transaction: where the notification that
 * produced it actually came from. Deliberately its own value object rather than
 * more top-level nullable fields on [Transaction] — if provenance evidence grows
 * (institution hints, IBAN fragments), it grows inside this type, not as new
 * columns scattered across the transaction model.
 *
 * Persistence-agnostic: [com.sherif.ledger.core.database.entity.TransactionEntity]
 * stores these as plain columns and the mapper reconstructs this object, the same
 * pattern already used for [Money]. This type carries no Room annotations.
 */
data class TransactionOrigin(
    val packageName: String?,
    val senderIdentity: String?,
) {
    companion object {
        val UNKNOWN = TransactionOrigin(packageName = null, senderIdentity = null)
    }
}

