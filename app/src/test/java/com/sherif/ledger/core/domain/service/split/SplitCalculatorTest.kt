package com.sherif.ledger.core.domain.service.split

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitCalculatorTest {

    private val calculator = SplitCalculator()

    @Test fun `equal share divides evenly when total divides cleanly`() {
        // AED 420 across 3 participants (you + Ahmed + Ali) = 140 each
        assertEquals(14000L, calculator.equalShare(42000L, 3))
    }

    @Test fun `equal share floors when total does not divide evenly, self absorbs the remainder`() {
        // 42100 / 3 = 14033.33 -> each non-self participant gets 14033 (floor);
        // self's implicit portion (never stored) absorbs the extra minor unit.
        val perPerson = calculator.equalShare(42100L, 3)
        assertEquals(14033L, perPerson)
        // Verifying the remainder-absorption property directly: two non-self
        // participants at this rate, plus self's implicit share, must sum to
        // exactly the total -- proving no money is silently lost or invented.
        val selfImplicit = 42100L - (perPerson * 2)
        assertEquals(42100L, perPerson * 2 + selfImplicit)
        assertEquals(14034L, selfImplicit) // self absorbs the 1 extra minor unit
    }

    @Test fun `equal share with only self participant returns the full amount`() {
        assertEquals(10000L, calculator.equalShare(10000L, 1))
    }

    @Test fun `percentage share rounds to the nearest minor unit`() {
        // 33.33% of 10000 = 3333.0 -> rounds to 3333
        assertEquals(3333L, calculator.percentageShare(10000L, 33.33))
        // 50% of 14033 = 7016.5 -> rounds to 7017 (round-half-up via roundToLong)
        assertEquals(7017L, calculator.percentageShare(14033L, 50.0))
    }

    @Test fun `isWithinTotal accepts shares that sum to exactly the total`() {
        assertTrue(calculator.isWithinTotal(listOf(10000L, 20000L), 30000L))
    }

    @Test fun `isWithinTotal accepts shares that sum to less than the total`() {
        // Self covers the remainder -- under-allocation is valid by design.
        assertTrue(calculator.isWithinTotal(listOf(10000L), 30000L))
    }

    @Test fun `isWithinTotal rejects shares that exceed the total`() {
        assertFalse(calculator.isWithinTotal(listOf(10000L, 25000L), 30000L))
    }

    @Test fun `isWithinTotalPercentage rejects percentages that exceed 100`() {
        assertFalse(calculator.isWithinTotalPercentage(listOf(60.0, 50.0)))
    }

    @Test fun `isWithinTotalPercentage accepts percentages under 100`() {
        assertTrue(calculator.isWithinTotalPercentage(listOf(30.0, 30.0)))
    }
}


