package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.BudgetDao
import com.sherif.ledger.core.database.entity.BudgetEntity
import com.sherif.ledger.core.domain.model.Budget
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomBudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
) : BudgetRepository {

    override fun observeAll(): Flow<List<Budget>> =
        budgetDao.observeAll().map { entities ->
            entities.mapNotNull { entity ->
                // A currency Ledger can no longer represent is skipped rather than
                // coerced to a default — the same fail-closed rule extraction uses.
                val currency = runCatching { CurrencyCode.valueOf(entity.currencyCode) }.getOrNull()
                    ?: return@mapNotNull null
                Budget(
                    id = entity.id,
                    category = entity.category,
                    limit = Money(entity.limitMinor, currency),
                )
            }
        }

    override suspend fun setBudget(category: String, limitMinor: Long, currencyCode: CurrencyCode) {
        budgetDao.upsert(
            BudgetEntity(category = category, limitMinor = limitMinor, currencyCode = currencyCode.name),
        )
    }

    override suspend fun removeBudget(category: String) = budgetDao.deleteByCategory(category)
}
