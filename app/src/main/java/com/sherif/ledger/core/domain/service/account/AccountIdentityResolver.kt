package com.sherif.ledger.core.domain.service.account

import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope

/**
 * How [AccountIdentityResolver] arrived at an account for a transaction.
 */
enum class AccountIdentityDecision {
    /** Bound to an existing, already-established account with high confidence. */
    BOUND_EXISTING,

    /** A new account was created — only ever reached via near-certainty or
     *  repeated independent observations, never a single moderate-confidence
     *  signal. */
    CREATED_NEW,

    /** Evidence was insufficient. The transaction is recorded against the default
     *  account. This is a visible, queryable state, not a silent guess. */
    FALLBACK_DEFAULT,

    /** RC7 Phase B: the institution itself was not recognized by
     *  [InstitutionRegistry] at all — never merged into the default account or
     *  any other existing account, regardless of currency. Recorded instead
     *  against a dedicated Candidate Account (Account.isCandidate = true),
     *  correctly currency-tagged from the transaction's own extracted
     *  currency, excluded from every balance/net-worth figure until a user
     *  promotes or dismisses it in Developer Console. This is what closes the
     *  class of bug behind the confirmed HDFC Bank currency-mixing incident
     *  (RC6) at the source, for any future unrecognized institution too. */
    CANDIDATE,
}

/**
 * The outcome of resolving which logical account a transaction belongs to, with
 * the evidence that produced it — so the decision is explainable, not a black box.
 */
data class AccountIdentityResult(
    val accountId: Long,
    val decision: AccountIdentityDecision,
    val confidence: Int,
    val inferredType: AccountType?,
    val evidence: List<String>,
)

/**
 * Determines which logical account a transaction belongs to from multiple
 * deterministic signals (institution, account/card tail, currency, semantic
 * wording, historical bindings). Behind an interface so a future implementation
 * (e.g. model-assisted) can be substituted without any downstream code —
 * insertion, balance computation, analytics — changing at all. The current
 * implementation is entirely deterministic; see
 * [DeterministicAccountIdentityResolver].
 *
 * Never creates an account from a single moderate-confidence signal. Auto-creation
 * requires either near-certainty in one observation or several independent
 * observations of the same identity that each individually clear a real bar.
 * Below that, the transaction falls back to the default account rather than
 * guessing — an unresolved identity is a visible state, not an invented one.
 */
interface AccountIdentityResolver {
    suspend fun resolve(
        envelope: NotificationEnvelope,
        candidate: TransactionCandidate,
    ): AccountIdentityResult
}

