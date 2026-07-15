package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.Brand
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.CategoryRepository
import com.sherif.ledger.core.domain.repository.MerchantRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.repository.TransactionRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class InsertTransactionUseCaseTest {

    private val transactionRepository = object : TransactionRepository {
        var lastInserted: Transaction? = null
        override fun observeRecentTransactions(limit: Int): Flow<LedgerResult<List<Transaction>>> = flowOf()
        override fun observeTransactionsForAccount(accountId: Long): Flow<LedgerResult<List<Transaction>>> = flowOf()
        override fun observeTransactionsBetween(start: Instant, end: Instant): Flow<LedgerResult<List<Transaction>>> = flowOf()
        override suspend fun getTransactionById(id: Long): LedgerResult<Transaction> = LedgerResult.Failure(LedgerError.Unknown("Not implemented in test"))
        override suspend fun insertTransaction(transaction: Transaction): LedgerResult<Long> {
            if (transaction.fingerprint == "duplicate") return LedgerResult.Failure(LedgerError.DuplicateTransaction)
            lastInserted = transaction
            return LedgerResult.Success(1L)
        }
        override suspend fun deleteTransaction(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override suspend fun updateNote(id: Long, note: String?): LedgerResult<Unit> = LedgerResult.Success(Unit)
        override fun observeAllTransactions(): Flow<LedgerResult<List<Transaction>>> = flowOf(LedgerResult.Success(emptyList()))
        override suspend fun countTransactionsByOrigin(packageName: String, cardTail: String): List<com.sherif.ledger.core.domain.repository.AccountOriginCount> = emptyList()
        override suspend fun reassignTransactions(fromAccountId: Long, packageName: String, cardTail: String, toAccountId: Long): Int = 0
    }

    private val accountRepository = object : AccountRepository {
        var updatedAccount: Account? = null
        override fun observeAllAccounts(): Flow<LedgerResult<List<Account>>> = flowOf()
        override suspend fun getAccountById(id: Long): LedgerResult<Account> {
            return if (id == 1L) {
                LedgerResult.Success(Account(1L, "Test", AccountType.CHECKING, Money(5000L, CurrencyCode.AED), null, null))
            } else {
                LedgerResult.Failure(LedgerError.AccountNotFound)
            }
        }
        override suspend fun insertAccount(account: Account): LedgerResult<Long> = LedgerResult.Success(1L)
        override suspend fun updateAccount(account: Account): LedgerResult<Unit> {
            updatedAccount = account
            return LedgerResult.Success(Unit)
        }
        override suspend fun deleteAccount(id: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
    }

    private val merchantRepository = object : MerchantRepository {
        override suspend fun getAllBrands(): LedgerResult<List<Brand>> = LedgerResult.Success(emptyList())
        override suspend fun getBrandByAlias(rawText: String): LedgerResult<Brand> {
            return if (rawText == "Amazon") {
                LedgerResult.Success(Brand(10L, "Amazon", "amazon", null))
            } else {
                LedgerResult.Failure(LedgerError.Unknown(""))
            }
        }
        override suspend fun insertBrand(brand: Brand): LedgerResult<Long> = LedgerResult.Success(11L)
        override suspend fun registerAlias(rawText: String, brandId: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
    }

    private val transactionRunner = object : TransactionRunner {
        override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
    }

    private val useCase = InsertTransactionUseCase(
        transactionRepository = transactionRepository,
        accountRepository = accountRepository,
        transactionRunner = transactionRunner,
        validator = com.sherif.ledger.core.domain.service.transaction.TransactionValidator(),
        fingerprintGenerator = com.sherif.ledger.core.domain.service.transaction.FingerprintGenerator(),
        merchantResolver = com.sherif.ledger.core.domain.service.transaction.MerchantResolver(merchantRepository),
        categoryResolver = com.sherif.ledger.core.domain.service.transaction.CategoryResolver()
    )

    @Test
    fun `successful insertion never touches account balance`() = runBlocking {
        // Phase 9: InsertTransactionUseCase no longer mutates any account balance.
        // The persisted transaction is the complete effect of an insert; balance
        // is always derived by replay (AccountBalanceService), never cached here.
        val params = InsertTransactionUseCase.Params(
            accountId = 1L,
            amountMinor = 1000L,
            currencyCode = CurrencyCode.AED,
            type = TransactionType.EXPENSE,
            timestamp = Instant.now(),
            source = IngestionSource.MANUAL,
            rawMerchantText = "Amazon"
        )

        val result = useCase.execute(params)

        assertTrue(result is LedgerResult.Success)
        assertEquals("InsertTransactionUseCase must never call updateAccount", null, accountRepository.updatedAccount)
        assertEquals(10L, (result as LedgerResult.Success).data.brandId)
        assertEquals(1L, result.data.categoryId) // Amazon -> Shopping (1)
    }

    @Test
    fun `duplicate detection returns error`() = runBlocking {
        // We simulate duplicate by tweaking params to generate a specific fingerprint if possible, 
        // or just force the repo to return duplicate for any call in this test.
        // For simplicity, I'll modify the fake repo to use a flag.
        
        val params = InsertTransactionUseCase.Params(
            accountId = 1L,
            amountMinor = 1000L,
            currencyCode = CurrencyCode.AED,
            type = TransactionType.EXPENSE,
            timestamp = Instant.ofEpochMilli(0), // predictable fingerprint
            source = IngestionSource.MANUAL,
            rawMerchantText = "Amazon"
        )
        
        // Mocking fingerprint "duplicate"
        val duplicateParams = params.copy(rawMerchantText = "DUPLICATE_TEXT") 
        // Since I can't easily predict the hash here, I'll just change the mock to check for "Amazon"
        
        val result = useCase.execute(params) // First one succeeds
        
        // Change mock to return duplicate
        val repoDuplicate = object : TransactionRepository by transactionRepository {
            override suspend fun insertTransaction(transaction: Transaction): LedgerResult<Long> = 
                LedgerResult.Failure(LedgerError.DuplicateTransaction)
        }
        val useCaseWithDuplicate = InsertTransactionUseCase(
            transactionRepository = repoDuplicate,
            accountRepository = accountRepository,
            transactionRunner = transactionRunner,
            validator = com.sherif.ledger.core.domain.service.transaction.TransactionValidator(),
            fingerprintGenerator = com.sherif.ledger.core.domain.service.transaction.FingerprintGenerator(),
            merchantResolver = com.sherif.ledger.core.domain.service.transaction.MerchantResolver(merchantRepository),
            categoryResolver = com.sherif.ledger.core.domain.service.transaction.CategoryResolver()
        )
        
        val resultDuplicate = useCaseWithDuplicate.execute(params)
        assertTrue(resultDuplicate is LedgerResult.Failure)
        assertEquals(LedgerError.DuplicateTransaction, (resultDuplicate as LedgerResult.Failure).error)
    }

    @Test
    fun `invalid account returns error`() = runBlocking {
        val params = InsertTransactionUseCase.Params(
            accountId = 99L,
            amountMinor = 1000L,
            currencyCode = CurrencyCode.AED,
            type = TransactionType.EXPENSE,
            timestamp = Instant.now(),
            source = IngestionSource.MANUAL,
            rawMerchantText = "Amazon"
        )

        val result = useCase.execute(params)
        assertTrue(result is LedgerResult.Failure)
        assertEquals(LedgerError.AccountNotFound, (result as LedgerResult.Failure).error)
    }
}


