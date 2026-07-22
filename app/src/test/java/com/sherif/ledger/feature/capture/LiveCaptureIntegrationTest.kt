package com.sherif.ledger.feature.capture

import com.sherif.ledger.core.domain.service.account.InstitutionRegistry
import com.sherif.ledger.core.domain.service.account.DeterministicAccountIdentityResolver
import com.sherif.ledger.feature.semantic.DeterministicFinancialIntentClassifier
import com.sherif.ledger.feature.diagnostics.PipelineTraceSink
import com.sherif.ledger.feature.capture.extraction.ConfirmationMatcher
import com.sherif.ledger.feature.capture.extraction.KnownBankExtractor
import com.sherif.ledger.feature.capture.extraction.ExtractionValidator
import com.sherif.ledger.feature.capture.extraction.ExtractionRegistry
import com.sherif.ledger.core.domain.model.*
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.repository.TransactionRunner
import com.sherif.ledger.core.domain.service.transaction.*
import com.sherif.ledger.core.domain.usecase.account.EnsureDefaultAccountUseCase
import com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.notification.NotificationFilter
import com.sherif.ledger.feature.capture.parsing.AdcbParser
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.PatternEngine
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import com.sherif.ledger.feature.capture.reconciliation.ReconciliationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class LiveCaptureIntegrationTest {

    private val transactionRepository = object : TransactionRepository {
        var insertedCount = 0
        override fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>> = flowOf()
        override fun observeTransactionsForAccount(accountId: Long): Flow<LedgerResult<List<Transaction>>> = flowOf()
        override fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>> = flowOf(LedgerResult.Success(emptyList()))
        override suspend fun getTransactionById(id: Long): LedgerResult<Transaction> = LedgerResult.Failure(LedgerError.Unknown("Not implemented in test"))
        override suspend fun insertTransaction(transaction: Transaction): LedgerResult<Long> {
            insertedCount++
            return LedgerResult.Success(insertedCount.toLong())
        }
        override suspend fun deleteTransaction(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun updateNote(id: Long, note: String?): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override fun observeAllTransactions(): Flow<LedgerResult<List<Transaction>>> = flowOf(LedgerResult.Success(emptyList()))
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String): List<com.sherif.ledger.core.domain.repository.AccountOriginCount> = emptyList()
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long): Int = 0
    }

    private val accountRepository = object : AccountRepository {
        // Fixed: was flowOf() (truly empty, zero emissions). DeterministicAccountIdentityResolver
        // calls observeAllAccounts().first() unconditionally (unchanged since the original
        // Phase 10 base) -- an empty flow throws NoSuchElementException immediately, before
        // this test's own logic ever runs. Kept consistent with getAccountById below rather
        // than an arbitrary empty list, matching what a real repository would present.
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(
            LedgerResult.Success(listOf(Account(1L, "Primary", AccountType.CHECKING, Money(500000L, CurrencyCode.AED), null, null)))
        )
        override suspend fun getAccountById(id: Long): LedgerResult<Account> = LedgerResult.Success(
            Account(1L, "Primary", AccountType.CHECKING, Money(500000L, CurrencyCode.AED), null, null)
        )
        override suspend fun insertAccount(account: Account): LedgerResult<Long> = LedgerResult.Success(1L)
        override suspend fun updateAccount(account: Account): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun deleteAccount(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun getDeletedAccounts() = LedgerResult.Success(emptyList<Account>())
        override fun observeCandidateAccounts() = kotlinx.coroutines.flow.flowOf(LedgerResult.Success(emptyList<Account>()))
    }

    private val transactionRunner = object : TransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
    }

    private val insertTransactionUseCase = InsertTransactionUseCase(
        transactionRepository,
        accountRepository,
        transactionRunner,
        TransactionValidator(),
        FingerprintGenerator(),
        CaptureMerchantResolver(object : com.sherif.ledger.core.domain.repository.MerchantRepository {
            override suspend fun getAllBrands(): LedgerResult<List<Brand>> = LedgerResult.Success(emptyList())
            override suspend fun getBrandByAlias(rawText: String): LedgerResult<Brand> = LedgerResult.Failure(LedgerError.Unknown(""))
            override suspend fun insertBrand(brand: Brand): LedgerResult<Long> = LedgerResult.Success(1L)
            override suspend fun registerAlias(rawText: String, brandId: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
        }),
        CategoryResolver(),
        com.sherif.ledger.testsupport.FakeFinancialEventRepository(),
    )

    private val parserRegistry = ParserRegistry(setOf(
        AdcbParser(PatternEngine(TextNormalizer()), MerchantNormalizer())
    ))

    private val useCase = ProcessNotificationUseCase(
        NotificationFilter(),
        ExtractionRegistry(setOf(KnownBankExtractor(parserRegistry)), ExtractionValidator()),
        ConfirmationMatcher(),
        ReconciliationEngine(FingerprintGenerator()),
        transactionRepository,
        insertTransactionUseCase,
        DeterministicAccountIdentityResolver(
            InstitutionRegistry(),
            accountRepository,
            transactionRepository,
            EnsureDefaultAccountUseCase(accountRepository),
            com.sherif.ledger.core.domain.service.intelligence.LearnedDecisionStore(
                object : com.sherif.ledger.core.database.dao.LearnedDecisionDao {
                    override suspend fun getAll() = emptyList<com.sherif.ledger.core.database.entity.LearnedDecisionEntity>()
                    override suspend fun upsert(entity: com.sherif.ledger.core.database.entity.LearnedDecisionEntity) {}
                },
            ),
        ),
            PipelineTraceSink(),
            DeterministicFinancialIntentClassifier(),
            FakeTransactionNotifier,
    )

    @Test
    fun `notification flows end-to-end to repository`() = runBlocking {
        val envelope = NotificationEnvelope(
            packageName = "com.adcb.nexgen", // RC7: was the stale "com.adcb.mobileapp", corrected alongside AdcbParser/InstitutionRegistry
            title = "Transaction",
            text = "Purchase of AED 50.00 at AMAZON AE with card ending 1234.",
            subText = null,
            timestamp = Instant.now(),
            notificationKey = "key"
        )

        useCase.execute(envelope)

        assertEquals(1, transactionRepository.insertedCount)
    }
}

private object FakeTransactionNotifier : com.sherif.ledger.feature.notification.TransactionNotifier {
    override fun notifyCaptured(
        transaction: com.sherif.ledger.core.domain.model.Transaction,
        merchantOrDescription: String,
        formattedAmount: String,
    ) {
        // no-op: notification posting requires the Android framework, out of
        // scope for these plain JVM tests
    }
}



