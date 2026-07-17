package com.sherif.ledger.core.domain.util

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.CurrencyRegistry
import kotlin.math.abs

/**
 * Same magnitude math as [MoneyFormatter], but keeps the sign — needed for
 * balance-correction UI, where showing a negative computed balance as if it
 * were positive would defeat the point of asking the user to confirm it.
 */
fun formatSignedPlainDecimal(minorUnits: Long, currencyCode: CurrencyCode): String {
    val currency = CurrencyRegistry.get(currencyCode)
    val sign = if (minorUnits < 0) "-" else ""
    val absMinor = abs(minorUnits)
    if (currency.decimalDigits == 0) return "$sign$absMinor"
    var divisor = 1L
    repeat(currency.decimalDigits) { divisor *= 10L }
    val major = absMinor / divisor
    val minor = (absMinor % divisor).toString().padStart(currency.decimalDigits, '0')
    return "$sign$major.$minor"
}

/** Inverse of [formatSignedPlainDecimal] for user-entered text; null for blank/unparseable input. */
fun parsePlainDecimalToMinor(text: String?, decimalDigits: Int): Long? {
    if (text.isNullOrBlank()) return null
    val negative = text.startsWith("-")
    val unsigned = text.removePrefix("-")
    val parts = unsigned.split(".")
    val majorPart = parts.getOrNull(0)?.toLongOrNull() ?: return null
    val minorPart = parts.getOrNull(1)?.padEnd(decimalDigits, '0')?.take(decimalDigits)?.toLongOrNull() ?: 0L
    var divisor = 1L
    repeat(decimalDigits) { divisor *= 10L }
    val minorUnits = majorPart * divisor + minorPart
    return if (negative) -minorUnits else minorUnits
}
