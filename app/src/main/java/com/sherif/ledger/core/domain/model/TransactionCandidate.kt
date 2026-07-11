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
    val transactionType: TransactionType?,
    // Normalized upstream at extraction time (the only place raw text is parsed for
    // this fact). Null for non-TRANSFER types, or when direction could not be
    // determined. Downstream (BalanceCalculator, analytics) consumes this field and
    // never re-derives direction from text.
    val transferDirection: TransferDirection? = null,
    // Provenance evidence for AccountIdentityResolver. See TransactionOrigin.
    val origin: TransactionOrigin? = null,
)


