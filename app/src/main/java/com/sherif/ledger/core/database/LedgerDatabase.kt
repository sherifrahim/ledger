package com.sherif.ledger.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sherif.ledger.core.database.converters.LedgerConverters
import com.sherif.ledger.core.database.dao.AccountDao
import com.sherif.ledger.core.database.dao.BrandDao
import com.sherif.ledger.core.database.dao.CategoryDao
import com.sherif.ledger.core.database.dao.TransactionDao
import com.sherif.ledger.core.database.entity.AccountEntity
import com.sherif.ledger.core.database.entity.BrandEntity
import com.sherif.ledger.core.database.entity.CategoryEntity
import com.sherif.ledger.core.database.entity.MerchantAliasEntity
import com.sherif.ledger.core.database.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BrandEntity::class,
        MerchantAliasEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(LedgerConverters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun brandDao(): BrandDao

    companion object {
        const val DATABASE_NAME = "ledger_db"
    }
}
