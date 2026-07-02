package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.CurrencyCode
import javax.inject.Inject

/**
 * Identifies supported currency codes from text.
 */
class CurrencyExtractor @Inject constructor() {

    fun extract(text: String): CurrencyCode? {
        val upper = text.uppercase()
        return when {
            "AED" in upper || "DIRHAM" in upper -> CurrencyCode.AED
            "INR" in upper || "RS" in upper || "₹" in upper -> CurrencyCode.INR
            else -> null
        }
    }
}
