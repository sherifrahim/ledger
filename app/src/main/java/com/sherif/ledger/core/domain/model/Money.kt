package com.sherif.ledger.core.domain.model

/**
 * Immutable money value object stored in minor units (e.g. fils, cents).
 * 
 * Floating point math is strictly forbidden for financial calculations to 
 * ensure absolute precision and prevent rounding errors.
 */
data class Money(
    val minorUnits: Long,
    val currencyCode: CurrencyCode,
) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits + other.minorUnits)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(minorUnits = minorUnits - other.minorUnits)
    }

    operator fun times(scalar: Int): Money = copy(minorUnits = minorUnits * scalar)
    operator fun times(scalar: Long): Money = copy(minorUnits = minorUnits * scalar)
    
    operator fun div(scalar: Int): Money = copy(minorUnits = minorUnits / scalar)
    operator fun div(scalar: Long): Money = copy(minorUnits = minorUnits / scalar)

    operator fun unaryMinus(): Money = copy(minorUnits = -minorUnits)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    private fun requireSameCurrency(other: Money) {
        require(currencyCode == other.currencyCode) {
            "Financial operations require matching currencies: $currencyCode vs ${other.currencyCode}"
        }
    }

    companion object {
        fun zero(code: CurrencyCode): Money = Money(0L, code)
    }
}
