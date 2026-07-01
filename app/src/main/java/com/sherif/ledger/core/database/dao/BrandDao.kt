package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sherif.ledger.core.database.entity.BrandEntity
import com.sherif.ledger.core.database.entity.MerchantAliasEntity

@Dao
interface BrandDao {
    @Query("SELECT * FROM brands")
    suspend fun getAllBrands(): List<BrandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrand(brand: BrandEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchantAlias(alias: MerchantAliasEntity)

    @Transaction
    @Query("SELECT * FROM brands WHERE id IN (SELECT brand_id FROM merchant_aliases WHERE rawText = :rawText)")
    suspend fun getBrandByAlias(rawText: String): BrandEntity?
}
