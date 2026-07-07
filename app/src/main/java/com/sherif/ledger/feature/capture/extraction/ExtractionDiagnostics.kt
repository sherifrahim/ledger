package com.sherif.ledger.feature.capture.extraction

/**
 * Observability-only record of a single extraction attempt.
 *
 * Purely for debugging and the Developer Console. MUST NEVER affect
 * reconciliation, persistence, or any financial decision. Nothing downstream of
 * the registry reads it; it is emitted to logs / the dev console and discarded.
 */
data class ExtractionDiagnostics(
    val extractor: String,
    val decision: String,
    val category: String,
    val durationMs: Long,
    val confidence: Int,
    val validationPassed: Boolean,
    val rejectedReason: String? = null,
    val positiveEvidence: List<String> = emptyList(),
    val negativeEvidence: List<String> = emptyList(),
    val reasoning: List<String> = emptyList(),
    val detectedIntent: String = "",
    val detectedType: String = "",
    val matchedLibraryEntries: List<String> = emptyList(),
    val confidenceBreakdown: String = "",
) {
    companion object {
        const val DECISION_EXTRACTED = "Extracted"
        const val DECISION_IGNORED = "Ignored"
        const val DECISION_NOT_APPLICABLE = "NotApplicable"
    }
}
