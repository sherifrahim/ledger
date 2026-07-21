package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.service.account.CandidateAccountNaming
import com.sherif.ledger.core.domain.service.intelligence.DecisionType
import com.sherif.ledger.core.domain.service.intelligence.LearnedDecisionStore
import javax.inject.Inject

/**
 * RC7 Phase B/D: turns a Candidate Account (an unrecognized institution the
 * Account Resolver parked separately, see AccountIdentityDecision.CANDIDATE)
 * into a real, confirmed account — reachable only from Developer Console. A
 * user decides this, not the resolver; nothing is ever auto-promoted.
 * Reuses the existing AccountRepository.updateAccount, no new persistence.
 *
 * RC8 Phase B ("Ledger must learn"): also records a [LearnedDecisionStore]
 * mapping from the raw institution identifier (recovered from the
 * pre-promotion Candidate Account name, see [CandidateAccountNaming]) to the
 * confirmed institution name, so [com.sherif.ledger.core.domain.service.account.DeterministicAccountIdentityResolver]
 * never has to park a second candidate for the same real, already-confirmed
 * bank — deterministic memory before repeating an unresolved state forever.
 */
class PromoteCandidateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val learnedDecisionStore: LearnedDecisionStore,
) {
    suspend fun execute(accountId: Long, newName: String? = null): LedgerResult<Unit> {
        val existing = accountRepository.getAccountById(accountId)
        if (existing !is LedgerResult.Success) {
            return LedgerResult.Failure(LedgerError.AccountNotFound)
        }
        if (!existing.data.isCandidate) {
            // Already confirmed — nothing to do, not an error.
            return LedgerResult.Success(Unit)
        }
        val confirmedName = newName?.takeIf { it.isNotBlank() } ?: existing.data.name
        val promoted = existing.data.copy(isCandidate = false, name = confirmedName)
        val result = accountRepository.updateAccount(promoted)
        if (result is LedgerResult.Success) {
            CandidateAccountNaming.parseRawIdentifier(existing.data.name)?.let { rawIdentifier ->
                learnedDecisionStore.learn(DecisionType.INSTITUTION, rawIdentifier, confirmedName)
            }
        }
        return result
    }
}
