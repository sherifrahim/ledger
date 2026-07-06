package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.CurrencyCode
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
}
