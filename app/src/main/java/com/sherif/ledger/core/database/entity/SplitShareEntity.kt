package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "split_shares",
    foreignKeys = [
        ForeignKey(
            entity = SplitEntity::class,
            parentColumns = ["id"],
            childColumns = ["split_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["participant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["split_id"]),
        Index(value = ["participant_id"]),
        // One share row per participant per split — adding the same participant
        // twice is a repository-level no-op, not a duplicate row.
        Index(value = ["split_id", "participant_id"], unique = true),
    ],
)
data class SplitShareEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "split_id")
    val splitId: String,

    @ColumnInfo(name = "participant_id")
    val participantId: String,

    @ColumnInfo(name = "share_amount_minor")
    val shareAmountMinor: Long,

    val percentage: Double? = null,

    @ColumnInfo(name = "is_settled", defaultValue = "0")
    val isSettled: Boolean = false,

    @ColumnInfo(name = "settled_at")
    val settledAt: Long? = null,
)


