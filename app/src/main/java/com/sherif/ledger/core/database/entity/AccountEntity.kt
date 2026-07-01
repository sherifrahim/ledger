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
    
    @ColumnInfo(name = "currency_code")
    val currencyCode: CurrencyCode,
    
    @ColumnInfo(name = "account_number_tail")
    val accountNumberTail: String?,
    
    @ColumnInfo(name = "bank_brand_id")
    val bankBrandId: Long?,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
