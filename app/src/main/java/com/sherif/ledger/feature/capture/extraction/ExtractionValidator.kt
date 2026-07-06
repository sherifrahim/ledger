package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.core.domain.model.TransactionCandidate
import javax.inject.Inject

/**
 * Deterministic safety rails applied to EVERY extraction result. Judges only the
 * shape and sanity of fields. Never persists, reconciles, or fingerprints.
 */
class ExtractionValidator @Inject constructor() {

    private val minConfidence = 70
    private val tailRegex = Regex("^\\d{4,6}$")

    fun validate(result: ExtractionResult.Extracted): ValidationOutcome {
        val reasons = mutableListOf<String>()
        val c = result.candidate

        if (result.confidence.value < minConfidence) {
            reasons += "confidence ${result.confidence.value} below threshold $minConfidence"
        }
        val amount = c.amountMinor
        if (amount == null || amount <= 0L) reasons += "amount missing or non-positive"
        if (c.currencyCode == null) reasons += "currency unsupported"
        if (c.transactionType == null) reasons += "transaction type unknown"
        c.merchantName?.let { m -> if (m.isBlank() || m.length > 64) reasons += "merchant invalid (length ${m.length})" }
        c.accountHint?.let { tail -> if (!tailRegex.matches(tail)) reasons += "tail not 4-6 digits: $tail" }

        return if (reasons.isEmpty()) ValidationOutcome.Valid(result)
        else ValidationOutcome.Invalid(result, reasons)
    }

    sealed interface ValidationOutcome {
        val candidate: TransactionCandidate
        data class Valid(val result: ExtractionResult.Extracted) : ValidationOutcome {
            override val candidate: TransactionCandidate get() = result.candidate
        }
        data class Invalid(val result: ExtractionResult.Extracted, val reasons: List<String>) : ValidationOutcome {
            override val candidate: TransactionCandidate get() = result.candidate
        }
    }
}
