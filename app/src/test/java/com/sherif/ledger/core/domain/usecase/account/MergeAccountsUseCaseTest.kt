package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountOriginCount
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.repository.TransactionRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MergeAccountsUseCaseTest {

    private class FakeAccountRepository(seed: List<Account>) : AccountRepository {
        val accounts = seed.toMutableList()
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> =
            flowOf(LedgerResult.Success(accounts.filterNot { it.isCandidate }))
        override suspend fun getAccountById(id: Long): LedgerResult<Account> =
            accounts.find { it.id == id }?.let { LedgerResult.Success(it) } ?: LedgerResult.Failure(LedgerError.AccountNotFound)
        override suspend fun insertAccount(account: Account): LedgerResult<Long> {
            val id = (accounts.maxOfOrNull { it.id } ?: 0L) + 1
            accounts += account.copy(id = id)
            return LedgerResult.Success(id)
        }
        override suspend fun updateAccount(account: Account): LedgerResult<Unit> {
            val index = accounts.indexOfFirst { it.id == account.id }
            if (index < 0) return LedgerResult.Failure(LedgerError.AccountNotFound)
            accounts[index] = account
            return LedgerResult.Success(Unit)
        }
        override suspend fun deleteAccount(id: Long): LedgerResult<Unit> {
            val index = accounts.indexOfFirst { it.id == id }
            if (index < 0) return LedgerResult.Failure(LedgerError.AccountNotFound)
            accounts.removeAt(index)
            return LedgerResult.Success(Unit)
        }
        override suspend fun getDeletedAccounts(): LedgerResult<List<Account>> = LedgerResult.Success(emptyList())
        override fun observeCandidateAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(accounts.filter { it.isCandidate }))
    }

    private class FakeTransactionRepository(seed: List<Transaction>) : TransactionRepository {
        val transactions = seed.toMutableList()
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
        override suspend fun reassignAllTransactions(fromAccountId: Long, toAccountId: Long): Int {
            var moved = 0
            for (i in transactions.indices) {
                if (transactions[i].accountId == fromAccountId) {
                    transactions[i] = transactions[i].copy(accountId = toAccountId)
                    moved++
                }
            }
            return moved
        }
    }

    private val runner = object : TransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
    }

    private fun account(
        id: Long, name: String, type: AccountType = AccountType.CHECKING,
        currency: CurrencyCode = CurrencyCode.AED, openingMinor: Long = 0L,
        tail: String? = null, isDefault: Boolean = false,
    ) = Account(id, name, type, Money(openingMinor, currency), tail, null, isDefault = isDefault)

    private fun txn(id: Long, accountId: Long) = Transaction(
        id = id, accountId = accountId, brandId = null, categoryId = null,
        amount = Money(1000L, CurrencyCode.AED), type = TransactionType.EXPENSE,
        timestamp = Instant.now(), source = IngestionSource.SMS, rawText = "txn-$id",
        fingerprint = "fp-$id",
    )

    @Test fun `moves every transaction from the merged account onto the kept one`() = runBlocking {
        val accounts = FakeAccountRepository(listOf(account(1, "ADCB Account"), account(2, "ADCB Account 2")))
        val transactions = FakeTransactionRepository(listOf(txn(10, 2), txn(11, 2), txn(12, 1)))
        val useCase = MergeAccountsUseCase(accounts, transactions, runner)

        val result = useCase.execute(keepAccountId = 1, mergeAccountId = 2)

        assertTrue(result is MergeAccountsResult.Success)
        assertEquals(2, (result as MergeAccountsResult.Success).transactionsMoved)
        assertTrue(transactions.transactions.all { it.accountId == 1L })
    }

    @Test fun `combines opening balances so the replayed total does not silently drop money`() = runBlocking {
        val accounts = FakeAccountRepository(listOf(
            account(1, "ADCB Account", openingMinor = 100_000L),
            account(2, "ADCB Account 2", openingMinor = 50_000L),
        ))
        val transactions = FakeTransactionRepository(emptyList())
        val useCase = MergeAccountsUseCase(accounts, transactions, runner)

        useCase.execute(keepAccountId = 1, mergeAccountId = 2)

        val survivor = accounts.accounts.single { it.id == 1L }
        assertEquals(150_000L, survivor.openingBalance.minorUnits)
    }

    @Test fun `backfills the survivors tail from the merged account when it has none`() = runBlocking {
        val accounts = FakeAccountRepository(listOf(
            account(1, "ADCB Account"),
            account(2, "ADCB Account 2", tail = "920001"),
        ))
        val useCase = MergeAccountsUseCase(accounts, FakeTransactionRepository(emptyList()), runner)

        useCase.execute(keepAccountId = 1, mergeAccountId = 2)

        assertEquals("920001", accounts.accounts.single { it.id == 1L }.accountNumberTail)
    }

    @Test fun `the merged account is removed after a successful merge`() = runBlocking {
        val accounts = FakeAccountRepository(listOf(account(1, "ADCB Account"), account(2, "ADCB Account 2")))
        val useCase = MergeAccountsUseCase(accounts, FakeTransactionRepository(emptyList()), runner)

        useCase.execute(keepAccountId = 1, mergeAccountId = 2)

        assertTrue(accounts.accounts.none { it.id == 2L })
    }

    @Test fun `the survivor stops being the default account after a merge`() = runBlocking {
        // RC7 Phase B's invariant — the default account is never a real
        // institution's identity — must hold after a merge too, whichever side
        // of the merge used to carry the flag.
        val accounts = FakeAccountRepository(listOf(
            account(1, "Primary Account", isDefault = true),
            account(2, "ADCB Account", tail = "920001"),
        ))
        val useCase = MergeAccountsUseCase(accounts, FakeTransactionRepository(emptyList()), runner)

        useCase.execute(keepAccountId = 1, mergeAccountId = 2)

        assertTrue("The survivor must not remain flagged as the fallback account", !accounts.accounts.single { it.id == 1L }.isDefault)
    }

    @Test fun `refuses to merge accounts of different currencies`() = runBlocking {
        val accounts = FakeAccountRepository(listOf(
            account(1, "ADCB Account", currency = CurrencyCode.AED),
            account(2, "US Account", currency = CurrencyCode.USD),
        ))
        val useCase = MergeAccountsUseCase(accounts, FakeTransactionRepository(emptyList()), runner)

        val result = useCase.execute(keepAccountId = 1, mergeAccountId = 2)

        assertTrue(result is MergeAccountsResult.Failed)
        assertEquals(2, accounts.accounts.size) // nothing touched
    }

    @Test fun `refuses to merge accounts of different types`() = runBlocking {
        val accounts = FakeAccountRepository(listOf(
            account(1, "ADCB Account", type = AccountType.CHECKING),
            account(2, "Mashreq Credit Card", type = AccountType.CREDIT),
        ))
        val useCase = MergeAccountsUseCase(accounts, FakeTransactionRepository(emptyList()), runner)

        val result = useCase.execute(keepAccountId = 1, mergeAccountId = 2)

        assertTrue(result is MergeAccountsResult.Failed)
    }

    @Test fun `refuses to merge an account into itself`() = runBlocking {
        val accounts = FakeAccountRepository(listOf(account(1, "ADCB Account")))
        val useCase = MergeAccountsUseCase(accounts, FakeTransactionRepository(emptyList()), runner)

        val result = useCase.execute(keepAccountId = 1, mergeAccountId = 1)

        assertTrue(result is MergeAccountsResult.Failed)
    }
}
