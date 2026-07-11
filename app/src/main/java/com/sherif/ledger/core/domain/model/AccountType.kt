package com.sherif.ledger.core.domain.model

/**
 * Strongly typed classification for financial accounts.
 */
enum class AccountType {
    CASH,
    SAVINGS,
    CHECKING,
    CREDIT,
    INVESTMENT;

    /**
     * True for account types that represent money OWED rather than money HELD.
     * The single point every balance/net-worth computation consults to decide
     * whether an EXPENSE against this account increases or decreases what it
     * represents. Adding a future liability type (e.g. a loan/financing account)
     * is a one-line change here, not scattered conditionals elsewhere.
     */
    val isLiability: Boolean
        get() = this == CREDIT
}

