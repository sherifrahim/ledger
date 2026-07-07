package com.sherif.ledger.feature.capture.extraction

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reusable financial LANGUAGE library. Not a registry, not a subsystem — a plain
 * injectable holder of concept vocabularies with simple matching helpers, so
 * every extractor (and a future on-device model, for prompt grounding) shares
 * the same financial-language knowledge base instead of hardcoding bank templates.
 *
 * The goal is bank-agnostic understanding: recognize CONCEPTS (salary credit,
 * outward transfer, POS purchase, card payment, loan disbursement, EMI deduction,
 * fee, interest, refund, reversal) regardless of which bank sent the message.
 *
 * INTENT POLARITY is the key design point. Some words appear in both a real event
 * and a marketing message ("loan", "emi", "cashback"); disambiguation comes from
 * the VERB/CONTEXT, not the noun:
 *  - [eventVerbs] (broad) are positive evidence of a real money movement.
 *  - [strongEventVerbs] (narrow: money LEAVING for a NEW event) veto a
 *    confirmation classification only — they exclude "payment of"/"received" so
 *    payment-received acknowledgements still classify as confirmations.
 *  - [hardOfferPhrases] (eligible / apply now / pre-approved ...) are strong
 *    negative evidence even when a transaction verb is also present.
 *
 * Extend the lists here to teach Ledger new financial language; no extractor
 * code needs to change.
 */
@Singleton
class FinancialPhraseLibrary @Inject constructor() {

    // ---- Money-movement verbs (broad; positive evidence of a real event) ----
    val eventVerbs: List<String> = listOf(
        "debited", "credited", "spent", "withdrawn", "deposited", "transferred",
        "paid", "charged", "used for", "used at", "purchase", "purchased",
        "payment of", "debit of", "credit of", "transaction of", "disbursed",
        "refunded", "reversed", "posted to", "sent to", "fund transfer",
        "transfer of", "transfer to", "remittance", "received from",
        "using your card", "using card",
    )

    /** Kept for backward compatibility (Phase 4B). Alias of the broad verbs. */
    val transactionVerbs: List<String> get() = eventVerbs

    /**
     * Narrow "money leaving for a NEW event" verbs. Used ONLY to veto a
     * confirmation. Deliberately excludes "payment of" / "received".
     */
    val strongEventVerbs: List<String> = listOf(
        "debited", "spent", "withdrawn", "transferred", "charged",
        "used for", "used at", "purchased", "disbursed", "sent to",
    )

    // ---- Offer / marketing (negative evidence) ----
    val promotionPhrases: List<String> = listOf(
        "eligible", "apply now", "apply for", "pre-approved", "preapproved",
        "pre-qualified", "instant approval", "limited time", "offer", "avail",
        "upgrade your", "increase your credit", "special offer", "exclusive",
        "hurry", "don't miss", "book now", "redeem your", "win ", "voucher",
        "discount", "% off", "% cashback on", "reward points", "promotion",
        "promo", "activate now", "claim your", "interest-free for",
        "convert to easy emi", "convert to emi", "easy emi", "lucky draw",
    )

    /** Offer phrases so strongly promotional they outweigh a transaction verb. */
    val hardOfferPhrases: List<String> = listOf(
        "eligible", "apply now", "apply for", "pre-approved", "preapproved",
        "pre-qualified", "instant approval",
    )

    // ---- Terminal ignores ----
    val otpPhrases: List<String> = listOf(
        "otp", "verification code", "one-time password", "one time password",
        "secure code", "security code",
    )

    val statementPhrases: List<String> = listOf(
        "statement is ready", "statement generated", "download statement",
        "e-statement", "statement available", "monthly statement",
        "statement has been generated",
    )

    // ---- Confirmation (acknowledges an EXISTING payment; Phase 4C) ----
    val confirmationPhrases: List<String> = listOf(
        "payment received", "payment posted", "payment credited",
        "payment successfully received", "payment acknowledged",
        "payment has been posted", "has been received",
        "we have received your payment", "thank you", "outstanding balance",
        "minimum amount due", "available credit restored",
        "receipt of your payment", "successfully processed your payment",
    )

    /**
     * STRONG confirmation phrases: unambiguous acknowledgements of an existing
     * payment or balance-state updates. Unlike the full [confirmationPhrases]
     * list, this excludes weak signals like "thank you" that also appear in
     * ordinary purchase receipts. A confirmation is only classified when a strong
     * phrase is present, preventing purchase receipts ("Thank you for using your
     * card ... at MERCHANT") from being mistaken for confirmations.
     */
    val strongConfirmationPhrases: List<String> = listOf(
        "payment received", "payment posted", "payment credited",
        "payment successfully received", "payment acknowledged",
        "payment has been posted", "we have received your payment",
        "outstanding balance", "minimum amount due",
        "available credit restored", "receipt of your payment",
        "successfully processed your payment", "balance restored",
        "outstanding updated", "credit card outstanding",
    )

    /**
     * Credit indicators: bank shorthand for a credit to the account that the
     * verb list misses (e.g. "Cr. transaction"). Signals Income. Bank-agnostic;
     * many banks abbreviate credit/debit as Cr./Dr.
     */
    val creditIndicatorPhrases: List<String> = listOf(
        "cr. transaction", "cr transaction", "a/c credited", "acct credited",
        "amount credited", "credit transaction",
    )

    // ---- Concept vocabularies for TYPE inference (bank-agnostic) ----
    val salaryPhrases: List<String> = listOf(
        "salary", "payroll", "wps", "end of service", "gratuity",
    )
    val atmPhrases: List<String> = listOf(
        "atm", "cash withdrawal", "cash withdrawn",
    )
    val cardPaymentPhrases: List<String> = listOf(
        "towards", "card payment", "credit card payment", "bill payment",
        "towards your card", "towards credit card",
    )
    val transferPhrases: List<String> = listOf(
        "transferred", "transfer to", "transfer of", "imps", "neft", "rtgs",
        "upi", "sent to", "remittance", "fund transfer", "inward", "outward",
    )
    val refundPhrases: List<String> = listOf(
        "refund", "refunded", "reversed", "reversal", "merchant refund",
        "chargeback",
    )
    val loanDisbursePhrases: List<String> = listOf(
        "loan disbursed", "loan credited", "disbursement of",
        "loan amount credited", "has been disbursed",
    )
    val emiDeductionPhrases: List<String> = listOf(
        "emi of", "emi debited", "installment of", "instalment debited",
        "emi deducted",
    )
    val feePhrases: List<String> = listOf(
        "fee", "service charge", "annual fee", "processing charge",
    )
    val interestPhrases: List<String> = listOf(
        "interest earned", "interest credited", "profit credited",
        "interest charged", "profit earned",
    )
    val depositPhrases: List<String> = listOf(
        "deposited", "cash deposit", "cheque deposit", "cheque credited",
    )

    /**
     * Returns the phrases from [group] that appear (case-insensitive substring)
     * in [text]. Empty list means no match.
     */
    fun matches(group: List<String>, text: String): List<String> {
        val lower = text.lowercase()
        return group.filter { lower.contains(it) }
    }

    /** Convenience: does [text] contain any phrase from [group]? */
    fun containsAny(group: List<String>, text: String): Boolean =
        matches(group, text).isNotEmpty()
}

