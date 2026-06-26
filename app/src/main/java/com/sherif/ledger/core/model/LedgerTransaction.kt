package com.sherif.ledger.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * Immutable domain transaction used by the Ledger core.
 *
 * This model is pure Kotlin and carries only transaction facts, review state,
 * and metadata. It is not a database entity and has no Android dependencies.
 */
data class LedgerTransaction(
    val id: TransactionId,
    val amount: Money,
    val type: TransactionType,
    val occurredOn: LocalDate,
    val paymentMethod: PaymentMethod,
    val source: TransactionSource,
    val reviewStatus: ReviewStatus,
    val category: Category? = null,
    val merchant: Merchant? = null,
    val tags: TransactionTags = TransactionTags.Empty,
    val metadata: TransactionMetadata,
) {
    init {
        require(amount.minorUnits != 0L) { "Transaction amount cannot be zero." }
    }

    companion object {
        /** Creates metadata for manual transactions from a single timestamp. */
        fun manualMetadata(createdAt: Instant): TransactionMetadata = TransactionMetadata(
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}
