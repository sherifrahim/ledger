package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.CurrencyCode
import java.util.regex.Pattern

/**
 * Reusable regex-based extraction units to avoid monolithic patterns.
 */
object ExtractionHelpers {
    private val AMOUNT_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+\\.\\d{1,2}|\\d+)")
    private val CARD_PATTERN = Pattern.compile("(?:[Xx*]{1,4}|ending in )(\\d{4})")

    fun extractAmountMinor(text: String): Long? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (!matcher.find()) return null
        
        val matchedValue = matcher.group(1) ?: return null
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
