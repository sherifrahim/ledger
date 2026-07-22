package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

/**
 * One-time correction, run only from the historical-import onboarding flow.
 *
 * Every account is created with `openingBalance = 0` (see [com.sherif.ledger.core.domain.model.Account]'s
 * own doc comment, which assumes a real opening value that nothing ever
 * actually sets). That was tolerable when import replayed a user's entire
 * SMS history; a bounded onboarding window (This Month, etc.) means the
 * computed balance is only the net effect of what was imported, not the
 * account's real balance — confirmed via a real diagnostic bundle (AED
 * 8,030.88 shown vs. an actual ~4.3k balance, with `balance_reconstruction`
 * still passing, i.e. the arithmetic was correct given incomplete data).
 *
 * This backs out what `openingBalance` needed to be for the computed
 * balance to equal the real balance the user supplies once, at the end of
 * onboarding, and persists it via [AccountRepository.updateAccount] — which
 * already existed, fully wired, with zero callers before this. This is a
 * deliberate, one-time exception to "opening balance is never mutated
 * afterward": it runs once, before any balance has ever been shown to the
 * user, not as an ongoing correction mechanism.
 */
class SeedOpeningBalanceUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountBalanceService: AccountBalanceService,
    private val transactionRepository: TransactionRepository,
) {
    suspend fun execute(accountId: Long, actualCurrentBalanceMinor: Long): LedgerResult<Unit> {
        val accountResult = accountRepository.getAccountById(accountId)
        val account = (accountResult as? LedgerResult.Success)?.data
            ?: return LedgerResult.Failure(LedgerError.AccountNotFound)

        val computedBalanceMinor = accountBalanceService.currentBalance(accountId)?.minorUnits ?: 0L
        val correction = actualCurrentBalanceMinor - computedBalanceMinor
        val newOpeningBalanceMinor = account.openingBalance.minorUnits + correction

        // Anchor the opening balance to the earliest captured transaction for this
        // account — the boundary before which the opening balance applies, so the
        // figure is explainable ("this much as of <date>"). Keep any existing
        // anchor if the account has no transactions yet.
        val anchor = earliestTransactionInstant(accountId) ?: account.openingBalanceAsOf

        return accountRepository.updateAccount(
            account.copy(
                openingBalance = Money(newOpeningBalanceMinor, account.openingBalance.currencyCode),
                openingBalanceAsOf = anchor,
            ),
        )
    }

    private suspend fun earliestTransactionInstant(accountId: Long): Instant? {
        val all = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)?.data
            ?: return null
        return all.filter { it.accountId == accountId }.minByOrNull { it.timestamp }?.timestamp
    }
}
