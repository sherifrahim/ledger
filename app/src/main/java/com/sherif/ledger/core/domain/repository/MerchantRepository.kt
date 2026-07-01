package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.Brand
import com.sherif.ledger.core.domain.model.LedgerResult

interface MerchantRepository {
    suspend fun getAllBrands(): LedgerResult<List<Brand>>
    suspend fun getBrandByAlias(rawText: String): LedgerResult<Brand>
    suspend fun insertBrand(brand: Brand): LedgerResult<Long>
    suspend fun registerAlias(rawText: String, brandId: Long): LedgerResult<Unit>
}
