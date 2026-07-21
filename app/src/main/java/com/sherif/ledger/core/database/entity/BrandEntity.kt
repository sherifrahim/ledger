package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brands")
data class BrandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    
    @ColumnInfo(name = "brand_key")
    val brandKey: String, // System B brand key; currently always "manual" (set by core.domain.service.transaction.MerchantResolver). NOT read by the UI-only LedgerBrandRegistry — see MERCHANT_ARCHITECTURE.md.
    
    @ColumnInfo(name = "default_category_id")
    val defaultCategoryId: Long?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
