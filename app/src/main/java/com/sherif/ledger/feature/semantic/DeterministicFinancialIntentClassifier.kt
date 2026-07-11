package com.sherif.ledger.feature.semantic

import com.sherif.ledger.feature.capture.extraction.ExtractionRegistry
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import javax.inject.Inject

/**
 * Deterministic, bank-agnostic financial intent classifier (Phase 7, refined).
 * No AI, no bank templates — classifies by MEANING using semantic phrase families
 * that hold across UAE and Indian banks.
 *
 * Priority order, deliberately layered:
 *  1-3. Independent text analysis (confirmation / information / movement phrases)
 *       is PRIMARY. This is the classifier's own understanding of the message and
 *       is what fixes the double-count bug: it does not care whether extraction
 *       succeeded.
 *  4-5. The extractor's own outcome is consulted only as a FALLBACK, when the
 *       classifier's own text analysis found no decisive signal. This uses the
 *       registry's structured output as corroborating evidence — never as the
 *       reason for a routing decision the classifier didn't independently reach.
 *  6.   Otherwise UNKNOWN — surfaced through diagnostics only, never persisted.
 *
 * A future Gemma/Phi model implements the same [FinancialIntentClassifier] and
 * replaces this class without touching downstream code.
 */
class DeterministicFinancialIntentClassifier @Inject constructor() : FinancialIntentClassifier {

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
    private val movementVerbs = listOf(
        "debited", "spent", "withdrawn", "deducted", "charged",
        "purchase of", "used for", "used at", "swiped",
        "transferred", "sent to", "paid to", "payment of",
        // Incoming movement (real credits/income are events too).
        "credited to your account", "credited to account", "cr. transaction",
        "cr transaction", "credit of", "deposited", "salary credited",
        "has been credited", "received in your account",
    )

    private val currencyTokens = listOf("aed", "inr", "usd", "rs.", "rs ", "dirham")

    override fun classify(
        envelope: NotificationEnvelope,
        extraction: ExtractionRegistry.ExtractionOutcome,
    ): FinancialIntentResult {
        val text = "${envelope.title} ${envelope.text}".lowercase()

        val movement = movementVerbs.filter { it in text }
        val confirmations = confirmationSignals.filter { it in text }
        val information = informationSignals.filter { it in text }
        val movementExcludingPaymentOf = movement.filterNot { it == "payment of" }

        // 1. PRIMARY: our own confirmation detection. This is the double-count fix —
        //    it fires regardless of what extraction concluded about this message.
        if (confirmations.isNotEmpty() && movementExcludingPaymentOf.isEmpty()) {
            return FinancialIntentResult(
                intent = FinancialIntent.FINANCIAL_CONFIRMATION,
                confidence = if (confirmations.size >= 2) 95 else 90,
                reasoning = listOf(
                    "Acknowledgement language present without a money-movement verb",
                    "Represents receipt/processing of an earlier payment, not new movement",
                ),
                matchedSignals = confirmations,
            )
        }

        // 2. PRIMARY: pure information (statement/balance/limit/reminder).
        if (information.isNotEmpty() && confirmations.isEmpty() && movement.isEmpty()) {
            return FinancialIntentResult(
                intent = FinancialIntent.FINANCIAL_INFORMATION,
                confidence = 88,
                reasoning = listOf("Statement / balance / limit / reminder with no money movement"),
                matchedSignals = information,
            )
        }

        // 3. PRIMARY: a movement verb in our own vocabulary. Money actually moved.
        if (movement.isNotEmpty()) {
            return FinancialIntentResult(
                intent = FinancialIntent.FINANCIAL_EVENT,
                confidence = 90,
                reasoning = listOf("Money-movement verb present: ${movement.first()}"),
                matchedSignals = movement,
            )
        }

        // 4. FALLBACK: our own text analysis found nothing decisive, but the
        //    extraction registry's OWN confirmation detection fired (a different,
        //    frozen phrase list). Treated as corroborating evidence, consulted only
        //    because our primary analysis was silent — not as a routing decision
        //    made by the extractor.
        if (extraction is ExtractionRegistry.ExtractionOutcome.Confirmation) {
            return FinancialIntentResult(
                intent = FinancialIntent.FINANCIAL_CONFIRMATION,
                confidence = 85,
                reasoning = listOf(
                    "Extraction registry independently matched a confirmation pattern: " +
                        extraction.matchedPhrases.joinToString(),
                ),
                matchedSignals = extraction.matchedPhrases,
            )
        }

        // 5. FALLBACK: no decisive phrase signal either way, but extraction DID
        //    produce a valid candidate (bank-specific or heuristic detection we
        //    don't want to silently override). Trust it cautiously, at lower
        //    confidence than a direct movement-verb match.
        if (extraction is ExtractionRegistry.ExtractionOutcome.Success) {
            return FinancialIntentResult(
                intent = FinancialIntent.FINANCIAL_EVENT,
                confidence = 70,
                reasoning = listOf(
                    "Extractor produced a valid transaction candidate; " +
                        "no confirmation/information language present",
                ),
            )
        }

        // 6. UNKNOWN. Surfaced through diagnostics only; never persisted.
        val extractionReason = when (extraction) {
            is ExtractionRegistry.ExtractionOutcome.Ignored -> "Extractor ignored: ${extraction.reason}"
            is ExtractionRegistry.ExtractionOutcome.Failed -> "Extractor failed: ${extraction.reason}"
            else -> null
        }
        val hasCurrency = currencyTokens.any { it in text }
        return FinancialIntentResult(
            intent = FinancialIntent.UNKNOWN,
            confidence = if (hasCurrency) 30 else 0,
            reasoning = listOfNotNull(
                extractionReason,
                if (hasCurrency) "Currency amount present but no decisive signal" else "No decisive financial signal",
            ),
        )
    }
}

