package com.sherif.ledger.feature.capture.extraction

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reusable financial phrase groups. Not a registry, not a subsystem — a plain
 * injectable holder of phrase lists with simple matching helpers, so every
 * extractor (and future on-device models, for prompt grounding) can share the
 * same vocabulary instead of hardcoding whole bank messages.
 *
 * Extend the lists here to teach Ledger new financial language; no extractor
 * code needs to change.
 */
@Singleton
class FinancialPhraseLibrary @Inject constructor() {

    /** Verbs that indicate an actual money movement (positive evidence). */
    val transactionVerbs: List<String> = listOf(
        "spent", "debited", "credited", "received", "transferred", "withdrawn",
        "deposited", "refunded", "purchased", "used for", "charged", "paid",
        "payment of", "withdrawal", "reversed", "purchase of", "debit of",
        "credit of", "transaction of",
    )

    /** Marketing / offer language (negative evidence). */
    val promotionPhrases: List<String> = listOf(
        "eligible", "apply now", "cashback", "reward", "offer", "promotion",
        "limited time", "pre-approved", "preapproved", "instant approval",
        "upgrade", "convert to emi", "easy emi", "personal loan", "credit limit",
        "increase your", "apply for", "avail", "% off", "% cashback", "voucher",
        "discount", "exclusive", "don't miss", "hurry", "win ", "lucky draw",
        "pre-qualified", "special offer", "book now", "redeem",
    )

    /** One-time-password language (terminal ignore). */
    val otpPhrases: List<String> = listOf(
        "otp", "verification code", "one-time password", "one time password",
        "secure code", "security code",
    )

    /** Statement / service-notification language (terminal ignore). */
    val statementPhrases: List<String> = listOf(
        "statement is ready", "statement generated", "download statement",
        "e-statement", "statement available", "monthly statement",
        "statement has been generated",
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
