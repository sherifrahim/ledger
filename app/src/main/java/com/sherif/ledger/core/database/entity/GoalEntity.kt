package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Something the user is saving towards, funded by one of their accounts.
 *
 * Progress is deliberately not a column. A goal's balance IS the funding
 * account's balance, which [com.sherif.ledger.core.domain.service.transaction.AccountBalanceService]
 * already derives by replaying transactions — so "saved so far" updates itself as
 * money arrives, with nothing for the user to maintain by hand and no second
 * figure that can drift from the account it describes.
 *
 * The trade-off is stated rather than hidden: this measures the account, so a
 * goal funded by an account also used for daily spending will move with that
 * spending. Pointing a goal at a dedicated savings account is what makes it
 * meaningful, and that is a choice the user makes rather than one Ledger fakes
 * around with an invented "contributions" concept it has no way to observe.
 */
@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["account_id"])],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    @ColumnInfo(name = "target_minor")
    val targetMinor: Long,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    /** The account whose balance counts towards this goal. */
    @ColumnInfo(name = "account_id")
    val accountId: Long,

    /** Optional deadline, epoch millis. Null when the user set no date. */
    @ColumnInfo(name = "target_date_millis")
    val targetDateMillis: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
