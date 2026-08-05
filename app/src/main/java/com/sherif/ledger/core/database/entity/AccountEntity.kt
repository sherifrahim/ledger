package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["account_number_tail"])]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    
    val type: AccountType,
    
    @ColumnInfo(name = "opening_balance_minor")
    val openingBalanceMinor: Long,

    @ColumnInfo(name = "currency_code")
    val currencyCode: CurrencyCode,
    
    @ColumnInfo(name = "account_number_tail")
    val accountNumberTail: String?,
    
    @ColumnInfo(name = "bank_brand_id")
    val bankBrandId: Long?,

    // The card's TOTAL credit limit. No purchase SMS ever states it, so it comes
    // from the user once per card; paired with the bank's stated available limit it
    // yields the outstanding balance exactly. Null for non-credit accounts and for
    // cards whose limit has not been supplied yet.
    @ColumnInfo(name = "credit_limit_minor")
    val creditLimitMinor: Long? = null,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "is_candidate", defaultValue = "0")
    val isCandidate: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    // Epoch millis the opening balance is anchored to; null until a real-balance
    // correction sets it. Nullable + no default — existing rows migrate to null.
    // Kept LAST to match the migration's ADD COLUMN and avoid positional breakage.
    @ColumnInfo(name = "opening_balance_as_of")
    val openingBalanceAsOfMillis: Long? = null,

    // True for exactly the one account EnsureDefaultAccountUseCase created (or,
    // for an install that predates this column, the one row the migration backfills
    // to preserve current behavior). See Account.isDefault for why this exists as
    // an explicit column rather than a derived position. Kept LAST to match the
    // migration's ADD COLUMN and avoid positional breakage.
    @ColumnInfo(name = "is_default", defaultValue = "0")
    val isDefault: Boolean = false,
)

