package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * RC7 Phase B/D: dismisses a Candidate Account the user recognizes as noise
 * (e.g. a one-off promotional SMS that happened to look transactional) —
 * soft-deletes it via the SAME mechanism every other account deletion already
 * uses, so its (already currency-isolated, already balance-excluded)
 * transactions remain in the database for audit rather than being destroyed.
 */
class DismissCandidateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend fun execute(accountId: Long): LedgerResult<Unit> = accountRepository.deleteAccount(accountId)
}
