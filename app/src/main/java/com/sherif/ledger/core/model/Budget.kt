package com.sherif.ledger.core.model

/**
 * Strongly typed identifier for a budget.
 */
@JvmInline
value class BudgetId(val value: String) {
    init { require(value.isNotBlank()) { "Budget id cannot be blank." } }
    override fun toString(): String = value
}

/**
 * The recurrence period a budget covers.
 */
enum class BudgetPeriod {
    Weekly,
    Monthly,
    Yearly,
}

/**
 * A spending limit applied overall or to a specific category.
 *
 * [limit] is the maximum the user intends to spend. [spent] is the
 * running total consumed so far within the current [period]. Both
 * carry the same [Currency] enforced at construction.
 *
 * Relationships:
 * - optionally scoped to one [Category] via [categoryId]
 * - [spent] is derived from [LedgerTransaction] amounts in the period
 */
data class Budget(
    val id: BudgetId,
    val name: String,
    val limit: Money,
    val spent: Money,
    val period: BudgetPeriod,
    val categoryId: CategoryId? = null,
) {
    init {
        require(name.isNotBlank()) { "Budget name cannot be blank." }
        require(limit.currency == spent.currency) { "Budget limit and spent must share the same currency." }
        require(limit.minorUnits > 0) { "Budget limit must be positive." }
    }

    val remaining: Money get() = limit - spent

    val progress: Float get() = if (limit.minorUnits == 0L) 0f
        else spent.minorUnits.toFloat() / limit.minorUnits.toFloat()
}
