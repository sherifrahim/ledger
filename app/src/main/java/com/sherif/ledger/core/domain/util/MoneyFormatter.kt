package com.sherif.ledger.core.domain.util

import com.sherif.ledger.core.domain.model.CurrencyRegistry
import com.sherif.ledger.core.domain.model.Money
import kotlin.math.abs

object MoneyFormatter {
    fun format(money: Money, includeSymbol: Boolean = true): String {
        val currency = CurrencyRegistry.get(money.currencyCode)
        val absoluteMinorUnits = abs(money.minorUnits)
        
        val amountStr = if (currency.decimalDigits == 0) {
            absoluteMinorUnits.toString()
        } else {
            val divisor = powerOfTen(currency.decimalDigits)
            val major = absoluteMinorUnits / divisor
            val minor = absoluteMinorUnits % divisor
            val minorText = minor.toString().padStart(currency.decimalDigits, '0')
            "$major.$minorText"
        }

        return if (includeSymbol) {
            "${currency.symbol} $amountStr"
        } else {
            amountStr
        }
    }

    private fun powerOfTen(exponent: Int): Long {
        var value = 1L
        repeat(exponent) { value *= 10L }
        return value
    }
}
