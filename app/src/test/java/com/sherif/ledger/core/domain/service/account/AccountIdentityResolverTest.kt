package com.sherif.ledger.core.domain.service.account

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountOriginCount
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.usecase.account.EnsureDefaultAccountUseCase
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AccountIdentityResolverTest {

    private class FakeAccountRepository : AccountRepository {
        val accounts = mutableListOf(
            Account(1L, "Primary Account", AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null),
        )
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(accounts.toList()))
        override suspend fun getAccountById(id: Long): LedgerResult<Account> =
            accounts.find { it.id == id }?.let { LedgerResult.Success(it) } ?: LedgerResult.Failure(com.sherif.ledger.core.domain.model.LedgerError.AccountNotFound)
        override suspend fun insertAccount(account: Account): LedgerResult<Long> {
            val id = (accounts.maxOfOrNull { it.id } ?: 0L) + 1
            accounts += account.copy(id = id)
            return LedgerResult.Success(id)
        }
        override suspend fun updateAccount(account: Account): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun deleteAccount(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
    }

    private class FakeTransactionRepository : TransactionRepository {
        // signature (packageName, tail) -> accountId -> count
        val originCounts = mutableMapOf<Pair<String, String>, MutableMap<Long, Int>>()
        fun recordFallback(packageName: String, tail: String, accountId: Long) {
            originCounts.getOrPut(packageName to tail) { mutableMapOf() }
                .merge(accountId, 1) { a, b -> a + b }
        }
        override fun observeRecentTransactions(limit: Int) = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(emptyList()))
        override fun observeAllTransactions() = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(emptyList()))
        override fun observeTransactionsForAccount(accountId: Long) = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(emptyList()))
        override fun observeTransactionsBetween(start: Instant, end: Instant) = flowOf<LedgerResult<List<Transaction>>>(LedgerResult.Success(emptyList()))
        override suspend fun getTransactionById(id: Long) = LedgerResult.Failure(com.sherif.ledger.core.domain.model.LedgerError.Unknown(""))
        override suspend fun insertTransaction(transaction: Transaction) = LedgerResult.Success(1L)
        override suspend fun deleteTransaction(id: Long) = LedgerResult.Success(Unit)
        override suspend fun updateNote(id: Long, note: String?) = LedgerResult.Success(Unit)
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String): List<AccountOriginCount> =
            (originCounts[packageName to cardTail] ?: emptyMap()).map { (accId, count) -> AccountOriginCount(accId, count) }
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long) = 0
    }

    private val accountRepository = FakeAccountRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val resolver = DeterministicAccountIdentityResolver(
        InstitutionRegistry(),
        accountRepository,
        transactionRepository,
        EnsureDefaultAccountUseCase(accountRepository),
    )

    private fun envelope(pkg: String) = NotificationEnvelope(pkg, "", "text", null, Instant.now(), "k")

    private fun candidate(rawText: String, tail: String?, type: TransactionType = TransactionType.EXPENSE) = TransactionCandidate(
        source = IngestionSource.NOTIFICATION,
        rawText = rawText,
        merchantName = null,
        amountMinor = 5000L,
        currencyCode = CurrencyCode.AED,
        timestamp = Instant.now(),
        accountHint = tail,
        transactionType = type,
    )

    @Test fun `unknown institution falls back to default with zero confidence`() = runBlocking {
        val result = resolver.resolve(envelope("com.unknown.bank"), candidate("some text", "1234"))
        assertEquals(AccountIdentityDecision.FALLBACK_DEFAULT, result.decision)
        assertEquals(1L, result.accountId)
    }

    @Test fun `known institution with no tail falls back`() = runBlocking {
        val result = resolver.resolve(envelope("com.fab.personalbanking"), candidate("some text", null))
        assertEquals(AccountIdentityDecision.FALLBACK_DEFAULT, result.decision)
    }

    @Test fun `single observation of a checking-style signal is not enough to create an account`() = runBlocking {
        val result = resolver.resolve(envelope("com.adcb.nexgen"), candidate("AED 200 debited from account", "920001"))
        assertEquals(AccountIdentityDecision.FALLBACK_DEFAULT, result.decision)
        assertTrue(result.evidence.any { it.contains("Observation 1/3") })
    }

    @Test fun `credit card payment with explicit type hint creates an account on first sighting`() = runBlocking {
        val result = resolver.resolve(
            envelope("com.fab.personalbanking"),
            candidate("AED 200.00 paid towards your FAB credit card ending 6989", "6989"),
        )
        assertEquals(AccountIdentityDecision.CREATED_NEW, result.decision)
        assertEquals(AccountType.CREDIT, result.inferredType)
        assertTrue(accountRepository.accounts.any { it.name.contains("FAB") && it.type == AccountType.CREDIT })
    }

    @Test fun `three independent observations of the same signature create an account`() = runBlocking {
        val pkg = "com.adcb.nexgen"
        val text = "AED 200 debited from account"
        val tail = "920001"

        val first = resolver.resolve(envelope(pkg), candidate(text, tail))
        assertEquals(AccountIdentityDecision.FALLBACK_DEFAULT, first.decision)
        transactionRepository.recordFallback(pkg, tail, first.accountId)

        val second = resolver.resolve(envelope(pkg), candidate(text, tail))
        assertEquals(AccountIdentityDecision.FALLBACK_DEFAULT, second.decision)
        transactionRepository.recordFallback(pkg, tail, second.accountId)

        val third = resolver.resolve(envelope(pkg), candidate(text, tail))
        assertEquals(AccountIdentityDecision.CREATED_NEW, third.decision)
        assertTrue(accountRepository.accounts.any { it.name.contains("ADCB") && it.accountNumberTail == tail })
    }

    @Test fun `subsequent messages bind to the account created from repeated observations`() = runBlocking {
        val pkg = "com.adcb.nexgen"
        val text = "AED 200 debited from account"
        val tail = "920001"
        transactionRepository.recordFallback(pkg, tail, 1L)
        transactionRepository.recordFallback(pkg, tail, 1L)
        val creating = resolver.resolve(envelope(pkg), candidate(text, tail))
        assertEquals(AccountIdentityDecision.CREATED_NEW, creating.decision)

        val next = resolver.resolve(envelope(pkg), candidate(text, tail))
        assertEquals(AccountIdentityDecision.BOUND_EXISTING, next.decision)
        assertEquals(creating.accountId, next.accountId)
    }

    @Test fun `no incorrect account is ever created from a single moderate-confidence tail match alone`() = runBlocking {
        // A single sighting, even with institution+tail agreeing, must never
        // silently create an account without either near-certainty or repetition.
        val result = resolver.resolve(envelope("com.adcb.nexgen"), candidate("AED 50 spent at CARREFOUR", "1234"))
        assertEquals(AccountIdentityDecision.FALLBACK_DEFAULT, result.decision)
        assertEquals(1, accountRepository.accounts.size)
    }

    // ---- RC2: the race-condition fix, proven directly (restored after being
    // lost during an unrelated recovery mishap earlier in RC3 -- caught by
    // this same audit's "verify, don't assume" discipline) ----

    /** Delays inside insertAccount() to force a real interleaving window between
     *  "check existing accounts" and "commit the new one" -- exactly the window
     *  the historical race condition exploited. Without the resolver's mutex,
     *  concurrent resolve() calls can both pass the check before either commits. */
    private class DelayedAccountRepository : AccountRepository {
        val accounts = java.util.Collections.synchronizedList(mutableListOf(
            Account(1L, "Primary Account", AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null),
        ))
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(accounts.toList()))
        override suspend fun getAccountById(id: Long): LedgerResult<Account> =
            accounts.find { it.id == id }?.let { LedgerResult.Success(it) } ?: LedgerResult.Failure(com.sherif.ledger.core.domain.model.LedgerError.AccountNotFound)
        override suspend fun insertAccount(account: Account): LedgerResult<Long> {
            kotlinx.coroutines.delay(20) // widen the race window deliberately
            val id = (accounts.maxOfOrNull { it.id } ?: 0L) + 1
            accounts += account.copy(id = id)
            return LedgerResult.Success(id)
        }
        override suspend fun updateAccount(account: Account): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun deleteAccount(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
    }

    @Test fun `concurrent resolution for the same new identity creates exactly one account`() = runBlocking {
        val delayedAccounts = DelayedAccountRepository()
        val concurrentResolver = DeterministicAccountIdentityResolver(
            InstitutionRegistry(),
            delayedAccounts,
            transactionRepository,
            EnsureDefaultAccountUseCase(delayedAccounts),
        )
        // Single-shot near-certainty: institution + tail + currency + explicit
        // type hint all agree, so this creates on the very first sighting --
        // exactly the shape most likely to race in practice (a burst of
        // messages for a newly-seen card, e.g. on notification-listener
        // reconnect replaying a backlog).
        val env = envelope("com.fab.personalbanking")
        val cand = candidate("AED 200.00 paid towards your FAB credit card ending 6989", "6989")

        val results = (1..5).map {
            async { concurrentResolver.resolve(env, cand) }
        }.map { it.await() }

        val distinctAccountIds = results.map { it.accountId }.distinct()
        assertEquals("All concurrent resolutions must agree on exactly one account", 1, distinctAccountIds.size)
        assertEquals(
            "Exactly one FAB account must exist, not one per concurrent caller",
            1,
            delayedAccounts.accounts.count { it.name.contains("FAB") },
        )
    }

    // ---- RC3: the type-hint scoring fix, tested directly since it cannot
    // change any observable resolve() decision given today's threshold
    // configuration (institution+tail alone already clears BIND_THRESHOLD) ----

    @Test fun `scoreAgainstExisting no longer awards the type bonus when there is no type signal at all`() {
        val account = Account(1L, "FAB Account", AccountType.CHECKING, Money.zero(CurrencyCode.AED), "6989", null)
        val institution = InstitutionIdentity("FAB", CurrencyCode.AED)

        val withNoSignal = resolver.scoreAgainstExisting(account, institution, "6989", CurrencyCode.AED, typeHint = null)
        assertEquals("No type evidence must not be treated as a confirmed match", 90, withNoSignal)

        val withGenuineMatch = resolver.scoreAgainstExisting(account, institution, "6989", CurrencyCode.AED, typeHint = AccountType.CHECKING)
        assertEquals("A genuine, explicit type agreement still earns the bonus", 100, withGenuineMatch)

        val withGenuineMismatch = resolver.scoreAgainstExisting(account, institution, "6989", CurrencyCode.AED, typeHint = AccountType.CREDIT)
        assertEquals("A genuine, explicit type conflict must not earn the bonus", 90, withGenuineMismatch)
    }
}





