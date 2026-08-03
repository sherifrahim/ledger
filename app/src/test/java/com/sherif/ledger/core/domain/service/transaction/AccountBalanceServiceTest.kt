package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionOrigin
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import com.sherif.ledger.core.domain.repository.AccountOriginCount
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.account.InstitutionRegistry
import com.sherif.ledger.feature.relationship.RelationshipEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AccountBalanceServiceTest {

    private class FakeAccountRepository(private val accounts: List<Account>) : AccountRepository {
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(accounts))
        override suspend fun getAccountById(id: Long) = LedgerResult.Success(accounts.first { it.id == id })
        override suspend fun insertAccount(account: Account) = LedgerResult.Success(1L)
        override suspend fun updateAccount(account: Account) = LedgerResult.Success(Unit)
        override suspend fun deleteAccount(id: Long) = LedgerResult.Success(Unit)
        override suspend fun getDeletedAccounts() = LedgerResult.Success(emptyList<Account>())
        override fun observeCandidateAccounts() = kotlinx.coroutines.flow.flowOf(LedgerResult.Success(emptyList<Account>()))
    }

    private class FakeTransactionRepository(private val transactions: List<Transaction>) : TransactionRepository {
        override fun observeRecentTransactions(limit: Int) = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(transactions))
        override fun observeAllTransactions() = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(transactions))
        override fun observeTransactionsForAccount(accountId: Long) =
            flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(transactions.filter { it.accountId == accountId }))
        override fun observeTransactionsBetween(start: Instant, end: Instant) = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(transactions))
        override suspend fun getTransactionById(id: Long) = LedgerResult.Success(transactions.first { it.id == id })
        override suspend fun insertTransaction(transaction: Transaction) = LedgerResult.Success(1L)
        override suspend fun deleteTransaction(id: Long) = LedgerResult.Success(Unit)
        override suspend fun updateNote(id: Long, note: String?) = LedgerResult.Success(Unit)
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String): List<AccountOriginCount> = emptyList()
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long) = 0
        override suspend fun reassignUntailedTransactions(fromAccountId: Long, packageName: String, toAccountId: Long) = 0
    }

    private fun txn(
        id: Long, accountId: Long, amount: Long, type: TransactionType, day: Long, rawText: String,
        direction: TransferDirection? = null, cardTail: String? = null, packageName: String? = null,
        availableCreditMinor: Long? = null,
    ) = Transaction(
        id = id, accountId = accountId, brandId = null, categoryId = null,
        amount = Money(amount, CurrencyCode.AED), type = type,
        timestamp = Instant.ofEpochSecond(day * 86_400L), source = IngestionSource.NOTIFICATION,
        rawText = rawText, cardTail = cardTail, fingerprint = "fp-$id",
        availableCreditMinor = availableCreditMinor,
        transferDirection = direction,
        origin = packageName?.let { TransactionOrigin(it, null) },
    )

    private fun service(accounts: List<Account>, transactions: List<Transaction>) = AccountBalanceService(
        BalanceCalculator(),
        FakeTransactionRepository(transactions),
        FakeAccountRepository(accounts),
        RelationshipEngine.default(),
        InstitutionRegistry(),
    )

    @Test fun `checking account balance is opening plus income minus expense`() = runBlocking {
        val checking = Account(10L, "ADCB Account", AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null)
        val transactions = listOf(
            txn(1, 10, 600_000, TransactionType.INCOME, 1, "salary credited"),
            txn(2, 10, 5_000, TransactionType.EXPENSE, 2, "spent at Carrefour"),
        )
        val balances = service(listOf(checking), transactions).currentBalances()
        assertEquals(595_000L, balances.first().balance.minorUnits)
    }

    @Test fun `credit account balance increases with a purchase and decreases with a refund`() = runBlocking {
        val credit = Account(20L, "FAB Credit Card", AccountType.CREDIT, Money.zero(CurrencyCode.AED), null, null)
        val transactions = listOf(
            txn(1, 20, 5_000, TransactionType.EXPENSE, 1, "spent at Amazon"),
            txn(2, 20, 1_000, TransactionType.REFUND, 2, "refunded from Amazon"),
        )
        val balances = service(listOf(credit), transactions).currentBalances()
        // Owed increases by 5000 (purchase), decreases by 1000 (refund) = 4000 owed.
        assertEquals(4_000L, balances.first().balance.minorUnits)
    }

    @Test fun `outgoing transfer decreases a checking balance`() = runBlocking {
        val checking = Account(10L, "ADCB Account", AccountType.CHECKING, Money(600_000L, CurrencyCode.AED), null, null)
        val transactions = listOf(
            txn(1, 10, 20_000, TransactionType.TRANSFER, 1, "paid towards FAB credit card", TransferDirection.OUTGOING, cardTail = "6989", packageName = "com.adcb.nexgen"),
        )
        val balances = service(listOf(checking), transactions).currentBalances()
        assertEquals(580_000L, balances.first().balance.minorUnits)
    }

    @Test fun `a credit-card payment recorded on the paying account also reduces the matched liability account`() = runBlocking {
        // This is the dual-effect: ONE transaction row, persisted only against the
        // paying (checking) account -- the liability account's balance still
        // correctly reflects the payment, derived via RelationshipEngine +
        // AccountMatching, never via a stored cross-reference field.
        val checking = Account(10L, "ADCB Account", AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null)
        val credit = Account(20L, "FAB Credit Card", AccountType.CREDIT, Money.zero(CurrencyCode.AED), "6989", null)
        val transactions = listOf(
            // Prior purchase on the credit card: owed = 5000.
            txn(1, 20, 5_000, TransactionType.EXPENSE, 1, "spent using FAB card ending 6989"),
            // Payment recorded ONLY on the checking account -- typed EXPENSE (as
            // GenericBankParser would, matching real production behavior), with
            // card-payment wording so CreditCardPaymentResolver recognizes it, and
            // carrying the FAB package + tail so AccountMatching can find the target.
            txn(2, 10, 2_000, TransactionType.EXPENSE, 2, "AED 20.00 paid towards your FAB credit card", cardTail = "6989", packageName = "com.fab.personalbanking"),
        )
        val balances = service(listOf(checking, credit), transactions).currentBalances()
        val checkingBalance = balances.first { it.account.id == 10L }.balance.minorUnits
        val creditBalance = balances.first { it.account.id == 20L }.balance.minorUnits

        assertEquals("Checking decreases by the payment", -2_000L, checkingBalance)
        assertEquals("Liability decreases by the SAME payment: 5000 owed - 2000 paid = 3000", 3_000L, creditBalance)
    }

    @Test fun `a card-payment-worded transaction already on the liability account is never double-counted`() = runBlocking {
        // RC3 regression: confirmed live (not hypothetical) — AccountMatching
        // previously had no check excluding a payment already persisted ON the
        // account being evaluated. A card-payment-shaped transaction recorded
        // directly on its own credit account (e.g. the card issuer's own
        // confirmation slipping through as an event) had its effect applied
        // twice: once via its own effect(), again via the dual-effect loop
        // matching it back to the same account it's already on.
        val credit = Account(20L, "FAB Credit Card", AccountType.CREDIT, Money.zero(CurrencyCode.AED), "6989", null)
        val transactions = listOf(
            // Prior purchase: owed = 5000.
            txn(1, 20, 5_000, TransactionType.EXPENSE, 1, "spent using FAB card ending 6989"),
            // Card-payment-worded EXPENSE persisted directly ON the credit
            // account itself (accountId=20), carrying its OWN tail+package —
            // the self-referencing shape the fix targets.
            txn(2, 20, 2_000, TransactionType.EXPENSE, 2, "AED 20.00 paid towards your FAB credit card", cardTail = "6989", packageName = "com.fab.personalbanking"),
        )
        val balances = service(listOf(credit), transactions).currentBalances()
        // Before the fix this asserted 5000 (7000 direct, minus an erroneous
        // -2000 self-match) and the test genuinely failed against the
        // unfixed code — verified by hand before writing this assertion.
        assertEquals(7_000L, balances.first().balance.minorUnits)
    }

    @Test fun `net worth is assets minus liabilities using the same replayed balances`() = runBlocking {
        val checking = Account(10L, "ADCB Account", AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null)
        val credit = Account(20L, "FAB Credit Card", AccountType.CREDIT, Money.zero(CurrencyCode.AED), null, null)
        val transactions = listOf(
            txn(1, 10, 600_000, TransactionType.INCOME, 1, "salary credited"),
            txn(2, 20, 500_000, TransactionType.EXPENSE, 2, "purchase on card"),
        )
        val svc = service(listOf(checking, credit), transactions)
        val netWorth = svc.netWorth()
        // Assets: 600000. Liabilities: 500000 owed. Net worth: 100000.
        assertEquals(100_000L, netWorth.minorUnits)
    }

    // ---- Credit-card outstanding, from the owner's real Mashreq card ----

    @Test
    fun `a card's outstanding is its limit minus the bank's latest stated headroom`() = runBlocking {
        // Real figures: the last message on Mashreq ···1959 stated "Available limit:
        // AED 8,115.13". Against a 10,000 limit the card owes 1,884.87 — regardless
        // of how much of its history Ledger happened to capture.
        val card = Account(
            1L, "Mashreq Credit Card", AccountType.CREDIT, Money.zero(CurrencyCode.AED),
            "1959", null, creditLimitMinor = 1_000_000L,
        )
        val txns = listOf(
            txn(1L, 1L, 3_000L, TransactionType.EXPENSE, 1, "purchase", availableCreditMinor = 850_000L),
            txn(2L, 1L, 1_900L, TransactionType.EXPENSE, 2, "purchase", availableCreditMinor = 811_513L),
        )

        val balance = service(listOf(card), txns).currentBalances().single().balance

        assertEquals(188_487L, balance.minorUnits)
    }

    @Test
    fun `paying the card is picked up without matching the payment to anything`() = runBlocking {
        // The point of the whole design. No payment transaction is present at all —
        // only a later purchase whose message quotes a HIGHER remaining limit,
        // exactly as the owner's card did on 9 Jul (+1,184.00). The outstanding
        // figure follows the bank without Ledger identifying a payment.
        val card = Account(
            1L, "Mashreq Credit Card", AccountType.CREDIT, Money.zero(CurrencyCode.AED),
            "1959", null, creditLimitMinor = 1_000_000L,
        )
        val before = listOf(txn(1L, 1L, 1_600L, TransactionType.EXPENSE, 1, "purchase", availableCreditMinor = 754_465L))
        val after = before + txn(2L, 1L, 1_600L, TransactionType.EXPENSE, 2, "purchase", availableCreditMinor = 872_865L)

        assertEquals(245_535L, service(listOf(card), before).currentBalances().single().balance.minorUnits)
        assertEquals(127_135L, service(listOf(card), after).currentBalances().single().balance.minorUnits)
    }

    @Test
    fun `without a limit the card still replays, exactly as before`() = runBlocking {
        // The fallback must be untouched: a card whose limit the user has not given
        // behaves identically to how it always did.
        val card = Account(1L, "Card", AccountType.CREDIT, Money.zero(CurrencyCode.AED), "1959", null)
        val txns = listOf(txn(1L, 1L, 5_000L, TransactionType.EXPENSE, 1, "purchase", availableCreditMinor = 811_513L))

        assertEquals(5_000L, service(listOf(card), txns).currentBalances().single().balance.minorUnits)
    }

    @Test
    fun `a limit with no stated headroom anywhere falls back to replay`() = runBlocking {
        val card = Account(
            1L, "Card", AccountType.CREDIT, Money.zero(CurrencyCode.AED), "1959", null,
            creditLimitMinor = 1_000_000L,
        )
        val txns = listOf(txn(1L, 1L, 5_000L, TransactionType.EXPENSE, 1, "purchase"))

        assertEquals(5_000L, service(listOf(card), txns).currentBalances().single().balance.minorUnits)
    }

    @Test
    fun `a limit on a checking account is ignored`() = runBlocking {
        // Only a liability has an outstanding balance. A stray limit on a current
        // account must never turn its balance into a subtraction.
        val checking = Account(
            1L, "Current", AccountType.CHECKING, Money(100_000L, CurrencyCode.AED), null, null,
            creditLimitMinor = 1_000_000L,
        )
        val txns = listOf(txn(1L, 1L, 5_000L, TransactionType.EXPENSE, 1, "purchase", availableCreditMinor = 811_513L))

        assertEquals(95_000L, service(listOf(checking), txns).currentBalances().single().balance.minorUnits)
    }
}
