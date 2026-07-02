package com.sherif.ledger.feature.capture.reconciliation

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
        
        // 1. Exact Fingerprint Match (100% confidence)
        val exactMatch = existingTransactions.find { it.fingerprint == candidateFingerprint }
        if (exactMatch != null) {
            return ReconciliationResult.Duplicate(exactMatch.id)
        }

        // 2. Fuzzy Matching Signals
        val matches = existingTransactions.map { existing ->
            calculateConfidence(candidate, existing) to existing
        }.filter { it.first >= 90 } // Min threshold for reconciliation
         .sortedByDescending { it.first }

        val bestMatch = matches.firstOrNull()

        return when {
            bestMatch == null -> ReconciliationResult.New(candidate)
            
            // Logic for Updated vs Duplicate (e.g. amount change or status change)
            bestMatch.first >= 98 -> ReconciliationResult.Duplicate(bestMatch.second.id)
            
            else -> ReconciliationResult.Updated(bestMatch.second.id, candidate)
        }
    }

    private fun calculateConfidence(candidate: TransactionCandidate, existing: Transaction): Int {
        // Absolute Reject: Different merchants or currencies
        if (candidate.merchantName != null && candidate.merchantName != existing.rawText) {
            // Note: In a real system, we'd use normalized brand IDs here.
            // For DFC-09, we stick to deterministic string comparison or assume brand resolution happened.
            return 0 
        }
        
        if (candidate.currencyCode != existing.amount.currencyCode) return 0
        if (candidate.accountId != null && candidate.accountId != existing.accountId) return 0

        var score = 0
        
        // Amount Match
        if (candidate.amountMinor == existing.amount.minorUnits) {
            score += 70
        }

        // Time Proximity
        val timeDrift = Duration.between(candidate.timestamp, existing.timestamp).abs().toMinutes()
        when {
            timeDrift <= 1 -> score += 30
            timeDrift <= 5 -> score += 25
            timeDrift <= 30 -> score += 15
            timeDrift <= 1440 -> score += 5 // Within 24 hours
        }

        // Transaction Type
        if (candidate.transactionType == existing.type) {
            score += 10
        }

        return score.coerceIn(0, 100)
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
