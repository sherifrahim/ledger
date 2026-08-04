package com.sherif.ledger.core.domain.usecase.goal

import com.sherif.ledger.core.domain.model.Goal
import com.sherif.ledger.core.domain.model.GoalProgress
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.service.transaction.AccountBalance
import javax.inject.Inject

/**
 * Pairs each goal with its funding account's real balance.
 *
 * Computes nothing itself: balances come from AccountBalanceService, the single
 * source of balance truth, so a goal can never disagree with the Accounts screen
 * about how much is in the account funding it.
 *
 * A goal whose account no longer exists is dropped rather than shown at zero —
 * "you have saved nothing" would be a false statement about a goal that has lost
 * its funding source, and the schema cascades the row away anyway.
 */
class GetGoalProgressUseCase @Inject constructor() {

    fun execute(goals: List<Goal>, balances: List<AccountBalance>): List<GoalProgress> {
        if (goals.isEmpty()) return emptyList()
        val byAccount = balances.associateBy { it.account.id }

        return goals.mapNotNull { goal ->
            val funding = byAccount[goal.accountId] ?: return@mapNotNull null
            // Never mix units: a goal in AED cannot be measured by a USD account.
            if (funding.balance.currencyCode != goal.target.currencyCode) return@mapNotNull null
            GoalProgress(
                goal = goal,
                accountName = funding.account.name,
                saved = Money(funding.balance.minorUnits.coerceAtLeast(0L), goal.target.currencyCode),
            )
        }.sortedByDescending { it.fraction }
    }
}
