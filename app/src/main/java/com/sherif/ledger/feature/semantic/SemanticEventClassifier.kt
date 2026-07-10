package com.sherif.ledger.feature.semantic

import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope

/**
 * Phase 7 — Semantic Event Resolution.
 *
 * The shift this phase encodes: stop reasoning about individual SMS messages and
 * start reasoning about real-world financial actions. One action ("I paid my
 * credit card") emits several notifications — an ADCB debit and a FAB "payment
 * received". Only the FIRST is money moving; the rest acknowledge it. Judging each
 * message in isolation double-counts. This classifier asks the chain-level
 * question instead: does this message represent NEW money movement, or is it ABOUT
 * movement that already happened?
 *
 * This is the single seam a future local model (Gemma/Phi) will replace. Downstream
 * code depends only on [SemanticClass], never on how the class was derived, so
 * swapping the deterministic classifier for an on-device model changes nothing else.
 */
interface SemanticEventClassifier {
    fun classify(envelope: NotificationEnvelope, candidate: TransactionCandidate?): SemanticClassification
}

/** The semantic category of a notification, independent of bank templates. */
enum class SemanticClass {
    /** Money actually moved. Persist as a transaction. */
    FINANCIAL_EVENT,

    /** Acknowledgement of an earlier event. Attach, never persist. */
    FINANCIAL_CONFIRMATION,

    /** Statement, reminder, balance/credit-limit update. Non-transactional. */
    FINANCIAL_INFORMATION,

    /** Cannot be classified with confidence. */
    UNKNOWN,
}

/**
 * A classification with its evidence, so the Developer Console can show why a
 * message was treated as a confirmation rather than an event. [confidence] is 0..100.
 */
data class SemanticClassification(
    val semanticClass: SemanticClass,
    val confidence: Int,
    val reasoning: List<String>,
    val matchedSignals: List<String> = emptyList(),
)

