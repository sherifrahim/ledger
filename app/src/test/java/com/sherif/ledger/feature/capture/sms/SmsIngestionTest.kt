package com.sherif.ledger.feature.capture.sms

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
import com.sherif.ledger.core.domain.service.transaction.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SmsIngestionTest {

    private val transactionRepository = object : TransactionRepository {
        var insertedTransactions = mutableListOf<Transaction>()
        override fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>> = flowOf()
        override fun observeTransactionsForAccount(accountId: Long): Flow<LedgerResult<List<Transaction>>> = flowOf()
        override fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>> = 
            flowOf(LedgerResult.Success(insertedTransactions))
            
        override suspend fun getTransactionById(id: Long): LedgerResult<Transaction> = LedgerResult.Failure(LedgerError.Unknown(""))
        override suspend fun insertTransaction(transaction: Transaction): LedgerResult<Long> {
            val id = (insertedTransactions.size + 1).toLong()
            insertedTransactions.add(transaction.copy(id = id))
            return LedgerResult.Success(id)
        }
        override suspend fun deleteTransaction(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun updateNote(id: Long, note: String?): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override fun observeAllTransactions(): Flow<LedgerResult<List<Transaction>>> = flowOf(LedgerResult.Success(emptyList()))
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String): List<com.sherif.ledger.core.domain.repository.AccountOriginCount> = emptyList()
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long): Int = 0
        override suspend fun reassignUntailedTransactions(fromAccountId: Long, packageName: String, toAccountId: Long): Int = 0
    }

    private val accountRepository = object : AccountRepository {
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf(LedgerResult.Success(listOf(
            Account(1L, "Primary", AccountType.CHECKING, Money(500000L, CurrencyCode.AED), null, null)
        )))
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
    fun `SMS with salary credit is parsed and inserted`() = runBlocking {
        val sms = NotificationEnvelope(
            packageName = "ADCB",
            title = "SMS",
            text = "Your salary AED6000.00 has been credited to your account no. XXX920001 on Jul 3 2026 2:12PM. The available balance is AED9079.30.",
            subText = null,
            timestamp = Instant.now(),
            notificationKey = "sms_1",
            source = IngestionSource.SMS
        )

        useCase.execute(sms)

        assertEquals(1, transactionRepository.insertedTransactions.size)
        val txn = transactionRepository.insertedTransactions.first()
        assertEquals(600000L, txn.amount.minorUnits)
        assertEquals(TransactionType.INCOME, txn.type)
    }

    @Test
    fun `ADCB personal-internet-banking transfer SMS is captured as outgoing transfer`() = runBlocking {
        // Real user SMS (redacted) that Ledger failed to capture. Amount is glued to
        // the currency ("AED2770.00"), there's a second amount (the balance), the
        // account is "acc. no. XXX920001", and the channel is "Personal Internet
        // Banking / Mobile App" with no card tail — a person-to-person transfer.
        val sms = NotificationEnvelope(
            packageName = "ADCB",
            title = "SMS",
            text = "AED2770.00 transferred via ADCB Personal Internet Banking / Mobile App " +
                "from acc. no. XXX920001 on Jul 23 2026 3:56PM. Avl. bal. AED 1493.52.",
            subText = null,
            timestamp = Instant.now(),
            notificationKey = "sms_transfer_1",
            source = IngestionSource.SMS,
        )

        useCase.execute(sms)

        assertEquals(1, transactionRepository.insertedTransactions.size)
        val txn = transactionRepository.insertedTransactions.first()
        assertEquals(277000L, txn.amount.minorUnits)
        assertEquals(TransactionType.TRANSFER, txn.type)
        assertEquals(TransferDirection.OUTGOING, txn.transferDirection)
    }

    @Test
    fun `Duplicate SMS is reconciled and not inserted twice`() = runBlocking {
        val sms = NotificationEnvelope(
            packageName = "ADCB",
            title = "SMS",
            text = "Purchase of AED 50.00 at AMAZON AE with card ending 1234.",
            subText = null,
            timestamp = Instant.now(),
            notificationKey = "sms_2",
            source = IngestionSource.SMS
        )

        useCase.execute(sms)
        useCase.execute(sms.copy(notificationKey = "sms_3")) // Same content, different key (e.g. re-receive)

        assertEquals(1, transactionRepository.insertedTransactions.size)
    }

    @Test
    fun `Notification and SMS for same transaction are reconciled`() = runBlocking {
        val timestamp = Instant.now()
        val notification = NotificationEnvelope(
            packageName = "com.adcb.mobileapp",
            title = "Transaction",
            text = "Purchase of AED 50.00 at AMAZON AE with card ending 1234.",
            subText = null,
            timestamp = timestamp,
            notificationKey = "notif_1",
            source = IngestionSource.NOTIFICATION
        )
        val sms = NotificationEnvelope(
            packageName = "ADCB",
            title = "SMS",
            text = "Purchase of AED 50.00 at AMAZON AE with card ending 1234.",
            subText = null,
            timestamp = timestamp,
            notificationKey = "sms_4",
            source = IngestionSource.SMS
        )

        useCase.execute(notification)
        useCase.execute(sms)

        assertEquals(1, transactionRepository.insertedTransactions.size)
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


