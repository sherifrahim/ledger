package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sherif.ledger.core.database.entity.MerchantCategoryOverrideEntity

@Dao
interface MerchantCategoryOverrideDao {
    @Query("SELECT * FROM merchant_category_overrides")
    suspend fun getAll(): List<MerchantCategoryOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MerchantCategoryOverrideEntity)
}
