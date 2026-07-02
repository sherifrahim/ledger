package com.sherif.ledger.feature.capture.parsing.extraction

import javax.inject.Inject

/**
 * Extracts monetary amount from normalized text.
 * Converts to minor units (Long) assuming 2 decimal places by default.
 */
class AmountExtractor @Inject constructor() {

    private val amountRegex = Regex("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+\\.\\d{1,2}|\\d+)")

    fun extract(text: String): Long? {
        val match = amountRegex.find(text) ?: return null
        val cleaned = match.value.replace(",", "")
        
        return if (cleaned.contains(".")) {
            val parts = cleaned.split(".")
            val major = parts[0].toLong()
            val minorText = parts[1].padEnd(2, '0').take(2)
            val minor = minorText.toLong()
            major * 100 + minor
        } else {
            cleaned.toLong() * 100
        }
    }
}
