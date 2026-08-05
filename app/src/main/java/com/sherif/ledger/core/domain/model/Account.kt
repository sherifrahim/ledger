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
    /**
     * The card's TOTAL credit limit, for a liability account.
     *
     * Supplied by the user once per card, because no purchase message ever states
     * it — the bank only ever quotes what REMAINS. Paired with the most recent
     * remaining figure it gives the outstanding balance exactly:
     * `outstanding = creditLimit - availableCredit`. Null for a non-credit account,
     * and for a card whose limit hasn't been given yet, in which case the balance
     * falls back to replaying captured transactions.
     */
    val creditLimitMinor: Long? = null,
    // The point in time [openingBalance] is anchored to — "the account held this
    // much as of this instant, before Ledger's captured history begins". Set when
    // the user confirms/corrects their real balance (SeedOpeningBalanceUseCase),
    // to the earliest captured transaction for the account. Null until then (older
    // rows, or an account with no correction yet). Makes the balance explainable:
    // opening (as of date) + captured deltas = current. Never used in the balance
    // arithmetic itself — purely a provenance/anchor date. Kept LAST so positional
    // Account(...) constructions are unaffected. See ADR-0009 follow-up.
    val openingBalanceAsOf: java.time.Instant? = null,
    /**
     * True for exactly the one account [com.sherif.ledger.core.domain.usecase.account.EnsureDefaultAccountUseCase]
     * created (or, for an install that already existed when this flag was
     * introduced, the one account already playing that role).
     *
     * Before this flag existed, "the default account" was not a property of any
     * row — it was `accounts.first().id`, whatever account happened to occupy
     * that position. A real, recognised institution's account could end up there
     * by nothing more than creation order, which is exactly the shape of a
     * confirmed bug: the owner's real ADCB balance was seeded onto an untailed
     * "Primary Account" while the real ADCB transactions accumulated on a
     * separate, later-created account, because the untailed account happened to
     * be first. [com.sherif.ledger.core.domain.service.account.DeterministicAccountIdentityResolver]
     * deliberately refuses to bind an unrecognised institution's transaction to
     * the default account — so the fallback and a real bank account must be two
     * different rows, never the same one by coincidence. This flag is what makes
     * that a guarantee instead of an accident of insert order: an account created
     * for a recognised institution is never marked default, and the default
     * account is never adopted as a real institution's account.
     */
    val isDefault: Boolean = false,
)

