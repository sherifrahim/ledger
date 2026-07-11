package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.TransferDirection
import java.util.regex.Pattern

/**
 * Reusable regex-based extraction units to avoid monolithic patterns.
 */
object ExtractionHelpers {
    // Currency-anchored: the amount must follow a currency token. Prevents card/account
    // numbers (which often precede the amount) from being mis-read as the amount.
    private val AMOUNT_ANCHORED_PATTERN = Pattern.compile("(?:AED|USD|INR|Rs\\.?|DIRHAM)\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    // Fallback for texts with no currency token: first number-like token.
    private val AMOUNT_FALLBACK_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?|\\d+\\.\\d{1,2}|\\d+)")
    private val CARD_PATTERN = Pattern.compile("(?:card no\\.?\\s*[Xx*]*|[Xx*]{2,4}|(?:card |account |a\\/c )?ending(?: in)? |account(?: no)?\\.?\\s*[Xx*]*|a\\/c\\s*[Xx*]*)(\\d{4,})", Pattern.CASE_INSENSITIVE)

    fun extractAmountMinor(text: String): Long? {
        val anchored = AMOUNT_ANCHORED_PATTERN.matcher(text)
        val matchedValue = if (anchored.find()) {
            anchored.group(1)
        } else {
            val fallback = AMOUNT_FALLBACK_PATTERN.matcher(text)
            if (fallback.find()) fallback.group(1) else null
        } ?: return null

        val cleaned = matchedValue.replace(",", "")
        return if (cleaned.contains(".")) {
            val parts = cleaned.split(".")
            val major = parts[0].toLong()
            val minorText = parts[1].padEnd(2, '0').take(2)
            major * 100 + minorText.toLong()
        } else {
            cleaned.toLong() * 100
        }
    }

    fun extractCurrency(text: String): CurrencyCode? {
        val upper = text.uppercase()
        return when {
            upper.contains("AED") || upper.contains("DIRHAM") -> CurrencyCode.AED
            upper.contains("INR") || upper.contains("RS") || upper.contains("₹") -> CurrencyCode.INR
            else -> null
        }
    }

    fun extractAccountHint(text: String): String? {
        val matcher = CARD_PATTERN.matcher(text)
        return if (matcher.find()) matcher.group(1) else null
    }

    // Incoming-transfer indicators: money ARRIVING at the account in question. Bank-
    // agnostic, deliberately narrow. Used ONLY to determine direction of a transfer
    // that has ALREADY been identified as a transfer by the caller's own type
    // detection (this function does not decide "is this a transfer").
    private val incomingTransferPhrases = listOf(
        "received from", "received via", "credited via", "credited from",
        "credited into your account", "credited into account", "credited to your account",
        "credited to account", "deposited into your account", "deposited to your account",
        "amount received", "funds received", "money received",
    )

    /**
     * Determines transfer direction from the SAME text already inspected by the
     * caller when it decided the message is a transfer. This is the ONE place
     * direction is inferred from raw text — it exists so the decision is made ONCE,
     * upstream, and recorded as structured data. Callers downstream of extraction
     * (balance calculation, analytics) MUST consume the resulting field and never
     * call this or re-parse text themselves.
     *
     * Defaults to OUTGOING when no incoming indicator is present, matching the
     * dominant real-world pattern this corpus has observed: a bank message
     * describing a transfer is overwhelmingly the account holder sending money out
     * ("transferred to", "sent via UPI", "paid towards your card").
     */
    fun inferTransferDirection(lower: String): TransferDirection =
        if (incomingTransferPhrases.any { lower.contains(it) }) {
            TransferDirection.INCOMING
        } else {
            TransferDirection.OUTGOING
        }
}


