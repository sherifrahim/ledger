package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.account.AccountMatching
import com.sherif.ledger.core.domain.service.account.InstitutionRegistry
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
            var minorUnits = account.openingBalance.minorUnits
            for (transaction in byAccount[account.id].orEmpty()) {
                minorUnits += balanceCalculator.effect(transaction, account.type)
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

    suspend fun currentBalance(accountId: Long): Money? =
        currentBalances().firstOrNull { it.account.id == accountId }?.balance

    /** Assets - Liabilities, using the SAME replayed balances above. */
    suspend fun netWorth(): Money {
        val balances = currentBalances()
        val currency = balances.firstOrNull()?.balance?.currencyCode
            ?: com.sherif.ledger.core.domain.model.CurrencyCode.AED
        val assets = balances.filter { !it.account.type.isLiability }.sumOf { it.balance.minorUnits }
        val liabilities = balances.filter { it.account.type.isLiability }.sumOf { it.balance.minorUnits }
        return Money(assets - liabilities, currency)
    }
}



