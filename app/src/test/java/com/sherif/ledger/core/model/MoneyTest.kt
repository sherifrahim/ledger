package com.sherif.ledger.core.model

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun `plus adds amount with same currency`() {
        val first = Money(1000L, CurrencyCode.AED)
        val second = Money(500L, CurrencyCode.AED)
        val result = first + second
        assertEquals(1500L, result.minorUnits)
    }

    @Test
    fun `plus throws on currency mismatch`() {
        val first = Money(1000L, CurrencyCode.AED)
        val second = Money(500L, CurrencyCode.INR)
        assertThrows(IllegalArgumentException::class.java) {
            first + second
        }
    }

    @Test
    fun `minus subtracts amount with same currency`() {
        val first = Money(1000L, CurrencyCode.AED)
        val second = Money(400L, CurrencyCode.AED)
        val result = first - second
        assertEquals(600L, result.minorUnits)
    }

    @Test
    fun `times scales amount by integer`() {
        val money = Money(1000L, CurrencyCode.AED)
        val result = money * 3
        assertEquals(3000L, result.minorUnits)
    }

    @Test
    fun `div scales amount by integer`() {
        val money = Money(1000L, CurrencyCode.AED)
        val result = money / 2
        assertEquals(500L, result.minorUnits)
    }

    @Test
    fun `zero creates zero amount`() {
        val money = Money.zero(CurrencyCode.AED)
        assertEquals(0L, money.minorUnits)
        assertEquals(CurrencyCode.AED, money.currencyCode)
    }
}
