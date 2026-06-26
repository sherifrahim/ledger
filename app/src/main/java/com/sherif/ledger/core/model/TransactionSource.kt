package com.sherif.ledger.core.model

/**
 * Origin of a transaction record before user review or editing.
 */
enum class TransactionSource {
    Manual,
    Sms,
    Notification,
    Import,
    System,
}
