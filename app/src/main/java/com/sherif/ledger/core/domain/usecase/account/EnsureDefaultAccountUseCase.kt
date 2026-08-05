package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Returns the id of THE fallback account — the one every unresolved capture
 * lands on — creating it once if it doesn't exist yet.
 *
 * Used to return `accounts.first().id`: whatever account happened to occupy
 * that position, decided purely by insert order. A real, recognised
 * institution's account could end up there by nothing more than coincidence,
 * which is a confirmed bug, not a hypothetical one — on a real device the
 * owner's real ADCB balance was seeded onto an untailed "Primary Account"
 * while the real ADCB transactions accumulated on a separate, later-created
 * account, because the untailed one happened to be created first.
 * [com.sherif.ledger.core.domain.service.account.DeterministicAccountIdentityResolver]
 * deliberately refuses to bind an unrecognised institution's transaction to the
 * default account — the fallback and a real bank account are meant to be two
 * different rows, never the same one by accident of ordering. [Account.isDefault]
 * is what makes that a guarantee: it is set on exactly one account, only here,
 * never derived from where a row happens to sit — a resolver creating a real
 * institution's account never sets it.
 *
 * @Singleton + an internal [Mutex] for the same reason
 * [com.sherif.ledger.core.domain.service.account.DeterministicAccountIdentityResolver]
 * has one: this is a read-then-maybe-insert sequence with no database-level
 * uniqueness constraint stopping two concurrent callers from both seeing "no
 * default yet" and both creating one. That resolver's own mutex already
 * serializes its calls into here, but this class has other, unprotected
 * callers (the debug-only Institution Diagnostics and duplicate-account
 * detectors) — owning the lock here makes "exactly one default account" hold
 * regardless of caller, rather than depending on every caller remembering to
 * synchronize externally.
 */
@Singleton
class EnsureDefaultAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    private val mutex = Mutex()

    suspend fun execute(): Long = mutex.withLock { executeLocked() }

    private suspend fun executeLocked(): Long {
        val existing = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data.orEmpty()
        existing.firstOrNull { it.isDefault }?.let { return it.id }

        val defaultAccount = Account(
            id = 0,
            name = "Primary Account",
            type = AccountType.CHECKING,
            openingBalance = Money.zero(CurrencyCode.AED),
            accountNumberTail = null,
            bankBrandId = null,
            isDefault = true,
        )

        val result = accountRepository.insertAccount(defaultAccount)
        return if (result is LedgerResult.Success) {
            result.data
        } else {
            // Preserves the pre-existing "never crash the capture pipeline over
            // this" posture: an unattributable capture landing on the wrong
            // account is recoverable (reassignable later); a crash on live
            // notification/SMS processing is not. 1L is the same historical
            // fallback constant this class always had for this branch.
            existing.firstOrNull()?.id ?: 1L
        }
    }
}
