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
    val fingerprint: String
)
