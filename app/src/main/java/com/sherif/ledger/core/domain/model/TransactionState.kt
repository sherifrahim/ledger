package com.sherif.ledger.core.domain.model

/**
 * Represents the lifecycle state of a transaction.
 * 
 * Future-ready for pending/posted support without changing persistence schema yet.
 */
enum class TransactionState {
    PENDING,
    POSTED,
    CANCELLED,
    REFUNDED,
    REVERSED
}
