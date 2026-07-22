package com.sherif.ledger.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sherif.ledger.core.database.converters.LedgerConverters
import com.sherif.ledger.core.database.dao.AccountDao
import com.sherif.ledger.core.database.dao.AiAuditLogDao
import com.sherif.ledger.core.database.dao.BrandDao
import com.sherif.ledger.core.database.dao.CategoryDao
import com.sherif.ledger.core.database.dao.FinancialEventDao
import com.sherif.ledger.core.database.dao.LearnedDecisionDao
import com.sherif.ledger.core.database.dao.MerchantCategoryOverrideDao
import com.sherif.ledger.core.database.dao.ParticipantDao
import com.sherif.ledger.core.database.dao.SplitDao
import com.sherif.ledger.core.database.dao.TransactionDao
import com.sherif.ledger.core.database.entity.AccountEntity
import com.sherif.ledger.core.database.entity.AiAuditLogEntity
import com.sherif.ledger.core.database.entity.BrandEntity
import com.sherif.ledger.core.database.entity.CategoryEntity
import com.sherif.ledger.core.database.entity.FinancialEventEntity
import com.sherif.ledger.core.database.entity.LearnedDecisionEntity
import com.sherif.ledger.core.database.entity.MerchantAliasEntity
import com.sherif.ledger.core.database.entity.MerchantCategoryOverrideEntity
import com.sherif.ledger.core.database.entity.ParticipantEntity
import com.sherif.ledger.core.database.entity.SplitEntity
import com.sherif.ledger.core.database.entity.SplitShareEntity
import com.sherif.ledger.core.database.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BrandEntity::class,
        MerchantAliasEntity::class,
        ParticipantEntity::class,
        SplitEntity::class,
        SplitShareEntity::class,
        MerchantCategoryOverrideEntity::class,
        AiAuditLogEntity::class,
        LearnedDecisionEntity::class,
        FinancialEventEntity::class,
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(LedgerConverters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun brandDao(): BrandDao
    abstract fun participantDao(): ParticipantDao
    abstract fun splitDao(): SplitDao
    abstract fun merchantCategoryOverrideDao(): MerchantCategoryOverrideDao
    abstract fun aiAuditLogDao(): AiAuditLogDao
    abstract fun learnedDecisionDao(): LearnedDecisionDao
    abstract fun financialEventDao(): FinancialEventDao

    companion object {
        const val DATABASE_NAME = "ledger_db"
        // Kept in sync with the @Database(version = ...) annotation above by
        // hand — Room doesn't expose that value as a runtime constant, and
        // AppInfoCollector needs a real number for its diagnostic bundle.
        const val DATABASE_VERSION = 12
    }
}







