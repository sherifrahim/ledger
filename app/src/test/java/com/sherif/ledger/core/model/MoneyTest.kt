package com.sherif.ledger.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for currency-safe money arithmetic and deterministic formatting.
 */
class MoneyTest {
    @Test
    fun plus_addsMinorUnitsWhenCurrenciesMatch() {
        val first = Money(1_250L, Currency.USD)
        val second = Money(375L, Currency.USD)

        val result = first + second

        assertEquals(Money(1_625L, Currency.USD), result)
    }

    @Test
    fun minus_subtractsMinorUnitsWhenCurrenciesMatch() {
        val first = Money(1_250L, Currency.USD)
        val second = Money(375L, Currency.USD)

        val result = first - second

        assertEquals(Money(875L, Currency.USD), result)
    }

    @Test
    fun arithmetic_rejectsMismatchedCurrencies() {
        val dollars = Money(1_250L, Currency.USD)
        val dirhams = Money(1_250L, Currency.AED)

        assertThrows(IllegalArgumentException::class.java) {
            dollars + dirhams
        }
    }

    @Test
    fun unaryMinus_reversesSign() {
        val money = Money(1_250L, Currency.USD)

        assertEquals(Money(-1_250L, Currency.USD), -money)
    }

    @Test
    fun zero_createsZeroForCurrency() {
        assertEquals(Money(0L, Currency.AED), Money.zero(Currency.AED))
    }

    @Test
    fun format_formatsTwoFractionDigits() {
        val money = Money(1_205L, Currency.USD)

        assertEquals("$12.05", money.format())
    }

    @Test
    fun format_formatsNegativeAmounts() {
        val money = Money(-1_205L, Currency.USD)

        assertEquals("-$12.05", money.format())
    }

    @Test
    fun format_formatsZeroDecimalCurrency() {
        val yen = Currency.of(
            code = "JPY",
            symbol = "JPY ",
            fractionDigits = 0,
        )
        val money = Money(1_205L, yen)

        assertEquals("JPY 1205", money.format())
    }
}
