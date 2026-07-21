package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.FinancialEventStatus
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionType

/**
 * Room persistence for the canonical [com.sherif.ledger.core.domain.model.FinancialEvent]
 * (ADR-0001). New, additive table `financial_events` — see MIGRATION_10_11.
 *
 * The only foreign key is to `transactions` (the originating record during
 * coexistence), matching the `splits` precedent. Enums persist natively as TEXT
 * (as `CurrencyCode` already does elsewhere). Every column/type/nullability/index
 * name here must match MIGRATION_10_11 exactly — Room validates the migrated
 * schema against these annotations at first open.
 */
@Entity(
    tableName = "financial_events",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["account_id"]),
        Index(value = ["timestamp_millis"]),
        Index(value = ["fingerprint"], unique = true),
        Index(value = ["status"]),
    ],
)
data class FinancialEventEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "transaction_id")
    val transactionId: Long?,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    @ColumnInfo(name = "brand_id")
    val brandId: Long?,

    @ColumnInfo(name = "category_id")
    val categoryId: Long?,

    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,

    @ColumnInfo(name = "currency_code")
    val currencyCode: CurrencyCode,

    val type: TransactionType,

    @ColumnInfo(name = "timestamp_millis")
    val timestampEpochMillis: Long,

    val source: IngestionSource,

    val confidence: Int,

    val status: FinancialEventStatus,

    @ColumnInfo(name = "supersedes_event_id")
    val supersedesEventId: String?,

    val fingerprint: String,

    @ColumnInfo(name = "raw_text")
    val rawText: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
