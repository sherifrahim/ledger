package com.sherif.ledger.core.model

/**
 * Strongly typed identifier for a financial account.
 */
@JvmInline
value class AccountId(val value: String) {
    init { require(value.isNotBlank()) { "Account id cannot be blank." } }
    override fun toString(): String = value
}

/**
 * A financial account the user holds (bank account, wallet, card).
 *
 * Each account has a single currency. Cross-currency views are built
 * at the presentation layer by aggregating accounts, never by storing
 * mixed currencies in one account.
 *
 * Relationships:
 * - owns zero or more [LedgerTransaction] instances
 * - carries a [Money] balance derived from those transactions
 * - belongs to exactly one [Currency]
 */
data class Account(
    val id: AccountId,
    val name: String,
    val currency: Currency,
    val balance: Money,
    val isDefault: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Account name cannot be blank." }
        require(balance.currency == currency) { "Account balance currency must match account currency." }
    }
}
