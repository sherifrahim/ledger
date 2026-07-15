package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionType
import java.time.Instant

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BrandEntity::class,
            parentColumns = ["id"],
            childColumns = ["brand_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["brand_id"]),
        Index(value = ["category_id"]),
        Index(value = ["timestamp_millis"]),
        Index(value = ["fingerprint"], unique = true),
        Index(value = ["origin_package_name", "card_tail"]),
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    
    @ColumnInfo(name = "brand_id")
    val brandId: Long?,
    
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
    
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    
    @ColumnInfo(name = "currency_code")
    val currencyCode: com.sherif.ledger.core.domain.model.CurrencyCode,
    
    val type: TransactionType,
    
    @ColumnInfo(name = "timestamp_millis")
    val timestampEpochMillis: Long,
    
    val source: IngestionSource,
    
    @ColumnInfo(name = "raw_text")
    val rawText: String?,

    @ColumnInfo(name = "card_tail")
    val cardTail: String? = null,

    val fingerprint: String, // account_id + amount + brand_id + timestamp_bucket + raw_text_hash

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    // Normalized once at extraction time; see Transaction.transferDirection.
    @ColumnInfo(name = "transfer_direction")
    val transferDirection: com.sherif.ledger.core.domain.model.TransferDirection? = null,

    // Provenance evidence for AccountIdentityResolver. Plain columns, not a Room
    // @Embedded object, so the domain TransactionOrigin stays free of persistence
    // annotations — the mapper reconstructs it, same pattern as Money.
    @ColumnInfo(name = "origin_package_name")
    val originPackageName: String? = null,

    @ColumnInfo(name = "origin_sender_identity")
    val originSenderIdentity: String? = null,

    // User-authored annotation — single note per transaction, editable in place.
    // Never consulted by any balance/analytics computation.
    val note: String? = null,

    @ColumnInfo(name = "note_updated_at")
    val noteUpdatedAt: Long? = null,
)




