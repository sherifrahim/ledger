package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.repository.TransactionRunner
import com.sherif.ledger.core.domain.service.transaction.BalanceCalculator
import com.sherif.ledger.core.domain.service.transaction.CategoryResolver
import com.sherif.ledger.core.domain.service.transaction.FingerprintGenerator
import com.sherif.ledger.core.domain.service.transaction.MerchantResolver
import com.sherif.ledger.core.domain.service.transaction.TransactionValidator
import java.time.Instant
import javax.inject.Inject

/**
 * Orchestrator Use Case for inserting a new transaction into the system.
 * Coordinates validation, identity generation, resolution services, and atomic persistence.
 */
class InsertTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val transactionRunner: TransactionRunner,
    private val validator: TransactionValidator,
    private val fingerprintGenerator: FingerprintGenerator,
    private val merchantResolver: MerchantResolver,
    private val categoryResolver: CategoryResolver,
    private val balanceCalculator: BalanceCalculator
) {
    init {
        com.sherif.ledger.core.common.logging.LedgerLogger.d("EXECUTING: InsertTransactionUseCase")
    }

    suspend fun execute(params: Params): LedgerResult<Transaction> {
        val traceId = LedgerLogger.getTraceId()
        LedgerLogger.d("InsertTransactionUseCase.execute(params=$params)")
        LedgerLogger.pipeline("Persistence", "LINEAGE: Params -> AccountId=${params.accountId}, Amount=${params.amountMinor}, Type=${params.type}, Merchant=${params.rawMerchantText}")

        // 1. Static Input Validation
        val inputError = validator.validateInput(params)
        if (inputError != null) return LedgerResult.Failure(inputError)

        // 2. Account Context Verification
        val accountResult = accountRepository.getAccountById(params.accountId)
        if (accountResult is LedgerResult.Failure) return LedgerResult.Failure(LedgerError.AccountNotFound)
        val account = (accountResult as LedgerResult.Success).data
        
        val accountError = validator.validateAccount(account, params)
        if (accountError != null) return LedgerResult.Failure(accountError)

        // 3. Identity and Resolution
        val fingerprint = fingerprintGenerator.generate(params)
        val brandId = merchantResolver.resolve(params.rawMerchantText)
        val categoryId = categoryResolver.resolve(params.rawMerchantText, brandId)

        com.sherif.ledger.core.common.logging.LedgerLogger.pipeline("Pipeline", "Resolved: Merchant=$brandId, Category=$categoryId")

        // 4. Atomic Persistence Boundary
        return transactionRunner.runInTransaction {
            val transaction = Transaction(
                id = 0,
                accountId = params.accountId,
                brandId = brandId,
                categoryId = categoryId,
                amount = Money(params.amountMinor, params.currencyCode),
                type = params.type,
                timestamp = params.timestamp,
                source = params.source,
                rawText = params.rawMerchantText,
                fingerprint = fingerprint
            )
            
            LedgerLogger.pipeline("Persistence", "LINEAGE: Domain -> Merchant=${transaction.rawText}, Amount=${transaction.amount.minorUnits}, Fingerprint=${transaction.fingerprint.take(8)}")
            com.sherif.ledger.core.common.logging.LedgerLogger.pipeline("Pipeline", "Persistence Input: ${params.amountMinor} ${params.currencyCode}")

            // Persistence
            val insertResult = transactionRepository.insertTransaction(transaction)
            if (insertResult is LedgerResult.Failure) {
                return@runInTransaction insertResult as LedgerResult<Transaction>
            }
            
            val newTransactionId = (insertResult as LedgerResult.Success).data
            val persistedTransaction = transaction.copy(id = newTransactionId)

            // Balance Adjustment
            val updatedBalance = balanceCalculator.calculate(account.balance, persistedTransaction)
            val updateResult = accountRepository.updateAccount(account.copy(balance = updatedBalance))
            
            if (updateResult is LedgerResult.Failure) {
                // Room withTransaction will rollback on exception
                throw IllegalStateException("Critical failure: Could not update account balance for transaction $newTransactionId")
            }

            LedgerResult.Success(persistedTransaction).also {
                com.sherif.ledger.core.common.logging.LedgerLogger.d("InsertTransactionUseCase: RETURNING Success(id=${it.data.id})")
            }
        }
    }

    data class Params(
        val accountId: Long,
        val amountMinor: Long,
        val currencyCode: CurrencyCode,
        val type: TransactionType,
        val timestamp: Instant,
        val source: IngestionSource,
        val rawMerchantText: String
    )
}
