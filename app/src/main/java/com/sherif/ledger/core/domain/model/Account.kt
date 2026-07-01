package com.sherif.ledger.core.domain.model

/**
 * Domain model for a financial account.
 */
data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val balance: Money,
    val accountNumberTail: String?,
    val bankBrandId: Long?
)
