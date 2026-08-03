package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionOrigin
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountOriginCount
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * RC2: detection only, by explicit instruction -- these tests assert the use
 * case correctly IDENTIFIES a split-identity pattern and returns it as data;
 * none of them exercise or expect any merge/reassignment behavior, because
 * this use case must never perform one.
 */
class DetectDuplicateAccountIdentitiesUseCaseTest {

    private class FakeAccountRepository(private val accounts: List<Account>) : AccountRepository {
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(accounts))
        override suspend fun getAccountById(id: Long) = LedgerResult.Success(accounts.first { it.id == id })
        override suspend fun insertAccount(account: Account) = LedgerResult.Success(1L)
        override suspend fun updateAccount(account: Account) = LedgerResult.Success(Unit)
        override suspend fun deleteAccount(id: Long) = LedgerResult.Success(Unit)
        override suspend fun getDeletedAccounts() = LedgerResult.Success(emptyList<Account>())
        override fun observeCandidateAccounts() = kotlinx.coroutines.flow.flowOf(LedgerResult.Success(emptyList<Account>()))
    }

    private class FakeTransactionRepository(
        private val transactions: List<Transaction>,
        private val originCounts: Map<Pair<String, String>, List<AccountOriginCount>>,
    ) : TransactionRepository {
        override fun observeRecentTransactions(limit: Int) = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(transactions))
        override fun observeAllTransactions() = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(transactions))
        override fun observeTransactionsForAccount(accountId: Long) = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(emptyList()))
        override fun observeTransactionsBetween(start: Instant, end: Instant) = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(emptyList()))
        override suspend fun getTransactionById(id: Long) = LedgerResult.Success(transactions.first { it.id == id })
        override suspend fun insertTransaction(transaction: Transaction) = LedgerResult.Success(1L)
        override suspend fun deleteTransaction(id: Long) = LedgerResult.Success(Unit)
        override suspend fun updateNote(id: Long, note: String?) = LedgerResult.Success(Unit)
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String) =
            originCounts[packageName to cardTail] ?: emptyList()
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long) = 0
        override suspend fun reassignUntailedTransactions(fromAccountId: Long, packageName: String, toAccountId: Long) = 0
    }

    private fun txn(id: Long, accountId: Long, packageName: String?, cardTail: String?) = Transaction(
        id = id, accountId = accountId, brandId = null, categoryId = null,
        amount = Money(1000L, CurrencyCode.AED), type = TransactionType.EXPENSE,
        timestamp = Instant.now(), source = IngestionSource.NOTIFICATION,
        rawText = "test", cardTail = cardTail, fingerprint = "dup-fp-$id",
        origin = packageName?.let { TransactionOrigin(it, null) },
    )

    private fun account(id: Long, name: String) =
        Account(id, name, AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null)

    @Test fun `detects a genuine duplicate identity split across two non-default accounts`() = runBlocking {
        val defaultAccount = account(1L, "Primary Account")
        val accountA = account(2L, "ADCB Account")
        val accountB = account(3L, "ADCB Account") // race-condition duplicate
        val accounts = listOf(defaultAccount, accountA, accountB)

        val transactions = listOf(
            txn(1, 2L, "com.adcb.nexgen", "1234"),
            txn(2, 3L, "com.adcb.nexgen", "1234"),
        )
        val originCounts = mapOf(
            ("com.adcb.nexgen" to "1234") to listOf(
                AccountOriginCount(2L, 1),
                AccountOriginCount(3L, 1),
            )
        )

        val useCase = DetectDuplicateAccountIdentitiesUseCase(
            FakeAccountRepository(accounts),
            FakeTransactionRepository(transactions, originCounts),
            EnsureDefaultAccountUseCase(FakeAccountRepository(accounts)),
        )

        val findings = useCase.execute()

        assertEquals(1, findings.size)
        val finding = findings.first()
        assertEquals("com.adcb.nexgen", finding.packageName)
        assertEquals("1234", finding.cardTail)
        assertEquals(setOf(2L, 3L), finding.accountIds.toSet())
    }

    @Test fun `does not flag a single real account as a duplicate`() = runBlocking {
        val defaultAccount = account(1L, "Primary Account")
        val accountA = account(2L, "ADCB Account")
        val accounts = listOf(defaultAccount, accountA)

        val transactions = listOf(txn(1, 2L, "com.adcb.nexgen", "1234"))
        val originCounts = mapOf(("com.adcb.nexgen" to "1234") to listOf(AccountOriginCount(2L, 1)))

        val useCase = DetectDuplicateAccountIdentitiesUseCase(
            FakeAccountRepository(accounts),
            FakeTransactionRepository(transactions, originCounts),
            EnsureDefaultAccountUseCase(FakeAccountRepository(accounts)),
        )

        assertTrue(useCase.execute().isEmpty())
    }

    @Test fun `does not flag the normal transition where early sightings sit on the default account`() = runBlocking {
        // This is the EXPECTED, non-buggy pattern: the first couple of
        // observations fall back to the default account before the identity
        // crosses the auto-creation bar, then later ones bind to the new real
        // account. Same signature on the default account + one real account is
        // normal, not a duplicate.
        val defaultAccount = account(1L, "Primary Account")
        val accountA = account(2L, "ADCB Account")
        val accounts = listOf(defaultAccount, accountA)

        val transactions = listOf(
            txn(1, 1L, "com.adcb.nexgen", "1234"), // early sighting, fell back to default
            txn(2, 2L, "com.adcb.nexgen", "1234"), // later, bound to the real account
        )
        val originCounts = mapOf(
            ("com.adcb.nexgen" to "1234") to listOf(
                AccountOriginCount(1L, 1),
                AccountOriginCount(2L, 1),
            )
        )

        val useCase = DetectDuplicateAccountIdentitiesUseCase(
            FakeAccountRepository(accounts),
            FakeTransactionRepository(transactions, originCounts),
            EnsureDefaultAccountUseCase(FakeAccountRepository(accounts)),
        )

        assertTrue(useCase.execute().isEmpty())
    }
}




