package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.TransactionType
import javax.inject.Inject

/**
 * Resolves transaction type based on action keywords in text.
 */
class TransactionTypeResolver @Inject constructor() {

    private val expenseKeywords = listOf("spent", "paid", "debited", "purchase", "dr")
    private val incomeKeywords = listOf("received", "credited", "deposit", "cr")
    private val refundKeywords = listOf("refund", "reversed")
    private val transferKeywords = listOf("transfer")

    fun resolve(text: String): TransactionType {
        val lower = text.lowercase()
        return when {
            refundKeywords.any { it in lower } -> TransactionType.REFUND
            incomeKeywords.any { it in lower } -> TransactionType.INCOME
            transferKeywords.any { it in lower } -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE // Default assumption for financial alerts
        }
    }
}
