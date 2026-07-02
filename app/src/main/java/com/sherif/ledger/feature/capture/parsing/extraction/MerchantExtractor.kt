package com.sherif.ledger.feature.capture.parsing.extraction

import javax.inject.Inject

/**
 * Extracts raw merchant name from notification text using keyword-based heuristics.
 */
class MerchantExtractor @Inject constructor() {

    private val merchantKeywords = listOf(" at ", " to ", " on ", " from ", " with ")

    fun extract(text: String): String? {
        // Find the first keyword and take the text following it until a period or end of string.
        for (keyword in merchantKeywords) {
            val index = text.lowercase().indexOf(keyword)
            if (index != -1) {
                val start = index + keyword.length
                val substring = text.substring(start)
                val end = substring.indexOfFirst { it == '.' || it == ',' }
                return if (end != -1) substring.substring(0, end).trim() else substring.trim()
            }
        }
        return null
    }
}
