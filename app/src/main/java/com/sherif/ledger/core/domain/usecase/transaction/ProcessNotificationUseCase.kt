package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.feature.capture.source.SourceChannel
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
    init {
        com.sherif.ledger.core.common.logging.LedgerLogger.d("EXECUTING: ProcessNotificationUseCase")
    }

    suspend fun execute(
        envelope: NotificationEnvelope,
        channel: SourceChannel = SourceChannel.NOTIFICATION,
    ) {
        val traceId = envelope.notificationKey
        LedgerLogger.setTraceId(traceId)

        LedgerLogger.d("ProcessNotificationUseCase.execute(envelope=$envelope)")
        LedgerLogger.pipeline("Capture", "channel=$channel source=${envelope.packageName}")

        // 1. Filter
        if (!filter.shouldProcess(envelope)) {
            LedgerLogger.d("ProcessNotificationUseCase: REJECTED by Filter (Sender=${envelope.packageName}, Source=${envelope.source})")
            LedgerLogger.pipeline("Filter", "Ignored: ${envelope.packageName}")
            return
        }
        LedgerLogger.d("ProcessNotificationUseCase: ACCEPTED by Filter")
        LedgerLogger.pipeline("Filter", "Accepted: ${envelope.packageName}")

        // 2. Parse
        val parseResult = parserRegistry.parse(envelope)
        val candidate = when (parseResult) {
            is ParseResult.Success -> {
                LedgerLogger.d("ProcessNotificationUseCase: PARSED candidate=${parseResult.candidate}")
                LedgerLogger.pipeline("Parser", "Matched: ${parseResult.candidate.merchantName}")
                parseResult.candidate
            }
            ParseResult.Ignore -> {
                LedgerLogger.d("ProcessNotificationUseCase: IGNORED (OTP/Statement)")
                LedgerLogger.pipeline("Parser", "Intentionally ignored (OTP/Statement)")
                return
            }
            is ParseResult.Failed -> {
                LedgerLogger.d("ProcessNotificationUseCase: PARSE FAILED reason=${parseResult.reason}")
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
        LedgerLogger.d("ProcessNotificationUseCase: RECONCILIATION RESULT=${reconciliationResult::class.simpleName}")

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
                    source = when(envelope.source) {
                        com.sherif.ledger.feature.capture.notification.IngestionSource.SMS -> com.sherif.ledger.core.domain.model.IngestionSource.SMS
                        else -> com.sherif.ledger.core.domain.model.IngestionSource.NOTIFICATION
                    },
                    rawMerchantText = candidate.merchantName ?: "Unknown"
                )
                LedgerLogger.d("ProcessNotificationUseCase: PERSISTING params=$params")
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
        
        LedgerLogger.setTraceId(null)
    }
}
