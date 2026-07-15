package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sherif.ledger.core.domain.model.SplitType

@Entity(
    tableName = "splits",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // One split per transaction in V1 — editing means mutating this row's
        // shares, never creating a second split for the same transaction.
        Index(value = ["transaction_id"], unique = true),
    ],
)
data class SplitEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,

    @ColumnInfo(name = "split_type")
    val splitType: SplitType,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

