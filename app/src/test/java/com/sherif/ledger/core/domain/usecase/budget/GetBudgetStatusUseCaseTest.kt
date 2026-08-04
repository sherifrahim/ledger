package com.sherif.ledger.core.domain.usecase.budget

import com.sherif.ledger.core.domain.model.Budget
import com.sherif.ledger.core.domain.model.CategoryTotal
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBudgetStatusUseCaseTest {

    private val useCase = GetBudgetStatusUseCase()

    private fun budget(category: String, limitMinor: Long) =
        Budget(id = 1L, category = category, limit = Money(limitMinor, CurrencyCode.AED))

    private fun total(category: String, amountMinor: Long) =
        CategoryTotal(category = category, amountMinor = amountMinor, transactionCount = 3)

    @Test
    fun `spend comes from the shared category totals, not a second calculation`() {
        val result = useCase.execute(
            budgets = listOf(budget("GROCERIES", 100_000L)),
            categoryTotals = listOf(total("GROCERIES", 42_000L)),
        ).single()

        assertEquals(42_000L, result.spent.minorUnits)
        assertEquals(58_000L, result.remainingMinor)
        assertFalse(result.isOver)
    }

    @Test
    fun `a budget with nothing spent against it yet still appears`() {
        // The user set that ceiling deliberately; it should not vanish until
        // something lands against it.
        val result = useCase.execute(
            budgets = listOf(budget("TRAVEL", 50_000L)),
            categoryTotals = listOf(total("GROCERIES", 42_000L)),
        ).single()

        assertEquals(0L, result.spent.minorUnits)
        assertEquals(50_000L, result.remainingMinor)
    }

    @Test
    fun `category matching is case-insensitive`() {
        // Budgets are stored as the user picked them; analytics emits enum names.
        val result = useCase.execute(
            budgets = listOf(budget("groceries", 100_000L)),
            categoryTotals = listOf(total("GROCERIES", 30_000L)),
        ).single()

        assertEquals(30_000L, result.spent.minorUnits)
    }

    @Test
    fun `going over is reported and headroom floors at zero`() {
        val result = useCase.execute(
            budgets = listOf(budget("DINING", 20_000L)),
            categoryTotals = listOf(total("DINING", 35_000L)),
        ).single()

        assertTrue(result.isOver)
        // Not negative: a budget cannot have less than no headroom left.
        assertEquals(0L, result.remainingMinor)
        assertEquals(1.75f, result.fraction, 0.001f)
    }

    @Test
    fun `the budget closest to breaking is listed first`() {
        val result = useCase.execute(
            budgets = listOf(
                budget("TRAVEL", 100_000L),
                budget("DINING", 100_000L),
                budget("GROCERIES", 100_000L),
            ),
            categoryTotals = listOf(
                total("TRAVEL", 10_000L),
                total("DINING", 95_000L),
                total("GROCERIES", 50_000L),
            ),
        )

        assertEquals(listOf("DINING", "GROCERIES", "TRAVEL"), result.map { it.budget.category })
    }

    @Test
    fun `a zero ceiling is not infinitely over`() {
        // Guards a divide-by-zero that would otherwise render as an infinite bar.
        val result = useCase.execute(
            budgets = listOf(budget("FUEL", 0L)),
            categoryTotals = listOf(total("FUEL", 5_000L)),
        ).single()

        assertEquals(0f, result.fraction, 0.001f)
    }

    @Test
    fun `no budgets means no work`() {
        assertTrue(useCase.execute(emptyList(), listOf(total("GROCERIES", 1L))).isEmpty())
    }
}
