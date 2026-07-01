package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.Category
import com.sherif.ledger.core.domain.model.LedgerResult
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAllCategories(): Flow<LedgerResult<List<Category>>>
    suspend fun insertCategory(category: Category): LedgerResult<Long>
    suspend fun deleteCategory(id: Long): LedgerResult<Unit>
}
