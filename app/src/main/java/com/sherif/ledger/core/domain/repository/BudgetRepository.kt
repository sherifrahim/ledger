package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.Budget
import com.sherif.ledger.core.domain.model.CurrencyCode
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>

    /** Sets or replaces the ceiling for [category]. */
    suspend fun setBudget(category: String, limitMinor: Long, currencyCode: CurrencyCode)

    suspend fun removeBudget(category: String)
}
