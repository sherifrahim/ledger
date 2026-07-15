package com.sherif.ledger.core.domain.model

import java.time.Instant

/**
 * A split expense, linked to the transaction it originated from. Never
 * duplicates the transaction's amount — the split's total is always read from
 * [Transaction.amount] at the moment it's needed, never stored separately, so
 * there is no possibility of the two drifting apart.
 *
 * One split per transaction in V1 (enforced by a unique index on
 * transaction_id) — "redo the split" means editing this one's shares, not
 * creating a second split for the same transaction.
 *
 * Completely isolated from Financial Truth: a Split never appears in, and is
 * never consulted by, BalanceCalculator, RelationshipEngine, or
 * GetFinancialAnalyticsUseCase. It tracks who owes whom; it does not adjust
 * what the transaction itself did to any account balance.
 */
data class Split(
    val id: String,
    val transactionId: Long,
    val splitType: SplitType,
    val createdAt: Instant,
    val updatedAt: Instant,
)

