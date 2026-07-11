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

