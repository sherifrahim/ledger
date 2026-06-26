package com.sherif.ledger.core.model

/**
 * Direction of money movement represented by a transaction.
 */
enum class TransactionType {
    Expense,
    Income,
    Transfer,
    Adjustment,
}
