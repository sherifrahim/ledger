package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeAll(): Flow<List<Goal>>
    suspend fun save(goal: Goal)
    suspend fun delete(goalId: Long)
}
