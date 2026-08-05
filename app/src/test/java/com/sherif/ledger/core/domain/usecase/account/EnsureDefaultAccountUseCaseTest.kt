package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.AccountRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnsureDefaultAccountUseCaseTest {

    private class FakeAccountRepository(seed: List<Account> = emptyList()) : AccountRepository {
        val accounts = seed.toMutableList()
        var insertDelayMs: Long = 0

        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(accounts.toList()))
        override suspend fun getAccountById(id: Long): LedgerResult<Account> =
            accounts.find { it.id == id }?.let { LedgerResult.Success(it) }
                ?: LedgerResult.Failure(com.sherif.ledger.core.domain.model.LedgerError.AccountNotFound)
        override suspend fun insertAccount(account: Account): LedgerResult<Long> {
            if (insertDelayMs > 0) kotlinx.coroutines.delay(insertDelayMs)
            val id = (accounts.maxOfOrNull { it.id } ?: 0L) + 1
            accounts += account.copy(id = id)
            return LedgerResult.Success(id)
        }
        override suspend fun updateAccount(account: Account): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun deleteAccount(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun getDeletedAccounts(): LedgerResult<List<Account>> = LedgerResult.Success(emptyList())
        override fun observeCandidateAccounts(): Flow<LedgerResult<List<Account>>> =
            flowOf(LedgerResult.Success(accounts.filter { it.isCandidate }))
    }

    private fun account(id: Long, name: String, isDefault: Boolean = false) =
        Account(id, name, AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null, isDefault = isDefault)

    @Test
    fun `with no accounts, one is created and marked default`() = runBlocking {
        val repo = FakeAccountRepository()
        val useCase = EnsureDefaultAccountUseCase(repo)

        val id = useCase.execute()

        val created = repo.accounts.single { it.id == id }
        assertTrue(created.isDefault)
    }

    @Test
    fun `the account marked default is returned, never a real account by position`() = runBlocking {
        // This is the exact bug: id 1 is a REAL recognised institution's account
        // (ADCB), created before the actual fallback account. The old
        // implementation returned accounts.first().id here — id 1 — which would
        // silently turn a real bank account into the catch-all for every
        // unrecognised capture. The default must be found by its flag, not by
        // whichever row happens to sit first.
        val repo = FakeAccountRepository(
            seed = listOf(
                account(1L, "ADCB Account"),
                account(2L, "Primary Account", isDefault = true),
            ),
        )
        val useCase = EnsureDefaultAccountUseCase(repo)

        val id = useCase.execute()

        assertEquals(2L, id)
    }

    @Test
    fun `an existing default account is never duplicated`() = runBlocking {
        val repo = FakeAccountRepository(seed = listOf(account(5L, "Primary Account", isDefault = true)))
        val useCase = EnsureDefaultAccountUseCase(repo)

        useCase.execute()
        useCase.execute()
        useCase.execute()

        assertEquals(1, repo.accounts.count { it.isDefault })
        assertEquals(1, repo.accounts.size)
    }

    @Test
    fun `concurrent callers with no default account converge on exactly one`() = runBlocking {
        // Same race shape DeterministicAccountIdentityResolver's own mutex guards
        // against: without serializing here too, every unprotected caller (the
        // debug-only diagnostics screens) could see "no default yet" at the same
        // time and each create one.
        val repo = FakeAccountRepository()
        repo.insertDelayMs = 20
        val useCase = EnsureDefaultAccountUseCase(repo)

        val ids = (1..5).map { async { useCase.execute() } }.map { it.await() }

        assertEquals("all callers must agree on one account", 1, ids.distinct().size)
        assertEquals("exactly one default account may ever be created", 1, repo.accounts.count { it.isDefault })
    }
}
