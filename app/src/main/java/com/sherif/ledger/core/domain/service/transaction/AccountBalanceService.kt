package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.account.AccountMatching
import com.sherif.ledger.core.domain.service.account.InstitutionRegistry
import com.sherif.ledger.feature.capture.parsing.extraction.ExtractionHelpers
import com.sherif.ledger.feature.relationship.RelationshipEngine
import com.sherif.ledger.feature.relationship.RelationshipType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** An account paired with its replay-derived current balance. */
data class AccountBalance(val account: Account, val balance: Money)

/**
 * The ONLY source of current-balance truth. No number shown to the user is ever
 * read from a cached/stored field — every balance is reproduced by replaying
 * persisted transactions through [BalanceCalculator]'s single effect rule,
 * starting from each account's [Account.openingBalance].
 *
 * [GetFinancialAnalyticsUseCase] consumes this service's output; it never
 * recomputes balance arithmetic itself. This keeps balance computation as its own
 * domain service, per the standing architectural rule for this codebase.
 *
 * Runs [RelationshipEngine.analyze] exactly ONCE across all transactions, shared
 * by every account's derivation below — not once per account. This is what
 * identifies credit-card-payment relationships so a liability account's balance
 * correctly reflects payments recorded against a different (paying) account,
 * without ever persisting a cross-reference field.
 */
class AccountBalanceService @Inject constructor(
    private val balanceCalculator: BalanceCalculator,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val relationshipEngine: RelationshipEngine,
    private val institutionRegistry: InstitutionRegistry,
) {

    suspend fun currentBalances(): List<AccountBalance> {
        val accounts = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data
            ?: return emptyList()
        val allTransactions = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)?.data
            ?: emptyList()

        val byAccount = allTransactions.groupBy { it.accountId }
        val txnById = allTransactions.associateBy { it.id }

        // One relationship pass, shared by every account below.
        val creditCardPayments = if (allTransactions.isNotEmpty()) {
            relationshipEngine.analyze(allTransactions).filter { it.type == RelationshipType.CREDIT_CARD_PAYMENT }
        } else emptyList()

        return accounts.map { account ->
            // A credit card's outstanding balance is not a replay problem.
            //
            // Replaying captured purchases can only ever total what Ledger happened
            // to see since the import window opened — on the owner's device that
            // reported AED 23,499.70 of "debt" which was really three months of
            // spending. The bank does this arithmetic itself and restates the
            // result in every message: verified on real data, the stated available
            // limit falls by exactly the purchase amount and jumps back up by the
            // payment when the card is paid. So when the card's total limit is
            // known, outstanding is exact subtraction that self-corrects on the
            // next message — no payment matching, no accumulated drift, and no
            // dependence on having captured every transaction.
            //
            // Falls through to the replay below whenever either half is missing, so
            // a card with no limit set, or one whose bank never quotes the clause,
            // behaves exactly as it always did.
            derivedCardOutstanding(account, byAccount[account.id].orEmpty())?.let { outstanding ->
                return@map AccountBalance(account, Money(outstanding, account.openingBalance.currencyCode))
            }

            var minorUnits = account.openingBalance.minorUnits
            for (transaction in byAccount[account.id].orEmpty()) {
                minorUnits += balanceCalculator.effect(transaction, account.type, account.openingBalance.currencyCode)
            }
            if (account.type.isLiability) {
                for (relationship in creditCardPayments) {
                    val payment = txnById[relationship.sourceTransactionId] ?: continue
                    // RC3: a payment already persisted ON this same account is
                    // not "a payment from elsewhere" — it's already counted via
                    // this account's own effect() loop above. Without this
                    // check, a card issuer's own confirmation message
                    // misrouted onto its own credit account (a live-verified
                    // scenario, not hypothetical) gets its effect applied
                    // twice: once as an ordinary transaction, once again here.
                    if (payment.accountId == account.id) continue
                    val institution = institutionRegistry.resolve(payment.origin?.packageName)
                    val tail = payment.cardTail
                    if (tail != null && AccountMatching.matches(institution, tail, payment.amount.currencyCode, account)) {
                        minorUnits += balanceCalculator.liabilityPaymentEffect(payment.amount)
                    }
                }
            }
            AccountBalance(account, Money(minorUnits, account.openingBalance.currencyCode))
        }
    }

    /**
     * `creditLimit - availableCredit` for a liability account, or null when that
     * cannot be stated honestly.
     *
     * Returns null — never a guess — if the account is not a liability, has no
     * limit recorded, or no message on it has ever quoted a remaining limit. The
     * caller then replays as before, so this is strictly additive.
     *
     * The reading used is the most RECENT one that quoted a figure, since each
     * message supersedes the last. A reading is ignored if its own transaction is
     * in a different currency from the account, for the same reason
     * [BalanceCalculator.effect] refuses to mix units.
     */
    private fun derivedCardOutstanding(account: Account, transactions: List<Transaction>): Long? {
        if (!account.type.isLiability) return null
        val limit = account.creditLimitMinor ?: return null
        val latestAvailable = transactions
            .filter { it.amount.currencyCode == account.openingBalance.currencyCode }
            .sortedByDescending { it.timestamp }
            .firstNotNullOfOrNull { availableCreditOf(it) } ?: return null
        // Positive-as-owed, matching the sign convention every liability balance
        // already uses (see BalanceCalculator.effect, which flips for liabilities).
        return limit - latestAvailable
    }

    /**
     * The bank's stated remaining limit for one message: the stored column, or —
     * for rows written before that column existed — read back out of the captured
     * message, which is still there verbatim.
     *
     * The same shape as [com.sherif.ledger.core.domain.model.merchantOrRawText],
     * and possible for the same reason: extraction stopped overwriting `raw_text`.
     * It means an existing library starts reporting real card balances immediately
     * rather than only after the user re-imports their history.
     */
    private fun availableCreditOf(transaction: Transaction): Long? =
        transaction.availableCreditMinor
            ?: transaction.rawText?.let { ExtractionHelpers.extractAvailableCreditMinor(it) }

    suspend fun currentBalance(accountId: Long): Money? =
        currentBalances().firstOrNull { it.account.id == accountId }?.balance

    /**
     * Assets - Liabilities, using the SAME replayed balances above.
     *
     * RC7 Phase C: previously summed every account's raw minor units
     * regardless of currency, then stamped the result with whichever
     * currency the first account in the list happened to have — a real,
     * confirmed currency-mixing bug of the exact same shape as the one
     * [BalanceCalculator.effect] already guards per-transaction (RC6), just
     * never closed at this aggregation layer. Now uses [CurrencyGuard] to
     * group by currency first: only accounts sharing the dominant ("primary")
     * currency are summed into this figure. Any other-currency account is
     * excluded here (see [nonPrimaryCurrencyBalances]) rather than silently
     * corrupting the total — no exchange-rate conversion is performed
     * anywhere in this codebase.
     */
    suspend fun netWorth(): Money {
        val balances = currentBalances()
        val grouped = CurrencyGuard.groupAndSum(
            items = balances,
            currencyOf = { it.balance.currencyCode },
            amountOf = { if (it.account.type.isLiability) -it.balance.minorUnits else it.balance.minorUnits },
        )
        return Money(grouped.primaryTotalMinor, grouped.primaryCurrency)
    }

    /**
     * RC7 Phase D: candidate accounts are deliberately absent from
     * [currentBalances] (via [AccountRepository.observeAllAccounts] now
     * excluding them) so they never appear in ordinary balance/net-worth
     * figures. Developer Console still needs to show what's actually
     * accumulating in each one before a user decides to promote or dismiss
     * it — this replays the SAME [BalanceCalculator.effect] rule, scoped to
     * candidates only. Deliberately skips the credit-card cross-account
     * liability adjustment [currentBalances] applies to confirmed liability
     * accounts — an unpromoted candidate has no established relationship to
     * cross-reference yet.
     */
    suspend fun candidateBalances(): List<AccountBalance> {
        val candidatesResult = accountRepository.observeCandidateAccounts().first()
        val candidates = (candidatesResult as? LedgerResult.Success)?.data ?: return emptyList()
        val allTransactions = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)?.data ?: emptyList()
        val byAccount = allTransactions.groupBy { it.accountId }
        return candidates.map { account ->
            var minorUnits = account.openingBalance.minorUnits
            for (transaction in byAccount[account.id].orEmpty()) {
                minorUnits += balanceCalculator.effect(transaction, account.type, account.openingBalance.currencyCode)
            }
            AccountBalance(account, Money(minorUnits, account.openingBalance.currencyCode))
        }
    }

    /**
     * Every account balance whose currency is NOT the primary currency
     * [netWorth] reports in — real accounts, real balances, deliberately
     * excluded from that single figure rather than mixed into it. Diagnostics
     * only (Balance Inspector v2, Part D: "reason excluded").
     */
    suspend fun nonPrimaryCurrencyBalances(): List<AccountBalance> {
        val balances = currentBalances()
        val grouped = CurrencyGuard.groupAndSum(
            items = balances,
            currencyOf = { it.balance.currencyCode },
            amountOf = { if (it.account.type.isLiability) -it.balance.minorUnits else it.balance.minorUnits },
        )
        return balances.filter { it.balance.currencyCode != grouped.primaryCurrency }
    }
}



