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

    private class FakeAccountRepository(
        seed: List<Account> = listOf(
            Account(1L, "Primary Account", AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null, isDefault = true),
        ),
    ) : AccountRepository {
        val accounts = seed.toMutableList()
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(accounts.toList()))
        override suspend fun getAccountById(id: Long): LedgerResult<Account> =
            accounts.find { it.id == id }?.let { LedgerResult.Success(it) } ?: LedgerResult.Failure(com.sherif.ledger.core.domain.model.LedgerError.AccountNotFound)
        override suspend fun insertAccount(account: Account): LedgerResult<Long> {
            val id = (accounts.maxOfOrNull { it.id } ?: 0L) + 1
            accounts += account.copy(id = id)
            return LedgerResult.Success(id)
        }
        // Persists, like the real Room-backed repository. A no-op here silently hid
        // the resolver's account-adoption path (claiming the untailed default
        // account instead of creating a duplicate beside it).
        override suspend fun updateAccount(account: Account): LedgerResult<Unit> {
            val index = accounts.indexOfFirst { it.id == account.id }
            if (index >= 0) accounts[index] = account
            return LedgerResult.Success(Unit)
        }
        override suspend fun deleteAccount(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun getDeletedAccounts(): LedgerResult<List<Account>> = LedgerResult.Success(emptyList())
        override fun observeCandidateAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(accounts.filter { it.isCandidate }))
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
        // Actually moves them, like the real Room-backed repository. A no-op here
        // hid the resolver leaving earlier fallback transactions stranded on the
        // default account after creating the real one.
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long): Int {
            val bySignature = originCounts[packageName to cardTail] ?: return 0
            val moved = bySignature.remove(fromAccountId) ?: return 0
            bySignature.merge(toAccountId, moved) { a, b -> a + b }
            return moved
        }

        // Transactions from this package that quote no account number at all. The
        // real bank sends both shapes; modelled with an empty-string tail key.
        override suspend fun reassignUntailedTransactions(fromAccountId: Long, packageName: String, toAccountId: Long): Int {
            val bySignature = originCounts[packageName to ""] ?: return 0
            val moved = bySignature.remove(fromAccountId) ?: return 0
            bySignature.merge(toAccountId, moved) { a, b -> a + b }
            return moved
        }
        override suspend fun reassignAllTransactions(fromAccountId: Long, toAccountId: Long): Int = 0

        /** Transactions currently attributed to [accountId] for this signature. */
        fun countFor(packageName: String, tail: String, accountId: Long): Int =
            originCounts[packageName to tail]?.get(accountId) ?: 0
    }

    private class FakeLearnedDecisionDao : com.sherif.ledger.core.database.dao.LearnedDecisionDao {
        val entries = mutableListOf<com.sherif.ledger.core.database.entity.LearnedDecisionEntity>()
        override suspend fun getAll() = entries.toList()
        override suspend fun upsert(entity: com.sherif.ledger.core.database.entity.LearnedDecisionEntity) {
            entries.removeAll { it.decisionType == entity.decisionType && it.subjectKey == entity.subjectKey }
            entries += entity
        }
    }

    private val accountRepository = FakeAccountRepository()
    private val transactionRepository = FakeTransactionRepository()
    private val learnedDecisionDao = FakeLearnedDecisionDao()
    private val learnedDecisionStore = com.sherif.ledger.core.domain.service.intelligence.LearnedDecisionStore(learnedDecisionDao)
    private val resolver = DeterministicAccountIdentityResolver(
        InstitutionRegistry(),
        accountRepository,
        transactionRepository,
        EnsureDefaultAccountUseCase(accountRepository),
        learnedDecisionStore,
        SenderClassifier(),
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

    @Test fun `unknown institution is parked as a Candidate Account, never merged into the default account`() = runBlocking {
        // RC7 Phase B: this replaces the old FALLBACK_DEFAULT behavior for an
        // unrecognized institution — merging an unknown bank's transaction
        // into the default account is exactly the shape of the confirmed
        // HDFC Bank currency-mixing bug (RC6). It must land in its own,
        // separate, currency-correct Candidate Account instead.
        val result = resolver.resolve(envelope("com.unknown.bank"), candidate("some text", "1234"))
        assertEquals(AccountIdentityDecision.CANDIDATE, result.decision)
        assertTrue("Candidate account must never be the default account", result.accountId != 1L)
        val created = accountRepository.accounts.first { it.id == result.accountId }
        assertTrue(created.isCandidate)
        assertEquals(CurrencyCode.AED, created.openingBalance.currencyCode)
    }

    @Test fun `a learned institution mapping binds directly instead of creating another candidate`() = runBlocking {
        // Simulate: a Candidate Account for "com.unknown.bank" was already
        // promoted once (PromoteCandidateAccountUseCase would have called
        // learn() at that point) and a confirmed account with that name exists.
        accountRepository.accounts += Account(
            id = 99L, name = "SomeBank Account", type = AccountType.CHECKING,
            openingBalance = Money.zero(CurrencyCode.AED), accountNumberTail = "1234", bankBrandId = null,
        )
        learnedDecisionStore.learn(com.sherif.ledger.core.domain.service.intelligence.DecisionType.INSTITUTION, "com.unknown.bank", "SomeBank Account")

        val result = resolver.resolve(envelope("com.unknown.bank"), candidate("some text", "1234"))
        assertEquals(AccountIdentityDecision.BOUND_EXISTING, result.decision)
        assertEquals(99L, result.accountId)
        assertTrue("No new candidate should be created once the institution is learned", accountRepository.accounts.none { it.isCandidate })
    }

    @Test fun `repeated messages from the same unrecognized institution reuse the same Candidate Account`() = runBlocking {
        val first = resolver.resolve(envelope("com.unknown.bank"), candidate("some text", "1234"))
        val second = resolver.resolve(envelope("com.unknown.bank"), candidate("more text", "1234"))
        assertEquals(AccountIdentityDecision.CANDIDATE, second.decision)
        assertEquals(first.accountId, second.accountId)
        assertEquals(1, accountRepository.accounts.count { it.isCandidate })
    }

    // ---- Senders that can never name an account ----
    //
    // A fresh import of the owner's real inbox created twelve accounts, ten of them
    // named after things that are not banks. Every identifier below is one that
    // actually produced an account row on the device.

    @Test fun `a messaging app never gets an account named after it`() = runBlocking {
        // "Unrecognized Institution (com.google.android.apps.messaging) •1959" held
        // the same card tail as the owner's real "Mashreq Credit Card •1959", so one
        // card's spending was split across two accounts.
        val result = resolver.resolve(
            envelope("com.google.android.apps.messaging"),
            candidate("AED 46.80 spent on card ending 1959", "1959"),
        )

        assertEquals(AccountIdentityDecision.CANDIDATE, result.decision)
        assertTrue(
            "No account may be named after the messaging app that displayed the message",
            accountRepository.accounts.none { it.name.contains("messaging") },
        )
        val holding = accountRepository.accounts.first { it.id == result.accountId }
        assertEquals("Unattributed Capture (AED)", holding.name)
        assertTrue("Must stay off every balance", holding.isCandidate)
        assertEquals("A tail seen through a messenger belongs to the issuing bank", null, holding.accountNumberTail)
    }

    @Test fun `every transport and telecom sender shares one holding account per currency`() = runBlocking {
        // The real senders, in the order the import met them.
        listOf("com.truecaller", "com.google.android.apps.messaging", "Smiles", "eandINF", "eandUAE", "AD-eand")
            .forEach { resolver.resolve(envelope(it), candidate("AED 50 charged", null)) }

        assertEquals(
            "Six unattributable senders must not become six accounts",
            1, accountRepository.accounts.count { it.isCandidate },
        )
        assertEquals("Unattributed Capture (AED)", accountRepository.accounts.first { it.isCandidate }.name)
    }

    @Test fun `a non-financial senders message is discarded, never even an unattributed transaction`() = runBlocking {
        // Real user testing: a "chance to win AED 100 voucher" message from
        // Smiles, and a telecom recharge-offer promotion, both cleared the
        // amount+verb extraction bar and showed up as real line items in the
        // transaction list. Nothing here should even reach persistence — not
        // the default account, not a candidate, not the shared unattributed
        // bucket. There is no transaction to attribute.
        val result = resolver.resolve(envelope("Smiles"), candidate("AED 500 cashback", null))

        assertEquals(AccountIdentityDecision.DISCARD, result.decision)
        assertTrue("Must never be the default account", result.accountId != 1L)
        assertTrue("No account should be touched at all", accountRepository.accounts.none { it.isCandidate })
    }

    @Test fun `a transport apps relayed message still becomes a real transaction, unlike a non-financial senders`() = runBlocking {
        // A messaging app (Google Messages, Truecaller) only DISPLAYED a real
        // bank's message — that's still a real transaction, just needs
        // somewhere to land until a future pass identifies the actual bank
        // from the text. This must not be swept into the same discard as
        // SenderKind.NON_FINANCIAL, or real transactions would silently vanish.
        val result = resolver.resolve(envelope("com.google.android.apps.messaging"), candidate("AED 50 charged", null))

        assertEquals(AccountIdentityDecision.CANDIDATE, result.decision)
        assertTrue("Must land on the shared unattributed bucket, not be discarded", accountRepository.accounts.any { it.isCandidate })
    }

    @Test fun `a wallet is not swept up with the telecom senders that share its prefix`() = runBlocking {
        // e& money is a real payment wallet; eandINF/eandUAE/AD-eand are the
        // operator's own marketing and billing senders. Matching whole identifiers
        // rather than a prefix is what keeps these apart.
        val wallet = resolver.resolve(envelope("eandmoney"), candidate("AED 200 paid", "4321"))
        val marketing = resolver.resolve(envelope("eandINF"), candidate("AED 200 paid", "4321"))

        assertEquals(AccountIdentityDecision.CANDIDATE, wallet.decision)
        assertTrue(
            "e& money keeps its own reviewable, promotable candidate account",
            accountRepository.accounts.any { it.name == "Unrecognized Institution (eandmoney) •4321" },
        )
        assertTrue("but the operator's marketing sender does not", wallet.accountId != marketing.accountId)
    }

    @Test fun `an unrecognised bank still keeps its own promotable candidate account`() = runBlocking {
        // Guards against over-reach: the point is to refuse messengers and telecoms,
        // not to collapse every institution Ledger has not met yet into one bucket.
        // RC7 Phase B's per-institution candidate is what makes promotion possible.
        val result = resolver.resolve(envelope("MBANKAlert"), candidate("AED 120 spent", "000001"))

        assertEquals(AccountIdentityDecision.CANDIDATE, result.decision)
        assertEquals(
            "Unrecognized Institution (MBANKAlert) •000001",
            accountRepository.accounts.first { it.id == result.accountId }.name,
        )
    }

    // ---- ACCOUNT_IDENTITY_PLAN Step 2: the default account is created only
    // when a message actually needs it as a fallback target ----

    @Test fun `the default account is never created when the first message resolves cleanly to a real institution`() = runBlocking {
        val emptyAccounts = FakeAccountRepository(seed = emptyList())
        val resolver = DeterministicAccountIdentityResolver(
            InstitutionRegistry(), emptyAccounts, transactionRepository,
            EnsureDefaultAccountUseCase(emptyAccounts), learnedDecisionStore, SenderClassifier(),
        )

        val result = resolver.resolve(
            envelope("com.fab.personalbanking"),
            candidate("AED 200.00 paid towards your FAB credit card ending 6989", "6989"),
        )

        assertEquals(AccountIdentityDecision.CREATED_NEW, result.decision)
        assertEquals(
            "Only the real FAB account should exist — no separate fallback row",
            1, emptyAccounts.accounts.size,
        )
        assertTrue(emptyAccounts.accounts.single().name.contains("FAB"))
    }

    @Test fun `the default account is still created lazily when a message genuinely needs to fall back`() = runBlocking {
        val emptyAccounts = FakeAccountRepository(seed = emptyList())
        val resolver = DeterministicAccountIdentityResolver(
            InstitutionRegistry(), emptyAccounts, transactionRepository,
            EnsureDefaultAccountUseCase(emptyAccounts), learnedDecisionStore, SenderClassifier(),
        )

        assertTrue("No account should exist before any message is processed", emptyAccounts.accounts.isEmpty())
        val result = resolver.resolve(envelope("com.fab.personalbanking"), candidate("some text", null))

        assertEquals(AccountIdentityDecision.FALLBACK_DEFAULT, result.decision)
        assertTrue(emptyAccounts.accounts.single { it.id == result.accountId }.isDefault)
    }

    // ---- ACCOUNT_IDENTITY_PLAN Step 3: adopt an untailed real account
    // instead of creating a sibling ----

    @Test fun `a tailed message adopts an existing untailed account at the same institution instead of duplicating it`() = runBlocking {
        accountRepository.accounts += Account(
            id = 50L, name = "ADCB Account", type = AccountType.CHECKING,
            openingBalance = Money(150_000L, CurrencyCode.AED), accountNumberTail = null, bankBrandId = null,
        )

        val result = resolver.resolve(
            envelope("com.adcb.nexgen"),
            candidate("AED 200 debited from account", "920001"),
        )

        assertEquals(AccountIdentityDecision.BOUND_EXISTING, result.decision)
        assertEquals(50L, result.accountId)
        assertEquals(
            "No sibling ADCB account should be created",
            1, accountRepository.accounts.count { it.name.contains("ADCB") },
        )
        assertEquals("920001", accountRepository.accounts.first { it.id == 50L }.accountNumberTail)
    }

    @Test fun `adoption never touches the default account itself`() = runBlocking {
        // The default account is untailed too — RC7 Phase B's invariant that it is
        // never bound to as a real institution's identity must still hold here.
        val result = resolver.resolve(
            envelope("com.adcb.nexgen"),
            candidate("AED 200.00 paid towards your FAB credit card ending 6989", "6989"),
        )
        assertTrue(result.accountId != 1L)
    }

    @Test fun `two untailed accounts at the same institution is ambiguous, so adoption is skipped`() = runBlocking {
        accountRepository.accounts += Account(
            id = 50L, name = "ADCB Account A", type = AccountType.CHECKING,
            openingBalance = Money.zero(CurrencyCode.AED), accountNumberTail = null, bankBrandId = null,
        )
        accountRepository.accounts += Account(
            id = 51L, name = "ADCB Account B", type = AccountType.CHECKING,
            openingBalance = Money.zero(CurrencyCode.AED), accountNumberTail = null, bankBrandId = null,
        )

        val result = resolver.resolve(
            envelope("com.adcb.nexgen"),
            candidate("AED 200.00 paid towards your FAB credit card ending 6989", "6989"),
        )

        assertEquals(AccountIdentityDecision.CREATED_NEW, result.decision)
        assertTrue(accountRepository.accounts.none { it.accountNumberTail == "6989" && it.id != result.accountId })
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

    @Test fun `creating an account reclaims the fallback transactions that justified it`() = runBlocking {
        // Every sighting before an account exists is parked on the default account.
        // Once the real account is created those must move onto it — otherwise one
        // real account is split in two, and any balance the user reconciles against
        // the default account is stranded away from its own transactions.
        // Observed live: AED 1,568.52 on "Primary Account" with 11 stranded ADCB
        // transactions while "ADCB Account ···920001" collected the other 25.
        val pkg = "com.adcb.nexgen"
        val text = "AED 200 debited from account"
        val tail = "920001"
        transactionRepository.recordFallback(pkg, tail, 1L)
        transactionRepository.recordFallback(pkg, tail, 1L)
        assertEquals(2, transactionRepository.countFor(pkg, tail, 1L))

        val created = resolver.resolve(envelope(pkg), candidate(text, tail))
        assertEquals(AccountIdentityDecision.CREATED_NEW, created.decision)

        assertEquals(
            "fallback transactions must not stay on the default account",
            0, transactionRepository.countFor(pkg, tail, 1L),
        )
        assertEquals(
            "they belong to the account their own signature created",
            2, transactionRepository.countFor(pkg, tail, created.accountId),
        )
    }

    @Test fun `creating an account also reclaims the banks tail-less messages`() = runBlocking {
        // ADCB sends both "…from acc. no. XXX920001…" and messages with no account
        // number. The tail-less ones can never match an origin signature, so they
        // stayed on the fallback account and split one real account by message
        // shape: AED 1,568.52 stranded on "Primary Account" while the tailed ones
        // sat on "ADCB Account ···920001" — the dashboard showed 1,104.21.
        val pkg = "com.adcb.nexgen"
        val text = "AED 200 debited from account"
        val tail = "920001"
        transactionRepository.recordFallback(pkg, tail, 1L)
        transactionRepository.recordFallback(pkg, tail, 1L)
        transactionRepository.recordFallback(pkg, "", 1L) // tail-less, same bank

        val created = resolver.resolve(envelope(pkg), candidate(text, tail))
        assertEquals(AccountIdentityDecision.CREATED_NEW, created.decision)

        assertEquals(
            "tail-less messages from the same bank must not stay stranded",
            0, transactionRepository.countFor(pkg, "", 1L),
        )
        assertEquals(
            "they belong with the rest of that bank's account",
            1, transactionRepository.countFor(pkg, "", created.accountId),
        )
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
            Account(1L, "Primary Account", AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null, isDefault = true),
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
        // Persists, like the real Room-backed repository. A no-op here silently hid
        // the resolver's account-adoption path (claiming the untailed default
        // account instead of creating a duplicate beside it).
        override suspend fun updateAccount(account: Account): LedgerResult<Unit> {
            val index = accounts.indexOfFirst { it.id == account.id }
            if (index >= 0) accounts[index] = account
            return LedgerResult.Success(Unit)
        }
        override suspend fun deleteAccount(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun getDeletedAccounts(): LedgerResult<List<Account>> = LedgerResult.Success(emptyList())
        override fun observeCandidateAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(accounts.filter { it.isCandidate }))
    }

    @Test fun `concurrent resolution for the same new identity creates exactly one account`() = runBlocking {
        val delayedAccounts = DelayedAccountRepository()
        val concurrentResolver = DeterministicAccountIdentityResolver(
            InstitutionRegistry(),
            delayedAccounts,
            transactionRepository,
            EnsureDefaultAccountUseCase(delayedAccounts),
            com.sherif.ledger.core.domain.service.intelligence.LearnedDecisionStore(FakeLearnedDecisionDao()),
            SenderClassifier(),
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





