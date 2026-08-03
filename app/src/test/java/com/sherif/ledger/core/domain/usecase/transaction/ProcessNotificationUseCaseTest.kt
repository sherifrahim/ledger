package com.sherif.ledger.core.domain.usecase.transaction

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
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.notification.NotificationFilter
import com.sherif.ledger.feature.capture.parsing.ParseResult
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import com.sherif.ledger.feature.capture.reconciliation.ReconciliationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ProcessNotificationUseCaseTest {

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
        override suspend fun reassignUntailedTransactions(fromAccountId: Long, packageName: String, toAccountId: Long): Int = 0
    }

    private val accountRepository = object : AccountRepository {
        // Fixed: was flowOf() (truly empty, zero emissions). See LiveCaptureIntegrationTest
        // for the identical fix and full rationale -- DeterministicAccountIdentityResolver
        // calls observeAllAccounts().first() unconditionally, throwing on an empty flow.
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

    private val insertTransactionUseCase = InsertTransactionUseCase(
        transactionRepository,
        accountRepository,
        object : TransactionRunner { override suspend fun <T> runInTransaction(block: suspend () -> T): T = block() },
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

    @Test
    fun `inserts new transaction when reconciliation returns New`() = runBlocking {
        val envelope = createEnvelope()
        val candidate = createCandidate()
        
        val parserRegistry = ParserRegistry(setOf(object : com.sherif.ledger.feature.capture.parsing.BankParser {
            override fun supports(envelope: NotificationEnvelope): Boolean = true
            override fun parse(envelope: NotificationEnvelope): ParseResult = ParseResult.Success(candidate)
        }))
        
        val reconciliationEngine = ReconciliationEngine(FingerprintGenerator())

        val useCase = ProcessNotificationUseCase(
            NotificationFilter(),
            ExtractionRegistry(setOf(KnownBankExtractor(parserRegistry)), ExtractionValidator()),
            ConfirmationMatcher(),
            reconciliationEngine,
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

        useCase.execute(envelope)

        assertEquals(1, transactionRepository.insertedCount)
    }

    private fun createEnvelope() = NotificationEnvelope("com.adcb.mobileapp", "title", "Purchase of AED 50 at Amazon", null, Instant.now(), "key")
    private fun createCandidate() = TransactionCandidate(IngestionSource.SMS, "raw", "Amazon", 1000L, CurrencyCode.AED, Instant.now(), null, 1L, TransactionType.EXPENSE)
}






