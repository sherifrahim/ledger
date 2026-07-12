package com.sherif.ledger.core.domain.usecase.analytics

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import com.sherif.ledger.feature.relationship.RelationshipEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Phase 8 — Financial Reality Rules (Part 3), made permanent regression assertions.
 * Uses the REAL [RelationshipEngine] (via its default factory, same Merchant
 * Intelligence composition production uses) — not mocks — so these tests exercise
 * exactly what production runs.
 *
 * Every bug fixed in Phase 8 has a corresponding invariant test here.
 */
class FinancialInvariantsTest {

    private val useCase = GetFinancialAnalyticsUseCase(
        transactionRepository = FakeUnusedRepository, // compute() is called directly; execute() is not exercised here
        relationshipEngine = RelationshipEngine.default(),
        merchantResolver = com.sherif.ledger.feature.merchant.MerchantResolver(com.sherif.ledger.feature.merchant.MerchantRegistry()),
        accountBalanceService = com.sherif.ledger.core.domain.service.transaction.AccountBalanceService(
            com.sherif.ledger.core.domain.service.transaction.BalanceCalculator(),
            FakeUnusedRepository,
            FakeUnusedAccountRepository,
            RelationshipEngine.default(),
            com.sherif.ledger.core.domain.service.account.InstitutionRegistry(),
        ), // never exercised: compute() is called directly, computeNetWorth() is not under test here
        storyPresenter = com.sherif.ledger.core.domain.service.transaction.FinancialStoryPresenter(),
    )

    private val periodStart = Instant.ofEpochSecond(0)
    private val periodEnd = Instant.ofEpochSecond(60 * 86_400L)
    private val DAY = 86_400L

    private fun txn(
        id: Long,
        accountId: Long,
        amountMinor: Long,
        type: TransactionType,
        day: Long,
        rawText: String,
        direction: TransferDirection? = null,
    ) = Transaction(
        id = id, accountId = accountId, brandId = null, categoryId = null,
        amount = Money(amountMinor, CurrencyCode.AED), type = type,
        timestamp = Instant.ofEpochSecond(day * DAY), source = IngestionSource.NOTIFICATION,
        rawText = rawText, fingerprint = "fp-$id", transferDirection = direction,
    )

    // ---- "A FinancialEvent changes money. A FinancialConfirmation/Information/
    //      Unknown never changes money." ----
    // This is enforced structurally by Phase 7's routing (confirmations/information
    // never persist), so nothing to assert here at the analytics layer — a
    // FinancialConfirmation literally has no Transaction row to aggregate. Verified
    // by ProcessNotificationUseCaseIntentRoutingTest (Phase 7).

    // ---- "Internal transfers: Account A decreases, Account B increases. Total
    //      wealth remains identical." ----
    @Test fun `internal transfer never appears as spend or income`() {
        val transactions = listOf(
            txn(1, 10, 100_000, TransactionType.TRANSFER, 1, "transferred to savings", TransferDirection.OUTGOING),
            txn(2, 20, 100_000, TransactionType.TRANSFER, 1, "received into savings account", TransferDirection.INCOMING),
        )
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        assertEquals("Transfer must not count as spend", 0L, analytics.netSpendMinor)
        assertEquals("Transfer must not count as income", 0L, analytics.incomeMinor)
    }

    // ---- "Credit-card payment: Debit account decreases, credit-card liability
    //      decreases, never record a second expense." ----
    @Test fun `credit card payment is excluded from spending even though extraction typed it EXPENSE`() {
        val transactions = listOf(
            // The ADCB side: extraction typed this EXPENSE (no transferSignal match),
            // exactly the real production bug this phase audited.
            txn(1, 10, 20_000, TransactionType.EXPENSE, 1, "AED 200.00 paid towards your FAB credit card"),
        )
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        assertEquals("Gross expense still reflects the raw EXPENSE row", 20_000L, analytics.grossExpenseMinor)
        assertEquals("Net spend must exclude the credit-card payment entirely", 0L, analytics.netSpendMinor)
        assertEquals(20_000L, analytics.excludedFromSpendingMinor)
    }

    @Test fun `credit card payment never becomes a second persisted expense`() {
        // The FAB confirmation side is never persisted at all -- Phase 7 routes it
        // to FINANCIAL_CONFIRMATION and blocks insertion before this layer ever
        // sees it (see ProcessNotificationUseCaseIntentRoutingTest). So the ONLY
        // row that can exist for one real card payment is the single ADCB-side
        // debit, worded so the relationship engine's OWN card-payment vocabulary
        // recognizes it. Analytics must show exactly one exclusion, never a
        // doubled or duplicated figure from two rows.
        val transactions = listOf(
            txn(1, 10, 15_000, TransactionType.EXPENSE, 1, "AED 150.00 payment towards your FAB credit card"),
        )
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        assertEquals(0L, analytics.netSpendMinor)
        assertEquals(15_000L, analytics.excludedFromSpendingMinor)
    }

    // ---- "Refund: Original expense decreases." ----
    @Test fun `refund decreases net spend for the matched purchase`() {
        val transactions = listOf(
            txn(1, 10, 5_000, TransactionType.EXPENSE, 1, "AED 50.00 at CARREFOUR"),
            txn(2, 10, 5_000, TransactionType.REFUND, 3, "AED 50.00 refunded from CARREFOUR"),
        )
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        assertEquals("Fully refunded purchase contributes zero net spend", 0L, analytics.netSpendMinor)
        assertEquals(5_000L, analytics.grossExpenseMinor)
        assertEquals(5_000L, analytics.refundedMinor)
    }

    @Test fun `refund with a different amount than the purchase is not netted (exact-amount matching is a frozen constraint)`() {
        // RefundOfPurchaseResolver (frozen, not modified by this phase) matches by
        // EXACT amount. A refund that doesn't exactly match a purchase amount finds
        // no relationship, so it is NOT netted against an unrelated expense — this
        // documents that real, honest limitation rather than asserting a netting
        // capability the engine does not have. The refund still surfaces in the
        // diagnostic refundedMinor total.
        val transactions = listOf(
            txn(1, 10, 10_000, TransactionType.EXPENSE, 1, "AED 100.00 at AMAZON"),
            txn(2, 10, 3_000, TransactionType.REFUND, 2, "AED 30.00 refunded from AMAZON"),
        )
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        assertEquals("No exact-amount match; purchase stays at full value", 10_000L, analytics.netSpendMinor)
        assertEquals("Refund still visible diagnostically", 3_000L, analytics.refundedMinor)
    }

    // ---- "Salary: Income only. Never expense." ----
    @Test fun `salary is income only`() {
        val transactions = listOf(txn(1, 10, 600_000, TransactionType.INCOME, 1, "Your salary AED 6000.00 has been credited"))
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        assertEquals(600_000L, analytics.incomeMinor)
        assertEquals(0L, analytics.netSpendMinor)
        assertEquals(0L, analytics.grossExpenseMinor)
    }

    // ---- "Cash withdrawal: Transfer between cash and account. Not spending by
    //      itself." ----
    @Test fun `cash withdrawal is excluded from spending`() {
        val transactions = listOf(txn(1, 10, 20_000, TransactionType.EXPENSE, 1, "AED 200.00 ATM cash withdrawal"))
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        assertEquals(0L, analytics.netSpendMinor)
        assertEquals(20_000L, analytics.excludedFromSpendingMinor)
    }

    // ---- Genuine purchases must remain real spend (the negative-control case) ----
    @Test fun `genuine purchase is real spend, unaffected by exclusion rules`() {
        val transactions = listOf(txn(1, 10, 3_700, TransactionType.EXPENSE, 1, "AED 37.00 spent at CARREFOUR"))
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        assertEquals(3_700L, analytics.netSpendMinor)
        assertEquals(0L, analytics.excludedFromSpendingMinor)
    }

    // ---- Net worth invariant: gross expense - net spend - excluded should equal
    //      zero (nothing lost or double-subtracted across the categorization) ----
    @Test fun `every EXPENSE-typed minor unit is accounted for as net spend, excluded, or refunded`() {
        val transactions = listOf(
            txn(1, 10, 3_700, TransactionType.EXPENSE, 1, "AED 37.00 spent at CARREFOUR"),
            txn(2, 10, 20_000, TransactionType.EXPENSE, 2, "AED 200.00 ATM cash withdrawal"),
            txn(3, 10, 5_000, TransactionType.EXPENSE, 3, "AED 50.00 at AMAZON"),
            txn(4, 10, 5_000, TransactionType.REFUND, 4, "AED 50.00 refunded from AMAZON"),
        )
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        // gross = net + excluded + refundedAgainstNettedPurchases
        assertEquals(analytics.grossExpenseMinor, analytics.netSpendMinor + analytics.excludedFromSpendingMinor + analytics.refundedMinor)
    }

    // ---- Full realistic chain: exactly what the original bug report described ----
    @Test fun `ADCB debit plus FAB confirmation chain never double counts`() {
        // Per Phase 7, the FAB confirmation message never persists a second
        // transaction at all -- it is intercepted before extraction's output ever
        // reaches this analytics layer. So the only row analytics ever sees for one
        // real card payment is the single ADCB-side debit. This asserts the
        // analytics-layer consequence: that one row is correctly excluded as a
        // credit-card payment (not spend), and nothing about it is doubled.
        val transactions = listOf(
            txn(1, 10, 15_000, TransactionType.EXPENSE, 1, "AED 150.00 payment towards your FAB credit card"),
        )
        val analytics = useCase.compute(transactions, periodStart, periodEnd)
        assertEquals(0L, analytics.netSpendMinor)
        assertTrue("Must not appear as income either", analytics.incomeMinor == 0L)
    }

    // ---- Phase 9 invariants, named exactly as specified, permanent regressions.
    //      The underlying mechanics (BalanceCalculator arithmetic, the
    //      RelationshipEngine-driven dual effect) are verified transaction-by-
    //      transaction in AccountBalanceServiceTest; these assert the invariant
    //      statements themselves hold for representative scenarios. ----

    @Test fun `invariant - Assets minus Liabilities equals Net Worth`() = runBlocking {
        val checking = com.sherif.ledger.core.domain.model.Account(
            10L, "ADCB Account", com.sherif.ledger.core.domain.model.AccountType.CHECKING,
            com.sherif.ledger.core.domain.model.Money.zero(CurrencyCode.AED), null, null,
        )
        val credit = com.sherif.ledger.core.domain.model.Account(
            20L, "FAB Credit Card", com.sherif.ledger.core.domain.model.AccountType.CREDIT,
            com.sherif.ledger.core.domain.model.Money.zero(CurrencyCode.AED), null, null,
        )
        val transactions = listOf(
            netWorthTxn(1, 10, 800_000, TransactionType.INCOME, 1, "salary credited"),
            netWorthTxn(2, 20, 300_000, TransactionType.EXPENSE, 2, "purchase on credit card"),
        )
        val service = com.sherif.ledger.core.domain.service.transaction.AccountBalanceService(
            com.sherif.ledger.core.domain.service.transaction.BalanceCalculator(),
            FakeTransactionsRepository(transactions),
            FakeAccountsRepository(listOf(checking, credit)),
            com.sherif.ledger.feature.relationship.RelationshipEngine.default(),
            com.sherif.ledger.core.domain.service.account.InstitutionRegistry(),
        )
        val balances = service.currentBalances()
        val assets = balances.filter { !it.account.type.isLiability }.sumOf { it.balance.minorUnits }
        val liabilities = balances.filter { it.account.type.isLiability }.sumOf { it.balance.minorUnits }
        val netWorth = service.netWorth()
        assertEquals(assets - liabilities, netWorth.minorUnits)
        assertEquals(500_000L, netWorth.minorUnits) // 800000 - 300000
    }

    @Test fun `invariant - a credit-card payment reduces an Asset account and reduces a Liability account`() = runBlocking {
        val checking = com.sherif.ledger.core.domain.model.Account(
            10L, "ADCB Account", com.sherif.ledger.core.domain.model.AccountType.CHECKING,
            com.sherif.ledger.core.domain.model.Money.zero(CurrencyCode.AED), null, null,
        )
        val credit = com.sherif.ledger.core.domain.model.Account(
            20L, "FAB Credit Card", com.sherif.ledger.core.domain.model.AccountType.CREDIT,
            com.sherif.ledger.core.domain.model.Money.zero(CurrencyCode.AED), "6989", null,
        )
        val transactions = listOf(
            netWorthTxn(1, 20, 500_000, TransactionType.EXPENSE, 1, "spent using FAB card ending 6989"),
            netWorthTxn(
                2, 10, 200_000, TransactionType.EXPENSE, 2,
                "AED 2000.00 paid towards your FAB credit card",
                cardTail = "6989",
                origin = com.sherif.ledger.core.domain.model.TransactionOrigin("com.fab.personalbanking", null),
            ),
        )
        val service = com.sherif.ledger.core.domain.service.transaction.AccountBalanceService(
            com.sherif.ledger.core.domain.service.transaction.BalanceCalculator(),
            FakeTransactionsRepository(transactions),
            FakeAccountsRepository(listOf(checking, credit)),
            com.sherif.ledger.feature.relationship.RelationshipEngine.default(),
            com.sherif.ledger.core.domain.service.account.InstitutionRegistry(),
        )
        val balances = service.currentBalances()
        val checkingBalance = balances.first { it.account.id == 10L }.balance.minorUnits
        val creditBalance = balances.first { it.account.id == 20L }.balance.minorUnits
        assertEquals("Asset (checking) decreased by the payment", -200_000L, checkingBalance)
        assertEquals("Liability (credit) decreased by the same payment: 500000 owed - 200000 = 300000", 300_000L, creditBalance)
    }

    private fun netWorthTxn(
        id: Long, accountId: Long, amount: Long, type: TransactionType, day: Long, rawText: String,
        cardTail: String? = null, origin: com.sherif.ledger.core.domain.model.TransactionOrigin? = null,
    ) = Transaction(
        id = id, accountId = accountId, brandId = null, categoryId = null,
        amount = Money(amount, CurrencyCode.AED), type = type,
        timestamp = Instant.ofEpochSecond(day * 86_400L), source = IngestionSource.NOTIFICATION,
        rawText = rawText, cardTail = cardTail, fingerprint = "inv-fp-$id", origin = origin,
    )

    private class FakeAccountsRepository(
        private val accounts: List<com.sherif.ledger.core.domain.model.Account>,
    ) : com.sherif.ledger.core.domain.repository.AccountRepository {
        override fun observeAllAccounts() = kotlinx.coroutines.flow.flowOf(com.sherif.ledger.core.domain.model.LedgerResult.Success(accounts))
        override suspend fun getAccountById(id: Long) = com.sherif.ledger.core.domain.model.LedgerResult.Success(accounts.first { it.id == id })
        override suspend fun insertAccount(account: com.sherif.ledger.core.domain.model.Account) = com.sherif.ledger.core.domain.model.LedgerResult.Success(1L)
        override suspend fun updateAccount(account: com.sherif.ledger.core.domain.model.Account) = com.sherif.ledger.core.domain.model.LedgerResult.Success(Unit)
        override suspend fun deleteAccount(id: Long) = com.sherif.ledger.core.domain.model.LedgerResult.Success(Unit)
    }

    private class FakeTransactionsRepository(
        private val transactions: List<Transaction>,
    ) : com.sherif.ledger.core.domain.repository.TransactionRepository {
        override fun observeRecentTransactions(limit: Int) = kotlinx.coroutines.flow.flowOf(com.sherif.ledger.core.domain.model.LedgerResult.Success(transactions))
        override fun observeAllTransactions() = kotlinx.coroutines.flow.flowOf(com.sherif.ledger.core.domain.model.LedgerResult.Success(transactions))
        override fun observeTransactionsForAccount(accountId: Long) =
            kotlinx.coroutines.flow.flowOf(com.sherif.ledger.core.domain.model.LedgerResult.Success(transactions.filter { it.accountId == accountId }))
        override fun observeTransactionsBetween(start: Instant, end: Instant) = kotlinx.coroutines.flow.flowOf(com.sherif.ledger.core.domain.model.LedgerResult.Success(transactions))
        override suspend fun getTransactionById(id: Long) = com.sherif.ledger.core.domain.model.LedgerResult.Success(transactions.first { it.id == id })
        override suspend fun insertTransaction(transaction: Transaction) = com.sherif.ledger.core.domain.model.LedgerResult.Success(1L)
        override suspend fun deleteTransaction(id: Long) = com.sherif.ledger.core.domain.model.LedgerResult.Success(Unit)
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String) = emptyList<com.sherif.ledger.core.domain.repository.AccountOriginCount>()
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long) = 0
    }

    /** Never invoked: compute() is exercised directly, execute()'s repository path is not under test here. */
    private object FakeUnusedRepository : com.sherif.ledger.core.domain.repository.TransactionRepository {
        override fun observeRecentTransactions(limit: Int) = throw NotImplementedError()
        override fun observeAllTransactions() = throw NotImplementedError()
        override fun observeTransactionsForAccount(accountId: Long) = throw NotImplementedError()
        override fun observeTransactionsBetween(start: Instant, end: Instant) = throw NotImplementedError()
        override suspend fun getTransactionById(id: Long) = throw NotImplementedError()
        override suspend fun insertTransaction(transaction: Transaction) = throw NotImplementedError()
        override suspend fun deleteTransaction(id: Long) = throw NotImplementedError()
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String) = throw NotImplementedError()
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long) = throw NotImplementedError()
    }

    /** Never invoked: AccountBalanceService is only constructor-required here, never exercised. */
    private object FakeUnusedAccountRepository : com.sherif.ledger.core.domain.repository.AccountRepository {
        override fun observeAllAccounts() = throw NotImplementedError()
        override suspend fun getAccountById(id: Long) = throw NotImplementedError()
        override suspend fun insertAccount(account: com.sherif.ledger.core.domain.model.Account) = throw NotImplementedError()
        override suspend fun updateAccount(account: com.sherif.ledger.core.domain.model.Account) = throw NotImplementedError()
        override suspend fun deleteAccount(id: Long) = throw NotImplementedError()
    }
}



