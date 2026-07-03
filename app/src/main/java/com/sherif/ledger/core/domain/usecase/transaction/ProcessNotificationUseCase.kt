package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.usecase.account.EnsureDefaultAccountUseCase
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.notification.NotificationFilter
import com.sherif.ledger.feature.capture.parsing.ParseResult
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import com.sherif.ledger.feature.capture.reconciliation.ReconciliationEngine
import com.sherif.ledger.feature.capture.reconciliation.ReconciliationResult
import kotlinx.coroutines.flow.first
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Orchestrates the end-to-end ingestion flow from a raw notification to a persisted transaction.
 */
class ProcessNotificationUseCase @Inject constructor(
    private val filter: NotificationFilter,
    private val parserRegistry: ParserRegistry,
    private val reconciliationEngine: ReconciliationEngine,
    private val transactionRepository: TransactionRepository,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val ensureDefaultAccountUseCase: EnsureDefaultAccountUseCase
) {
    suspend fun execute(envelope: NotificationEnvelope) {
        LedgerLogger.pipeline("Capture", "Received notification from ${envelope.packageName}")

        // 1. Filter
        if (!filter.shouldProcess(envelope)) {
            LedgerLogger.pipeline("Filter", "Ignored: ${envelope.packageName}")
            return
        }
        LedgerLogger.pipeline("Filter", "Accepted: ${envelope.packageName}")

        // 2. Parse
        val parseResult = parserRegistry.parse(envelope)
        val candidate = when (parseResult) {
            is ParseResult.Success -> {
                LedgerLogger.pipeline("Parser", "Matched: ${parseResult.candidate.merchantName}")
                parseResult.candidate
            }
            ParseResult.Ignore -> {
                LedgerLogger.pipeline("Parser", "Intentionally ignored (OTP/Statement)")
                return
            }
            is ParseResult.Failed -> {
                LedgerLogger.pipeline("Parser", "Failed: ${parseResult.reason}")
                return
            }
        }

        // 3. Reconcile
        val start = candidate.timestamp.minus(24, ChronoUnit.HOURS)
        val end = candidate.timestamp.plus(24, ChronoUnit.HOURS)
        
        val existingTransactionsResult = transactionRepository.observeTransactionsBetween(start, end).first()
        val existingTransactions = if (existingTransactionsResult is LedgerResult.Success) {
            existingTransactionsResult.data
        } else emptyList()

        val reconciliationResult = reconciliationEngine.reconcile(candidate, existingTransactions)

        // 4. Persistence
        when (reconciliationResult) {
            is ReconciliationResult.New -> {
                LedgerLogger.pipeline("Reconciliation", "Classified as NEW transaction")
                
                // Ensure a valid account exists before insertion
                val accountId = candidate.accountId ?: ensureDefaultAccountUseCase.execute()
                
                val params = InsertTransactionUseCase.Params(
                    accountId = accountId,
                    amountMinor = candidate.amountMinor ?: 0L,
                    currencyCode = candidate.currencyCode ?: CurrencyCode.AED,
                    type = candidate.transactionType ?: TransactionType.EXPENSE,
                    timestamp = candidate.timestamp,
                    source = candidate.source,
                    rawMerchantText = candidate.merchantName ?: "Unknown"
                )
                val result = insertTransactionUseCase.execute(params)
                if (result is LedgerResult.Success) {
                    LedgerLogger.pipeline("Persistence", "Transaction inserted successfully: ${result.data.id}")
                } else if (result is LedgerResult.Failure) {
                    LedgerLogger.e("Persistence failed: ${result.error}")
                }
            }
            is ReconciliationResult.Updated -> {
                LedgerLogger.pipeline("Reconciliation", "Classified as UPDATE for ${reconciliationResult.existingTransactionId}")
            }
            is ReconciliationResult.Duplicate -> {
                LedgerLogger.pipeline("Reconciliation", "Ignored: Duplicate of ${reconciliationResult.existingTransactionId}")
            }
            ReconciliationResult.Ignored -> {
                LedgerLogger.pipeline("Reconciliation", "Ignored by engine")
            }
        }
    }
}
