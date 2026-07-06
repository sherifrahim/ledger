package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionType

/**
 * Raw semantic fields an extractor pulled from a message, before validation and
 * before conversion into a domain [TransactionCandidate]. All nullable.
 */
data class ExtractedFields(
    val amountMinor: Long? = null,
    val currency: CurrencyCode? = null,
    val merchant: String? = null,
    val transactionType: TransactionType? = null,
    val accountTail: String? = null,
    val cardTail: String? = null,
)

/**
 * Confidence in an extraction, 0..100. Each extractor OWNS and returns its own
 * value; the registry compares whatever each reports.
 */
@JvmInline
value class ExtractionConfidence(val value: Int) {
    init {
        require(value in 0..100) { "Confidence must be 0..100, was $value" }
    }

    operator fun compareTo(other: ExtractionConfidence): Int = value.compareTo(other.value)
}

/**
 * The uniform output contract of EVERY extractor. Reconciliation and everything
 * downstream never know which extractor produced this.
 */
sealed interface ExtractionResult {

    data class Extracted(
        val candidate: TransactionCandidate,
        val confidence: ExtractionConfidence,
        val fields: ExtractedFields,
        val reasoning: List<String>,
        val positiveEvidence: List<String>,
        val extractorName: String,
    ) : ExtractionResult

    /**
     * Recognized as intentionally non-transactional. [category] names the kind
     * (Promotion, OTP, Statement, ...) and [matchedPhrases] lists the evidence,
     * both for diagnostics only. Terminal: never becomes a candidate.
     */
    data class Ignore(
        val reason: String,
        val extractorName: String,
        val category: String = "Unknown",
        val matchedPhrases: List<String> = emptyList(),
        val confidence: Int = 0,
    ) : ExtractionResult

    data class NotApplicable(val reason: String, val extractorName: String) : ExtractionResult
}
