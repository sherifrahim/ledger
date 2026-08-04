package com.sherif.ledger.core.domain.model

/** A monthly ceiling the user set for one spending category. */
data class Budget(
    val id: Long,
    val category: String,
    val limit: Money,
)

/**
 * A budget paired with what has actually been spent against it this month.
 *
 * [spent] is never stored — it comes from the same category aggregation every
 * other screen reads, so a budget can never disagree with the Insights breakdown
 * about how much went on groceries.
 */
data class BudgetStatus(
    val budget: Budget,
    val spent: Money,
) {
    /** 0f when the limit is zero — a zero ceiling is not "infinitely over". */
    val fraction: Float
        get() = if (budget.limit.minorUnits <= 0L) 0f
        else (spent.minorUnits.toFloat() / budget.limit.minorUnits.toFloat())

    val isOver: Boolean get() = spent.minorUnits > budget.limit.minorUnits

    /** What is left, floored at zero — a budget cannot have negative headroom. */
    val remainingMinor: Long
        get() = (budget.limit.minorUnits - spent.minorUnits).coerceAtLeast(0L)
}
