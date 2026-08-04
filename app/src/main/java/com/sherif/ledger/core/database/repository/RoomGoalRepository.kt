package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.GoalDao
import com.sherif.ledger.core.database.entity.GoalEntity
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Goal
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomGoalRepository @Inject constructor(
    private val goalDao: GoalDao,
) : GoalRepository {

    override fun observeAll(): Flow<List<Goal>> = goalDao.observeAll().map { entities ->
        entities.mapNotNull { entity ->
            // Fail closed on a currency this build can no longer represent, rather
            // than coercing it to a default and quietly restating the target.
            val currency = runCatching { CurrencyCode.valueOf(entity.currencyCode) }.getOrNull()
                ?: return@mapNotNull null
            Goal(
                id = entity.id,
                name = entity.name,
                target = Money(entity.targetMinor, currency),
                accountId = entity.accountId,
                targetDate = entity.targetDateMillis?.let { Instant.ofEpochMilli(it) },
            )
        }
    }

    override suspend fun save(goal: Goal) {
        goalDao.upsert(
            GoalEntity(
                id = goal.id,
                name = goal.name,
                targetMinor = goal.target.minorUnits,
                currencyCode = goal.target.currencyCode.name,
                accountId = goal.accountId,
                targetDateMillis = goal.targetDate?.toEpochMilli(),
            ),
        )
    }

    override suspend fun delete(goalId: Long) = goalDao.delete(goalId)
}
