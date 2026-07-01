package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_aliases",
    foreignKeys = [
        ForeignKey(
            entity = BrandEntity::class,
            parentColumns = ["id"],
            childColumns = ["brand_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["brand_id"])]
)
data class MerchantAliasEntity(
    @PrimaryKey
    val rawText: String, // e.g. "AMZN MKTP"
    
    @ColumnInfo(name = "brand_id")
    val brandId: Long
)
