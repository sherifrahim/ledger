package com.sherif.ledger.feature.capture.notification

import java.util.regex.Pattern

/**
 * Lightweight, filter-side heuristics for deciding whether a notification LOOKS
 * financial. This is intentionally NOT extraction: it does not parse or return an
 * amount, it only answers "does this contain a financial-looking amount?". Keeping
 * this separate from the extractor's amount parsing decouples the admission filter
 * from extraction semantics — the filter detects financial-looking content, the
 * extractor performs extraction.
 */
object FinancialContentHeuristics {

    // A currency token immediately followed by a number. Deliberately looser and
    // cheaper than the extractor's parser: it only needs to recognize the SHAPE of
    // a monetary value, not compute its minor units.
    private val CURRENCY_AMOUNT: Pattern = Pattern.compile(
        "(?:AED|USD|INR|Rs\\.?|DIRHAM)\\s*\\d",
        Pattern.CASE_INSENSITIVE,
    )

    /** True when the text contains a currency-anchored, financial-looking amount. */
    fun looksLikeFinancialAmount(text: String): Boolean =
        CURRENCY_AMOUNT.matcher(text).find()
}

