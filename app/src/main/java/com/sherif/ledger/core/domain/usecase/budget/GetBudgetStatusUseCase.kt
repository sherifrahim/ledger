package com.sherif.ledger.core.domain.usecase.budget

import com.sherif.ledger.core.domain.model.Budget
import com.sherif.ledger.core.domain.model.BudgetStatus
import com.sherif.ledger.core.domain.model.CategoryTotal
import com.sherif.ledger.core.domain.model.Money
import javax.inject.Inject

/**
 * Pairs each budget with what has actually been spent against it this month.
 *
 * Deliberately does NOT compute spending. Category totals are already produced by
 * [com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase],
 * which is the single source of truth every other screen reads — netting refunds,
 * excluding card settlements and ATM withdrawals, and resolving each merchant's
 * category through the same resolver. Recomputing here would be a second,
 * independently drifting definition of "spent on groceries", and the first
 * disagreement between this screen and Insights would be impossible to explain.
 *
 * A budget whose category has no spend yet still comes back, at zero — the user
 * set that ceiling deliberately and it should not vanish until something lands
 * against it.
 */
class GetBudgetStatusUseCase @Inject constructor() {

    fun execute(budgets: List<Budget>, categoryTotals: List<CategoryTotal>): List<BudgetStatus> {
        if (budgets.isEmpty()) return emptyList()
        val spentByCategory = categoryTotals.associateBy({ it.category.uppercase() }, { it.amountMinor })

        return budgets
            .map { budget ->
                BudgetStatus(
                    budget = budget,
                    spent = Money(
                        spentByCategory[budget.category.uppercase()] ?: 0L,
                        budget.limit.currencyCode,
                    ),
                )
            }
            // Worst first: a budget you are about to blow is the one worth seeing.
            .sortedByDescending { it.fraction }
    }
}
