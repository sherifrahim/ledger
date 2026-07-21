package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionOrigin
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.FinancialEventRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.repository.TransactionRunner
import com.sherif.ledger.core.domain.service.event.FinancialEventFactory
import com.sherif.ledger.core.domain.service.transaction.CategoryResolver
import com.sherif.ledger.core.domain.service.transaction.FingerprintGenerator
import com.sherif.ledger.core.domain.service.transaction.MerchantResolver
import com.sherif.ledger.core.domain.service.transaction.TransactionValidator
import java.time.Instant
import javax.inject.Inject

/**
 * Orchestrator Use Case for inserting a new transaction into the system.
 * Coordinates validation, identity generation, resolution services, and atomic persistence.
 *
 * Phase 9: does NOT write or mutate any account balance. Balances are never
 * cached — they are always derived by replaying persisted transactions (see
 * [com.sherif.ledger.core.domain.service.transaction.AccountBalanceService]).
 * Persisting a transaction here is the complete effect of an insert; there is no
 * second, independently-maintained number to keep in sync.
 */
class InsertTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val transactionRunner: TransactionRunner,
    private val validator: TransactionValidator,
    private val fingerprintGenerator: FingerprintGenerator,
    private val merchantResolver: MerchantResolver,
    private val categoryResolver: CategoryResolver,
    private val financialEventRepository: FinancialEventRepository,
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
        val insertOutcome = transactionRunner.runInTransaction {
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
                cardTail = params.cardTail,
                fingerprint = fingerprint,
                transferDirection = params.transferDirection,
                origin = params.origin,
            )
            
            LedgerLogger.pipeline("Persistence", "LINEAGE: Domain -> Merchant=${transaction.rawText}, Amount=${transaction.amount.minorUnits}, Fingerprint=${transaction.fingerprint.take(8)}")
            com.sherif.ledger.core.common.logging.LedgerLogger.pipeline("Pipeline", "Persistence Input: ${params.amountMinor} ${params.currencyCode}")

            // Persistence. This is the ONLY write. No account balance is mutated —
            // the persisted transaction IS the complete effect of this insert.
            val insertResult = transactionRepository.insertTransaction(transaction)
            if (insertResult is LedgerResult.Failure) {
                return@runInTransaction insertResult as LedgerResult<Transaction>
            }
            
            val newTransactionId = (insertResult as LedgerResult.Success).data
            val persistedTransaction = transaction.copy(id = newTransactionId)

            LedgerResult.Success(persistedTransaction).also {
                com.sherif.ledger.core.common.logging.LedgerLogger.pipeline(
    "Insert",
    "SUCCESS id=${it.data.id}, amount=${params.amountMinor}, type=${params.type}, merchant=${params.rawMerchantText}"
)            }
        }

        // Dual-write (ADR-0001, Milestone 2 Track B). Record the canonical mirror
        // FinancialEvent AFTER the transaction has committed — deliberately OUTSIDE
        // the atomic boundary above. A failure here must never roll back or fail the
        // insert: Financial Truth is the persisted transaction; the event is an
        // idempotent coexistence projection (deduped by fingerprint). Working
        // business logic is untouched (ADR-0000).
        if (insertOutcome is LedgerResult.Success) {
            recordMirrorEvent(insertOutcome.data)
        }
        return insertOutcome
    }

    private suspend fun recordMirrorEvent(transaction: Transaction) {
        try {
            financialEventRepository.record(FinancialEventFactory.mirrorOf(transaction))
        } catch (e: Exception) {
            LedgerLogger.e(
                "Dual-write: mirror FinancialEvent failed for tx=${transaction.id} " +
                    "(transaction is unaffected)",
                e,
            )
        }
    }

    data class Params(
        val accountId: Long,
        val amountMinor: Long,
        val currencyCode: CurrencyCode,
        val type: TransactionType,
        val timestamp: Instant,
        val source: IngestionSource,
        val rawMerchantText: String,
        val cardTail: String? = null,
        val transferDirection: TransferDirection? = null,
        val origin: TransactionOrigin? = null,
    )
}

