package com.sherif.ledger.feature.semantic

import com.sherif.ledger.feature.capture.extraction.ExtractionRegistry
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope

/**
 * Phase 7 (refined) — Financial Intent Classification.
 *
 * Clean separation of responsibility:
 *  - The EXTRACTOR (ExtractionRegistry) is responsible only for producing
 *    structured data — an [ExtractionRegistry.ExtractionOutcome]. It ALWAYS runs
 *    and NEVER decides routing, including when it ignores or fails a message.
 *  - The CLASSIFIER is responsible only for determining financial INTENT. It
 *    consumes the complete outcome — Success, Ignored, Failed, or Confirmation —
 *    as context, not as a routing decision made on its behalf.
 *  - The ROUTER (ProcessNotificationUseCase) decides behavior from intent alone.
 *
 * Nothing returns early before classification. Even a message the extractor could
 * not parse (Ignored/Failed) still reaches the classifier, because intent is a
 * property of the notification's real-world meaning, not of whether a candidate
 * could be parsed from it — that coupling is exactly what caused the double-count
 * this phase fixes.
 *
 * THE SEAM: this interface is the single point a future on-device model
 * (Gemma/Phi) replaces. It is given the same inputs a deterministic classifier
 * gets — the notification and the extractor's structured output — so nothing
 * downstream changes when the implementation is swapped.
 */
interface FinancialIntentClassifier {
    fun classify(
        envelope: NotificationEnvelope,
        extraction: ExtractionRegistry.ExtractionOutcome,
    ): FinancialIntentResult
}

/** The four intent classes the router consumes. */
enum class FinancialIntent {
    /** Money actually moved. The only class that reaches persistence. */
    FINANCIAL_EVENT,

    /** Acknowledgement of an earlier event. Always routed to confirmation matching. */
    FINANCIAL_CONFIRMATION,

    /** Statement, reminder, balance/limit update. Ignored after diagnostics. */
    FINANCIAL_INFORMATION,

    /** Intent could not be determined. Surfaced through diagnostics only. */
    UNKNOWN,
}

/**
 * A classification with its evidence, so the Developer Console can show why a
 * message was routed as it was. [confidence] is 0..100.
 */
data class FinancialIntentResult(
    val intent: FinancialIntent,
    val confidence: Int,
    val reasoning: List<String>,
    val matchedSignals: List<String> = emptyList(),
)

