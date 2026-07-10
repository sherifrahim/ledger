package com.sherif.ledger.feature.semantic

import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import javax.inject.Inject

/**
 * Deterministic, bank-agnostic semantic classifier (Phase 7). No AI, no bank
 * templates — it classifies by MEANING using semantic phrase families that hold
 * across UAE and Indian banks.
 *
 * Core rule for the double-count bug: a message is a CONFIRMATION when it
 * acknowledges a payment ("payment received / processed / acknowledged", "funds
 * received", "outstanding updated") AND does not itself describe money leaving an
 * account in this message (no strong movement verb like "debited"/"spent"). Such a
 * message is the bank acknowledging money that already moved elsewhere; persisting
 * it double-counts.
 *
 * A future Gemma/Phi model implements the same [SemanticEventClassifier] interface
 * and replaces this class without touching downstream code.
 */
class DeterministicSemanticClassifier @Inject constructor() : SemanticEventClassifier {

    // Acknowledgement of an EARLIER payment/event. Bank-agnostic, meaning-based.
    private val confirmationSignals = listOf(
        "payment received", "funds received", "amount received",
        "payment processed", "has been processed", "payment has been processed",
        "processed successfully", "successfully processed", "processed on",
        "payment successful", "payment successfully", "bill payment successful",
        "card payment received", "credit card payment received",
        "thank you for your payment", "thank you for paying",
        "payment acknowledged", "acknowledge receipt", "receipt of your payment",
        "we have received", "successfully received",
        "outstanding balance updated", "outstanding updated", "outstanding balance",
        "balance restored", "credit limit restored", "available credit restored",
        "available credit updated", "credit limit updated",
        "payment towards", "received against", "credited to your card",
        "payment credited", "payment posted", "minimum amount due",
    )

    // Pure information: statements, reminders, balance/limit notices. Not money moving.
    private val informationSignals = listOf(
        "statement is ready", "statement generated", "e-statement",
        "statement available", "monthly statement", "view your statement",
        "available balance is", "avl. bal", "avl bal", "available balance:",
        "current balance", "balance enquiry", "balance inquiry",
        "credit limit is", "available limit", "reminder", "due date",
        "is due on", "will be due", "kindly pay", "please pay your",
    )

    // Strong movement verbs: money leaving/entering in THIS message = an EVENT.
    // If any of these is present, the message describes an actual movement and is
    // NOT a mere acknowledgement, even if it also contains "payment".
    private val movementVerbs = listOf(
        "debited", "spent", "withdrawn", "deducted", "charged",
        "purchase of", "used for", "used at", "swiped",
        "transferred", "sent to", "paid to", "payment of",
    )

    override fun classify(
        envelope: NotificationEnvelope,
        candidate: TransactionCandidate?,
    ): SemanticClassification {
        val text = "${envelope.title} ${envelope.text}".lowercase()

        val movement = movementVerbs.filter { it in text }
        val confirmations = confirmationSignals.filter { it in text }
        val information = informationSignals.filter { it in text }

        // 1. CONFIRMATION: acknowledgement present, and no strong movement verb that
        //    would indicate money leaving in THIS message. This is the FAB case:
        //    "payment ... has been processed" with no "debited/spent".
        //    Note: "payment of"/"paid to" are treated as movement (outgoing), so a
        //    genuine outgoing payment is NOT misread as a confirmation.
        val movementExcludingPaymentOf = movement.filterNot { it == "payment of" }
        if (confirmations.isNotEmpty() && movementExcludingPaymentOf.isEmpty()) {
            val conf = when {
                confirmations.size >= 2 -> 95
                else -> 90
            }
            return SemanticClassification(
                semanticClass = SemanticClass.FINANCIAL_CONFIRMATION,
                confidence = conf,
                reasoning = listOf(
                    "Acknowledgement language present without a money-movement verb",
                    "Represents receipt/processing of an earlier payment, not new movement",
                ),
                matchedSignals = confirmations,
            )
        }

        // 2. INFORMATION: statement/balance/limit/reminder with no payment ack and
        //    no movement verb. Non-transactional.
        if (information.isNotEmpty() && confirmations.isEmpty() && movement.isEmpty()) {
            return SemanticClassification(
                semanticClass = SemanticClass.FINANCIAL_INFORMATION,
                confidence = 88,
                reasoning = listOf("Statement / balance / limit / reminder with no money movement"),
                matchedSignals = information,
            )
        }

        // 3. EVENT: a movement verb (or an extracted candidate) with no dominant
        //    acknowledgement. Money actually moved.
        if (movement.isNotEmpty() || candidate != null) {
            return SemanticClassification(
                semanticClass = SemanticClass.FINANCIAL_EVENT,
                confidence = if (movement.isNotEmpty()) 90 else 75,
                reasoning = listOf(
                    if (movement.isNotEmpty()) "Money-movement verb present: ${movement.first()}"
                    else "Extractor produced a transaction candidate",
                ),
                matchedSignals = movement,
            )
        }

        // 4. UNKNOWN.
        return SemanticClassification(
            semanticClass = SemanticClass.UNKNOWN,
            confidence = 0,
            reasoning = listOf("No decisive semantic signal"),
        )
    }
}

