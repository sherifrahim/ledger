package com.sherif.ledger.core.model

import kotlin.math.abs

/**
 * Immutable money value stored in minor units, such as cents or fils.
 *
 * Ledger never stores money as floating point values. Arithmetic is only
 * allowed between values that share the same strongly typed currency.
 */
data class Money(
    val minorUnits: Long,
    val currency: Currency,
) : Comparable<Money> {
    /** Adds two money values with the same currency. */
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits + other.minorUnits)
    }

    /** Subtracts two money values with the same currency. */
    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits - other.minorUnits)
    }

    /** Returns this amount with the sign reversed. */
    operator fun unaryMinus(): Money = copy(minorUnits = -minorUnits)

    /** Compares two money values with the same currency. */
    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    /** Formats this money value using its currency metadata. */
    fun format(): String {
        val sign = if (minorUnits < 0) "-" else ""
        val absoluteMinorUnits = abs(minorUnits)

        if (currency.fractionDigits == 0) {
            return "$sign${currency.symbol}$absoluteMinorUnits"
        }

        val divisor = powerOfTen(currency.fractionDigits)
        val major = absoluteMinorUnits / divisor
        val minor = absoluteMinorUnits % divisor
        val minorText = minor.toString().padStart(currency.fractionDigits, '0')

        return "$sign${currency.symbol}$major.$minorText"
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Money arithmetic requires matching currencies."
        }
    }

    private fun powerOfTen(exponent: Int): Long {
        var value = 1L
        repeat(exponent) {
            value *= 10L
        }
        return value
    }

    companion object {
        /** Creates a zero amount for the given currency. */
        fun zero(currency: Currency): Money = Money(
            minorUnits = 0L,
            currency = currency,
        )
    }
}
