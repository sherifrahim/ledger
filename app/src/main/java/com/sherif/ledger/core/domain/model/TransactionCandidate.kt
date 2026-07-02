package com.sherif.ledger.core.domain.model

import java.time.Instant

/**
 * Immutable domain model representing an unvalidated transaction extracted from an external source.
 * Contains raw information before being processed by the Transaction Pipeline.
 */
data class TransactionCandidate(
    val source: IngestionSource,
    val rawText: String,
    val merchantName: String?,
    val amountMinor: Long?,
    val currencyCode: CurrencyCode?,
    val timestamp: Instant,
    val accountHint: String?, // e.g., last 4 digits of account
    val accountId: Long? = null, // Resolved account ID
    val transactionType: TransactionType?
)
