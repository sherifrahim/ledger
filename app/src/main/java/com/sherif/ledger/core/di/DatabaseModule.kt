package com.sherif.ledger.core.di

import android.content.Context
import androidx.room.Room
import com.sherif.ledger.core.database.LedgerDatabase
import com.sherif.ledger.core.database.dao.AccountDao
import com.sherif.ledger.core.database.dao.BrandDao
import com.sherif.ledger.core.database.dao.CategoryDao
import com.sherif.ledger.core.database.dao.ParticipantDao
import com.sherif.ledger.core.database.dao.SplitDao
import com.sherif.ledger.core.database.dao.TransactionDao
import com.sherif.ledger.core.database.migration.MIGRATION_5_6
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LedgerDatabase {
        return Room.databaseBuilder(
            context,
            LedgerDatabase::class.java,
            LedgerDatabase.DATABASE_NAME
        )
            // Real migration, not a destructive fallback — this database now
            // holds real user financial history. Additive-only schema change
            // (nullable columns, new tables), so no data transform is needed.
            .addMigrations(MIGRATION_5_6)
            .build()
    }

    @Provides
    fun provideAccountDao(db: LedgerDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTransactionDao(db: LedgerDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: LedgerDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBrandDao(db: LedgerDatabase): BrandDao = db.brandDao()

    @Provides
    fun provideParticipantDao(db: LedgerDatabase): ParticipantDao = db.participantDao()

    @Provides
    fun provideSplitDao(db: LedgerDatabase): SplitDao = db.splitDao()
}


