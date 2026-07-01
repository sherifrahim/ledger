package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.BrandDao
import com.sherif.ledger.core.database.entity.MerchantAliasEntity
import com.sherif.ledger.core.database.mapper.toDomain
import com.sherif.ledger.core.database.mapper.toEntity
import com.sherif.ledger.core.domain.model.Brand
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.MerchantRepository
import javax.inject.Inject

class RoomMerchantRepository @Inject constructor(
    private val brandDao: BrandDao
) : MerchantRepository {

    override suspend fun getAllBrands(): LedgerResult<List<Brand>> = try {
        val brands = brandDao.getAllBrands().map { it.toDomain() }
        LedgerResult.Success(brands)
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun getBrandByAlias(rawText: String): LedgerResult<Brand> = try {
        val entity = brandDao.getBrandByAlias(rawText)
        if (entity != null) {
            LedgerResult.Success(entity.toDomain())
        } else {
            // This might not be an error in some flows, but a failure for this specific lookup
            LedgerResult.Failure(LedgerError.Unknown("No brand registered for alias: $rawText"))
        }
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun insertBrand(brand: Brand): LedgerResult<Long> = try {
        val id = brandDao.insertBrand(brand.toEntity())
        LedgerResult.Success(id)
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun registerAlias(rawText: String, brandId: Long): LedgerResult<Unit> = try {
        brandDao.insertMerchantAlias(MerchantAliasEntity(rawText, brandId))
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }
}
