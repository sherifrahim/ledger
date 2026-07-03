package com.sherif.ledger.feature.capture.reconciliation

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.service.transaction.FingerprintGenerator
import com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase
import java.time.Duration
import javax.inject.Inject

/**
 * Engine responsible for reconciling transaction candidates against existing data.
 * Implements multi-signal confidence scoring to detect duplicates and updates.
 */
class ReconciliationEngine @Inject constructor(
    private val fingerprintGenerator: FingerprintGenerator
) {
    /**
     * Reconciles a candidate against a list of nearby existing transactions.
     */
    fun reconcile(candidate: TransactionCandidate, existingTransactions: List<Transaction>): ReconciliationResult {
        val candidateFingerprint = generateFingerprint(candidate)
        LedgerLogger.pipeline("Reconciliation", "Fingerprint: $candidateFingerprint")
        
        // 1. Exact Fingerprint Match (100% confidence)
        val exactMatch = existingTransactions.find { it.fingerprint == candidateFingerprint }
        if (exactMatch != null) {
            LedgerLogger.pipeline("Reconciliation", "Exact match found (Fingerprint)")
            return ReconciliationResult.Duplicate(exactMatch.id)
        }

        // 2. Fuzzy Matching Signals
        val matches = existingTransactions.map { existing ->
            calculateConfidenceWithDetails(candidate, existing) to existing
        }.filter { it.first.score >= 90 } // Min threshold for reconciliation
         .sortedByDescending { it.first.score }

        val bestMatch = matches.firstOrNull()

        return when {
            bestMatch == null -> {
                LedgerLogger.pipeline("Reconciliation", "No match found. Score < 90")
                ReconciliationResult.New(candidate)
            }
            
            // Logic for Updated vs Duplicate (e.g. amount change or status change)
            bestMatch.first.score >= 98 -> {
                LedgerLogger.pipeline("Reconciliation", "Match Score: ${bestMatch.first.score}. Reason: ${bestMatch.first.details}")
                ReconciliationResult.Duplicate(bestMatch.second.id)
            }
            
            else -> {
                LedgerLogger.pipeline("Reconciliation", "Match Score: ${bestMatch.first.score}. Reason: ${bestMatch.first.details}")
                ReconciliationResult.Updated(bestMatch.second.id, candidate)
            }
        }
    }

    private data class ScoreResult(val score: Int, val details: String)

    private fun calculateConfidenceWithDetails(candidate: TransactionCandidate, existing: Transaction): ScoreResult {
        // Absolute Reject: Different merchants or currencies
        if (candidate.merchantName != null && candidate.merchantName != existing.rawText) {
            return ScoreResult(0, "Merchant mismatch") 
        }
        
        if (candidate.currencyCode != existing.amount.currencyCode) return ScoreResult(0, "Currency mismatch")
        if (candidate.accountId != null && candidate.accountId != existing.accountId) return ScoreResult(0, "Account mismatch")

        var score = 0
        val details = mutableListOf<String>()
        
        // Amount Match
        if (candidate.amountMinor == existing.amount.minorUnits) {
            score += 70
            details.add("Amount: 70")
        }

        // Time Proximity
        val timeDrift = Duration.between(candidate.timestamp, existing.timestamp).abs().toMinutes()
        when {
            timeDrift <= 1 -> { score += 30; details.add("Time: 30") }
            timeDrift <= 5 -> { score += 25; details.add("Time: 25") }
            timeDrift <= 30 -> { score += 15; details.add("Time: 15") }
            timeDrift <= 1440 -> { score += 5; details.add("Time: 5") }
        }

        // Transaction Type
        if (candidate.transactionType == existing.type) {
            score += 10
            details.add("Type: 10")
        }

        return ScoreResult(score.coerceIn(0, 100), details.joinToString(", "))
    }

    private fun generateFingerprint(candidate: TransactionCandidate): String {
        // Helper to map Candidate to UseCase Params for fingerprinting
        val params = InsertTransactionUseCase.Params(
            accountId = candidate.accountId ?: 0L,
            amountMinor = candidate.amountMinor ?: 0L,
            currencyCode = candidate.currencyCode ?: com.sherif.ledger.core.domain.model.CurrencyCode.AED,
            type = candidate.transactionType ?: com.sherif.ledger.core.domain.model.TransactionType.EXPENSE,
            timestamp = candidate.timestamp,
            source = candidate.source,
            rawMerchantText = candidate.merchantName ?: ""
        )
        return fingerprintGenerator.generate(params)
    }
}
