package com.sherif.ledger.core.domain.model

/**
 * Domain model for a financial account.
 *
 * [openingBalance] is set once at account creation and never mutated afterward —
 * it is NOT the current balance. The current balance is always derived by
 * replaying this account's persisted transactions through
 * [com.sherif.ledger.core.domain.service.transaction.BalanceCalculator], starting
 * from this opening value. See
 * [com.sherif.ledger.core.domain.service.transaction.AccountBalanceService].
 * Nothing displays [openingBalance] directly as "the balance" — doing so would
 * reintroduce a cached number as an independent source of truth.
 */
data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val openingBalance: Money,
    val accountNumberTail: String?,
    val bankBrandId: Long?
)

