package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-taught category for a specific raw merchant string, keyed by its
 * normalized form (uppercase, whitespace-collapsed — same normalization
 * [com.sherif.ledger.feature.merchant.MerchantResolver] already uses).
 * Consulted as a fallback AFTER the deterministic brand registry and generic
 * keyword classifier both fail to categorize a transaction — see
 * [com.sherif.ledger.feature.merchant.LearnedMerchantCategoryStore].
 */
@Entity(tableName = "merchant_category_overrides")
data class MerchantCategoryOverrideEntity(
    @PrimaryKey
    @ColumnInfo(name = "merchant_key")
    val merchantKey: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
