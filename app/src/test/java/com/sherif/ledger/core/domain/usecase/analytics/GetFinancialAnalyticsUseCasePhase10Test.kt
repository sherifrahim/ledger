package com.sherif.ledger.core.domain.usecase.analytics

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountOriginCount
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.account.InstitutionRegistry
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.service.transaction.BalanceCalculator
import com.sherif.ledger.core.domain.service.transaction.FinancialStoryPresenter
import com.sherif.ledger.feature.relationship.RelationshipEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * Phase 10 — V3 Integration & Financial Truth Reconciliation. Regression tests
 * for the new analytics-layer capabilities that replace what used to be
 * presentation-layer computation or fabrication:
 *  - transactionStories(): real explanation + category per transaction, sourced
 *    from RelationshipEngine + Merchant Intelligence, never a UI heuristic.
 *  - computeMonthOverMonthChange(): real comparison from existing data, or null
 *    when not computable — never a fabricated static value.
 *  - FinancialAnalytics.intelligenceSummary: real, ordered facts about what was
 *    found, empty when nothing was — never a static confidence figure.
 */
class GetFinancialAnalyticsUseCasePhase10Test {

    private class FakeTransactionRepository(private val byPeriod: Map<Pair<Instant, Instant>, List<Transaction>>) : TransactionRepository {
        override fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>> = flowOf(LedgerResult.Success(emptyList()))
        override fun observeAllTransactions(): Flow<LedgerResult<List<Transaction>>> = flowOf(LedgerResult.Success(emptyList()))
        override fun observeTransactionsForAccount(accountId: Long): Flow<LedgerResult<List<Transaction>>> = flowOf(LedgerResult.Success(emptyList()))
        override fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>> {
            val match = byPeriod.entries.firstOrNull { (range, _) -> start >= range.first && end <= range.second }
                ?: byPeriod.entries.firstOrNull { it.key.first == start }
            return flowOf(LedgerResult.Success(match?.value ?: emptyList()))
        }
        override suspend fun getTransactionById(id: Long) = throw NotImplementedError()
        override suspend fun insertTransaction(transaction: Transaction) = throw NotImplementedError()
        override suspend fun deleteTransaction(id: Long) = throw NotImplementedError()
        override suspend fun updateNote(id: Long, note: String?) = throw NotImplementedError()
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String): List<AccountOriginCount> = emptyList()
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long) = 0
        override suspend fun reassignUntailedTransactions(fromAccountId: Long, packageName: String, toAccountId: Long) = 0
        override suspend fun reassignAllTransactions(fromAccountId: Long, toAccountId: Long) = 0
    }

    private object FakeUnusedAccountRepository : AccountRepository {
        override fun observeAllAccounts() = throw NotImplementedError()
        override suspend fun getAccountById(id: Long) = throw NotImplementedError()
        override suspend fun insertAccount(account: Account) = throw NotImplementedError()
        override suspend fun updateAccount(account: Account) = throw NotImplementedError()
        override suspend fun deleteAccount(id: Long) = throw NotImplementedError()
        override suspend fun getDeletedAccounts() = throw NotImplementedError()
        override fun observeCandidateAccounts() = throw NotImplementedError()
    }

    /** getAll() IS invoked (LearnedMerchantCategoryStore's init block loads it eagerly); upsert() never invoked here. */
    private object FakeMerchantCategoryOverrideDao : com.sherif.ledger.core.database.dao.MerchantCategoryOverrideDao {
        override suspend fun getAll() = emptyList<com.sherif.ledger.core.database.entity.MerchantCategoryOverrideEntity>()
        override suspend fun upsert(entity: com.sherif.ledger.core.database.entity.MerchantCategoryOverrideEntity) = throw NotImplementedError()
    }

    private fun useCase(byPeriod: Map<Pair<Instant, Instant>, List<Transaction>> = emptyMap()): GetFinancialAnalyticsUseCase {
        val repo = FakeTransactionRepository(byPeriod)
        return GetFinancialAnalyticsUseCase(
            transactionRepository = repo,
            relationshipEngine = RelationshipEngine.default(),
            merchantResolver = com.sherif.ledger.feature.merchant.MerchantResolver(com.sherif.ledger.feature.merchant.MerchantRegistry()),
            accountBalanceService = AccountBalanceService(
                BalanceCalculator(), repo, FakeUnusedAccountRepository, RelationshipEngine.default(), InstitutionRegistry(),
            ),
            storyPresenter = FinancialStoryPresenter(),
            learnedMerchantCategoryStore = com.sherif.ledger.feature.merchant.LearnedMerchantCategoryStore(FakeMerchantCategoryOverrideDao),
        )
    }

    private fun txn(id: Long, amount: Long, type: TransactionType, day: Long, rawText: String) = Transaction(
        id = id, accountId = 1L, brandId = null, categoryId = null,
        amount = Money(amount, CurrencyCode.AED), type = type,
        timestamp = Instant.ofEpochSecond(day * 86_400L), source = IngestionSource.NOTIFICATION,
        rawText = rawText, fingerprint = "p10-fp-$id",
    )

    // ---- transactionStories(): real backend explanation + category ----

    @Test fun `transactionStories gives a real relationship-derived explanation, not a generic type label`() {
        val transactions = listOf(
            txn(1, 5_000, TransactionType.EXPENSE, 1, "AED 50.00 at CARREFOUR"),
            txn(2, 5_000, TransactionType.REFUND, 2, "AED 50.00 refunded from CARREFOUR"),
        )
        val stories = useCase().transactionStories(transactions)
        assertEquals("Refund processed", stories[2]?.explanation)
        assertEquals("GROCERIES", stories[1]?.category)
    }

    @Test fun `transactionStories falls back to a generic label only when no relationship exists`() {
        val transactions = listOf(txn(1, 3_700, TransactionType.EXPENSE, 1, "AED 37.00 spent at CARREFOUR"))
        val stories = useCase().transactionStories(transactions)
        assertEquals("Expense", stories[1]?.explanation)
    }

    @Test fun `transactionStories is empty for an empty transaction list`() {
        assertTrue(useCase().transactionStories(emptyList()).isEmpty())
    }

    // ---- intelligenceSummary: real facts, never fabricated ----

    @Test fun `intelligenceSummary is empty when no relationships are found`() {
        val transactions = listOf(txn(1, 3_700, TransactionType.EXPENSE, 1, "AED 37.00 spent at CARREFOUR"))
        val start = Instant.ofEpochSecond(0)
        val end = Instant.ofEpochSecond(10 * 86_400L)
        val analytics = useCase().compute(transactions, start, end)
        assertTrue(analytics.intelligenceSummary.isEmpty())
    }

    @Test fun `intelligenceSummary reports real counts only for facts that were actually found`() {
        val transactions = listOf(
            txn(1, 5_000, TransactionType.EXPENSE, 1, "AED 50.00 at Amazon"),
            txn(2, 5_000, TransactionType.REFUND, 2, "AED 50.00 refunded from Amazon"),
        )
        val start = Instant.ofEpochSecond(0)
        val end = Instant.ofEpochSecond(10 * 86_400L)
        val analytics = useCase().compute(transactions, start, end)
        assertEquals("1 Financial Story matched", analytics.intelligenceSummary.first())
        assertTrue(analytics.intelligenceSummary.any { it.contains("refund") && it.contains("matched automatically") })
        // No subscription/bill/salary facts exist in this data -- must not appear.
        assertTrue(analytics.intelligenceSummary.none { it.contains("subscription") })
        assertTrue(analytics.intelligenceSummary.none { it.contains("Salary") })
    }

    // ---- computeMonthOverMonthChange(): real comparison, or null, never fabricated ----

    // Built via the system default zone -- matching exactly how production
    // (DashboardViewModel.currentMonthRange) constructs period boundaries -- so
    // this test is correct regardless of which timezone the test JVM runs in.
    // A raw Instant.parse("...Z") UTC string, compared against zone-aware
    // production logic, can silently land on the wrong calendar day depending on
    // the runner's default zone; building both sides through ZonedDateTime avoids
    // that entirely.
    private fun monthStart(year: Int, month: Int): Instant =
        java.time.ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant()

    private fun monthEnd(year: Int, month: Int): Instant {
        val zdt = java.time.ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneId.systemDefault())
        val lastDay = zdt.toLocalDate().lengthOfMonth()
        return java.time.ZonedDateTime.of(year, month, lastDay, 23, 59, 59, 0, ZoneId.systemDefault()).toInstant()
    }

    @Test fun `month-over-month change is null when there is no prior month data`() = runBlocking {
        val currentStart = monthStart(2026, 7)
        val result = useCase(emptyMap()).computeMonthOverMonthChange(50_000L, currentStart)
        assertNull(result)
    }

    @Test fun `month-over-month change is null when the prior month had zero spend`() = runBlocking {
        val currentStart = monthStart(2026, 7)
        val juneStart = monthStart(2026, 6)
        val juneEnd = monthEnd(2026, 6)
        // June has a transaction, but it's INCOME, not spend -- previous netSpend is 0.
        val byPeriod = mapOf((juneStart to juneEnd) to listOf(txn(1, 100_000, TransactionType.INCOME, 1, "salary credited")))
        val result = useCase(byPeriod).computeMonthOverMonthChange(50_000L, currentStart)
        assertNull(result)
    }

    @Test fun `month-over-month change is a real computed percentage when both periods have spend`() = runBlocking {
        val currentStart = monthStart(2026, 7)
        val juneStart = monthStart(2026, 6)
        val juneEnd = monthEnd(2026, 6)
        val byPeriod = mapOf((juneStart to juneEnd) to listOf(txn(1, 10_000, TransactionType.EXPENSE, 1, "spent at Amazon")))
        // Current month spend = 15000, previous = 10000 -> +50%
        val result = useCase(byPeriod).computeMonthOverMonthChange(15_000L, currentStart)
        assertEquals("+50%", result)
    }
}



