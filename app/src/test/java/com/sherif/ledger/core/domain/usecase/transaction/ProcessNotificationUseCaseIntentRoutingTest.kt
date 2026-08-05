package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.domain.service.account.InstitutionRegistry
import com.sherif.ledger.core.domain.service.account.DeterministicAccountIdentityResolver
import com.sherif.ledger.core.domain.model.*
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.repository.TransactionRunner
import com.sherif.ledger.core.domain.service.transaction.*
import com.sherif.ledger.core.domain.usecase.account.EnsureDefaultAccountUseCase
import com.sherif.ledger.feature.capture.extraction.ConfirmationMatcher
import com.sherif.ledger.feature.capture.extraction.ExtractionRegistry
import com.sherif.ledger.feature.capture.extraction.ExtractionValidator
import com.sherif.ledger.feature.capture.extraction.FinancialPhraseLibrary
import com.sherif.ledger.feature.capture.extraction.HeuristicExtractor
import com.sherif.ledger.feature.capture.extraction.KnownBankExtractor
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.notification.NotificationFilter
import com.sherif.ledger.feature.capture.parsing.GenericBankParser
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import com.sherif.ledger.feature.capture.reconciliation.ReconciliationEngine
import com.sherif.ledger.feature.diagnostics.PipelineStage
import com.sherif.ledger.feature.diagnostics.PipelineTraceSink
import com.sherif.ledger.feature.semantic.DeterministicFinancialIntentClassifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Regression suite for the refined Phase 7 architecture: extraction ALWAYS runs,
 * NEVER decides routing, and the classifier is the sole authority — consuming the
 * complete ExtractionOutcome, including Ignored and Failed. Uses the REAL
 * extraction pipeline (not fakes) so these prove actual behavior, not a mocked
 * approximation of it.
 */
class ProcessNotificationUseCaseIntentRoutingTest {

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
        override suspend fun reassignAllTransactions(fromAccountId: Long, toAccountId: Long): Int = 0
    }

    private val accountRepository = object : AccountRepository {
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(emptyList()))
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

    /** Builds the use case with the REAL extraction pipeline (no fakes). */
    private fun realUseCase(traceSink: PipelineTraceSink = PipelineTraceSink()): ProcessNotificationUseCase {
        val textNormalizer = TextNormalizer()
        val merchantNormalizer = MerchantNormalizer()
        val parserRegistry = ParserRegistry(setOf(GenericBankParser(textNormalizer, merchantNormalizer)))
        val registry = ExtractionRegistry(
            setOf(
                KnownBankExtractor(parserRegistry),
                HeuristicExtractor(textNormalizer, merchantNormalizer, FinancialPhraseLibrary()),
            ),
            ExtractionValidator(),
        )
        return ProcessNotificationUseCase(
            NotificationFilter(),
            registry,
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
                com.sherif.ledger.core.domain.service.account.SenderClassifier(),
            ),
            traceSink,
            DeterministicFinancialIntentClassifier(),
            FakeTransactionNotifier,
            FakeAiCategorizationTrigger,
        )
    }

    private fun envelope(text: String, pkg: String = "com.adcb.nexgen") =
        NotificationEnvelope(pkg, "", text, null, Instant.now(), "key-${text.hashCode()}")

    // ---- The real production bug, through the FULL pipeline ----
    @Test
    fun `FAB payment processed extracts successfully but never persists`() = runBlocking {
        transactionRepository.insertedCount = 0
        val useCase = realUseCase()
        useCase.execute(envelope("Dear Customer, Your Payment of AED 150.00 for card 5492XXXXXXXX6989 has been processed on 09/07/2026", "com.fab.personalbanking"))
        assertEquals("FAB confirmation must never be persisted", 0, transactionRepository.insertedCount)
    }

    @Test
    fun `ADCB debit persists as a genuine event`() = runBlocking {
        transactionRepository.insertedCount = 0
        val useCase = realUseCase()
        useCase.execute(envelope("AED 200 debited from ADCB account XXX920001. Avl. bal. AED 7955.36."))
        assertEquals(1, transactionRepository.insertedCount)
    }

    // ---- The core architectural guarantee: extraction never gates routing ----
    @Test
    fun `a confirmation with no amount at all still never persists and is still classified`() = runBlocking {
        transactionRepository.insertedCount = 0
        val sink = PipelineTraceSink()
        val useCase = realUseCase(sink)
        // No currency amount anywhere in this message: extraction WILL fail or
        // ignore it (no anchored amount, no account tail). It must still reach the
        // classifier and be correctly routed as a confirmation.
        val text = "Thank you for your payment. Your account is now up to date."
        useCase.execute(envelope(text))

        assertEquals("Must never fabricate a transaction from an unmatched confirmation", 0, transactionRepository.insertedCount)

        val trace = sink.recent().last()
        val extractionEvent = trace.eventFor(PipelineStage.FINANCIAL_EXTRACTORS)
        val intentEvent = trace.eventFor(PipelineStage.INTENT_CLASSIFIER)
        assertNotNull("Extraction stage must still be recorded", extractionEvent)
        assertNotNull(
            "Intent classifier stage MUST be present even when extraction found nothing " +
                "(nothing may return early before classification)",
            intentEvent,
        )
        assertEquals("FINANCIAL_CONFIRMATION", intentEvent!!.metadata["intent"])
    }

    @Test
    fun `a message with an amount but no decisive signal is classified unknown and never persists`() = runBlocking {
        transactionRepository.insertedCount = 0
        val sink = PipelineTraceSink()
        val useCase = realUseCase(sink)
        // Has a currency amount (passes the filter, a separate upstream gate) but
        // no account/card pattern (extraction fails to produce a candidate) and no
        // confirmation/information/movement phrase (the classifier's own analysis
        // is silent too). Must still reach the classifier and land on UNKNOWN.
        val text = "AED 999 miscellaneous note on your ledger"
        useCase.execute(envelope(text))

        assertEquals("Must never fabricate a transaction from an unresolved message", 0, transactionRepository.insertedCount)
        val intentEvent = sink.recent().last().eventFor(PipelineStage.INTENT_CLASSIFIER)
        assertNotNull("Must still reach the classifier even with no decisive signal", intentEvent)
        assertEquals("UNKNOWN", intentEvent!!.metadata["intent"])
    }

    @Test
    fun `a message with no financial signal at all is correctly rejected by the filter`() {
        // The filter is a separate, legitimate upstream gate (Notification -> Filter
        // -> ExtractionRegistry -> Classifier). A message with no financial signal
        // at all is discarded here, BEFORE extraction/classification -- this is not
        // the early-return this phase eliminated; that constraint concerns the space
        // between extraction and classification, not the filter itself.
        runBlocking {
            transactionRepository.insertedCount = 0
            val sink = PipelineTraceSink()
            val useCase = realUseCase(sink)
            useCase.execute(envelope("Please verify your identity to continue using our services."))

            assertEquals(0, transactionRepository.insertedCount)
            val trace = sink.recent().last()
            assertEquals(
                "A message with no financial signal is rejected at the filter, before classification",
                null,
                trace.eventFor(PipelineStage.INTENT_CLASSIFIER),
            )
        }
    }

    // ---- Duplicate confirmation chain through the full pipeline: one event only ----
    @Test
    fun `duplicate confirmation chain through full pipeline persists exactly once`() = runBlocking {
        transactionRepository.insertedCount = 0
        val useCase = realUseCase()
        useCase.execute(envelope("AED 150 debited from ADCB account XXX920001"))
        useCase.execute(envelope("Your Payment of AED 150.00 for card 5492XXXXXXXX6989 has been processed on 09/07/2026", "com.fab.personalbanking"))
        useCase.execute(envelope("Payment received. Outstanding balance updated. Thank you for your payment.", "com.mashreq.app"))

        assertEquals("Exactly one transaction from the whole chain", 1, transactionRepository.insertedCount)
    }
}






