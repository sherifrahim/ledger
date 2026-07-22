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
    val bankBrandId: Long?,
    // RC7 Phase B: true for an account the Account Resolver created for an
    // institution it could not recognize (see AccountIdentityDecision.CANDIDATE).
    // Excluded from observeAllAccounts()/every balance and net-worth figure —
    // the same exclusion mechanism soft-delete already uses — until a user
    // promotes (Developer Console) or dismisses it. Never silently merged into
    // an existing account; never silently included in totals either.
    val isCandidate: Boolean = false,
    // The point in time [openingBalance] is anchored to — "the account held this
    // much as of this instant, before Ledger's captured history begins". Set when
    // the user confirms/corrects their real balance (SeedOpeningBalanceUseCase),
    // to the earliest captured transaction for the account. Null until then (older
    // rows, or an account with no correction yet). Makes the balance explainable:
    // opening (as of date) + captured deltas = current. Never used in the balance
    // arithmetic itself — purely a provenance/anchor date. Kept LAST so positional
    // Account(...) constructions are unaffected. See ADR-0009 follow-up.
    val openingBalanceAsOf: java.time.Instant? = null,
)

