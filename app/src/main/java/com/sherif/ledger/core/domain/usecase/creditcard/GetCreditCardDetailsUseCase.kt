package com.sherif.ledger.core.domain.usecase.creditcard

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Everything the Credit Card Manager screen shows for one card, all derived
 * from real captured data — nothing here is a number the user has to type in
 * and keep updated (only [Account.creditLimitMinor] is, and that is entered
 * once, not per-cycle).
 */
data class CreditCardDetails(
    val account: Account,
    /** This account's own captured transactions, most recent first. */
    val transactions: List<Transaction>,
    val currency: CurrencyCode,
    /** What is currently owed — the SAME figure [AccountBalanceService] reports elsewhere. */
    val outstandingMinor: Long,
    /** The card's total limit, when the user has supplied it. */
    val limitMinor: Long?,
    /**
     * Remaining headroom: `limit - outstanding`, when the limit is known.
     * Null rather than a guess when it isn't — same rule [AccountBalanceService]
     * already follows for the outstanding figure itself.
     */
    val availableMinor: Long?,
    /**
     * Spend on this card so far in the current calendar month. Deliberately NOT
     * "this statement cycle" — no bank message this app parses ever states a
     * statement date, so claiming to know one would be a fabricated figure.
     * The calendar month is the same cycle boundary Budgets already uses.
     */
    val monthSpendMinor: Long,
)

class GetCreditCardDetailsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceService: AccountBalanceService,
) {
    suspend fun execute(accountId: Long): CreditCardDetails? {
        val account = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)
            ?.data?.firstOrNull { it.id == accountId } ?: return null

        val transactions = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)
            ?.data?.filter { it.accountId == accountId }?.sortedByDescending { it.timestamp }
            ?: emptyList()

        val currency = account.openingBalance.currencyCode
        val outstanding = accountBalanceService.currentBalance(accountId)?.minorUnits ?: 0L
        val limit = account.creditLimitMinor
        val available = limit?.let { it - outstanding }

        val now = ZonedDateTime.now()
        val monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
        val monthSpend = transactions
            .filter { it.type == TransactionType.EXPENSE && !it.timestamp.isBefore(monthStart) }
            .sumOf { it.amount.minorUnits }

        return CreditCardDetails(
            account = account,
            transactions = transactions,
            currency = currency,
            outstandingMinor = outstanding,
            limitMinor = limit,
            availableMinor = available,
            monthSpendMinor = monthSpend,
        )
    }
}
