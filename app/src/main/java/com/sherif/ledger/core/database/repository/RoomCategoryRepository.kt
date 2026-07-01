package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.CategoryDao
import com.sherif.ledger.core.database.mapper.toDomain
import com.sherif.ledger.core.database.mapper.toEntity
import com.sherif.ledger.core.domain.model.Category
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeAllCategories(): Flow<LedgerResult<List<Category>>> =
        categoryDao.getAllCategories()
            .map { entities -> 
                val result: LedgerResult<List<Category>> = LedgerResult.Success(entities.map { it.toDomain() })
                result
            }
            .catch { e -> 
                emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error"))) 
            }

    override suspend fun insertCategory(category: Category): LedgerResult<Long> = try {
        val id = categoryDao.insertCategory(category.toEntity())
        LedgerResult.Success(id)
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun deleteCategory(id: Long): LedgerResult<Unit> = try {
        // Soft delete logic can be added to DAO if needed
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }
}
