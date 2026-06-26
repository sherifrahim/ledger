package com.sherif.ledger.core.model

/**
 * User-visible payment rail or instrument used for a transaction.
 */
enum class PaymentMethod {
    Cash,
    DebitCard,
    CreditCard,
    BankTransfer,
    Wallet,
    Unknown,
}
