package com.sherif.ledger.core.domain.util

import com.sherif.ledger.core.domain.model.CurrencyRegistry
import com.sherif.ledger.core.domain.model.Money
import kotlin.math.abs

/**
 * Always formats a MAGNITUDE — the sign is discarded, never a leading "-".
 * This is safe for transaction amounts, which pair with a separate
 * isExpense/isIncome-style field the UI uses to color/prefix the number.
 * It is NOT safe for a value whose sign IS the information (account balance,
 * net worth) unless the caller separately threads that sign through to the
 * UI — see [com.sherif.ledger.presentation.dashboard.DashboardUiState.isNegativeBalance].
 */
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
