package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A label the user invented, attached to transactions by hand.
 *
 * Every other classification in Ledger is derived — the category comes from the
 * merchant registry, the account from the sender, the relationship from the
 * engine. A tag is the only one the user authors, which makes it the only place
 * they can express something the bank never said: "reimbursable", "Dubai trip",
 * "split with Dana". Nothing infers it and nothing overwrites it.
 *
 * [normalizedName] is what uniqueness is enforced on, so "Dubai Trip" and
 * "dubai trip" cannot both exist, while [name] preserves the capitalisation the
 * user actually typed.
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["normalized_name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** As the user typed it — this is what gets displayed. */
    val name: String,

    /** Lower-cased, whitespace-collapsed. The uniqueness key, never displayed. */
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * The many-to-many join between a transaction and its tags.
 *
 * A composite primary key rather than a surrogate id, so tagging the same
 * transaction twice with the same tag is impossible at the schema level rather
 * than merely guarded in code. Both sides cascade: deleting a transaction or a
 * tag cannot leave a dangling edge behind.
 */
@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transaction_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tag_id"])],
)
data class TransactionTagEntity(
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,

    @ColumnInfo(name = "tag_id")
    val tagId: Long,

    @ColumnInfo(name = "tagged_at")
    val taggedAt: Long = System.currentTimeMillis(),
)
