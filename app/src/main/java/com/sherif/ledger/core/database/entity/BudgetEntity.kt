package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A monthly spending ceiling the user set for one category.
 *
 * Stores only the intention — the limit — and never the progress against it.
 * How much has been spent this month is already answered by
 * [com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase]'s
 * category totals; caching it here would create a second number to keep in sync
 * with the first, which is exactly the mistake balances deliberately avoid (no
 * stored balance anywhere in this codebase — everything is replayed).
 *
 * One budget per category, enforced by a unique index rather than by convention.
 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category"], unique = true)],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** A [com.sherif.ledger.feature.merchant.MerchantCategory] name. */
    val category: String,

    @ColumnInfo(name = "limit_minor")
    val limitMinor: Long,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
