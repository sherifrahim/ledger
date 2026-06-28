package com.sherif.ledger.core.model

import java.util.Locale

@JvmInline
value class CurrencyCode(val value: String) {
    init { require(value.matches(CURRENCY_CODE_PATTERN)) { "Currency code must contain exactly three uppercase letters." } }
    override fun toString(): String = value
    private companion object { val CURRENCY_CODE_PATTERN = Regex("[A-Z]{3}") }
}

/**
 * Currency metadata used for formatting and currency-safe money operations.
 *
 * Ledger supports dual currency (AED and INR). Additional currencies can be
 * added by extending this companion.
 */
data class Currency(
    val code: CurrencyCode,
    val symbol: String,
    val fractionDigits: Int,
) {
    init {
        require(symbol.isNotBlank()) { "Currency symbol cannot be blank." }
        require(fractionDigits >= 0) { "Currency fraction digits cannot be negative." }
    }

    companion object {
        val AED = Currency(CurrencyCode("AED"), "AED", 2)
        val INR = Currency(CurrencyCode("INR"), "\u20B9", 2)
        val USD = Currency(CurrencyCode("USD"), "$", 2)

        fun of(code: String, symbol: String = code.uppercase(Locale.US), fractionDigits: Int = 2): Currency =
            Currency(CurrencyCode(code.uppercase(Locale.US)), symbol, fractionDigits)
    }
}
