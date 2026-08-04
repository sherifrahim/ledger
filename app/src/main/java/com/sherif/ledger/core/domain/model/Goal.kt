package com.sherif.ledger.core.domain.model

import java.time.Instant

/** Something the user is saving towards, funded by one account. */
data class Goal(
    val id: Long,
    val name: String,
    val target: Money,
    val accountId: Long,
    val targetDate: Instant? = null,
)

/**
 * A goal paired with the funding account's real, replayed balance.
 *
 * [saved] is never stored — see [com.sherif.ledger.core.database.entity.GoalEntity].
 */
data class GoalProgress(
    val goal: Goal,
    val accountName: String,
    val saved: Money,
) {
    val fraction: Float
        get() = if (goal.target.minorUnits <= 0L) 0f
        else (saved.minorUnits.toFloat() / goal.target.minorUnits.toFloat()).coerceAtLeast(0f)

    val isReached: Boolean get() = saved.minorUnits >= goal.target.minorUnits

    /** Still to find, floored at zero. */
    val remainingMinor: Long
        get() = (goal.target.minorUnits - saved.minorUnits).coerceAtLeast(0L)
}
