package com.sherif.ledger.core.di

import android.content.Context
import androidx.room.Room
import com.sherif.ledger.core.database.LedgerDatabase
import com.sherif.ledger.core.database.dao.AccountDao
import com.sherif.ledger.core.database.dao.BrandDao
import com.sherif.ledger.core.database.dao.CategoryDao
import com.sherif.ledger.core.database.dao.TransactionDao
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
        ).build()
    }

    @Provides
    fun provideAccountDao(db: LedgerDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideTransactionDao(db: LedgerDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: LedgerDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBrandDao(db: LedgerDatabase): BrandDao = db.brandDao()
}
