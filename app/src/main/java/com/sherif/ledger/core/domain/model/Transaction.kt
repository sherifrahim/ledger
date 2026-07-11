package com.sherif.ledger.core.domain.model

import java.time.Instant

/**
 * Domain model for a financial transaction.
 */
data class Transaction(
    val id: Long,
    val accountId: Long,
    val brandId: Long?,
    val categoryId: Long?,
    val amount: Money,
    val type: TransactionType,
    val timestamp: Instant,
    val source: IngestionSource,
    val rawText: String?,
    val cardTail: String? = null,
    val fingerprint: String,
    // See TransactionCandidate.transferDirection: normalized once upstream, never
    // re-derived downstream.
    val transferDirection: TransferDirection? = null,
    // Provenance evidence for AccountIdentityResolver. See TransactionOrigin.
    val origin: TransactionOrigin? = null,
)


