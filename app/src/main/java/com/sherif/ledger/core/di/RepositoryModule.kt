package com.sherif.ledger.core.di

import com.sherif.ledger.core.database.repository.RoomAccountRepository
import com.sherif.ledger.core.database.repository.RoomBudgetRepository
import com.sherif.ledger.core.database.repository.RoomCategoryRepository
import com.sherif.ledger.core.database.repository.RoomInsightsRepository
import com.sherif.ledger.core.database.repository.RoomMerchantRepository
import com.sherif.ledger.core.database.repository.RoomTransactionRepository
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.BudgetRepository
import com.sherif.ledger.core.domain.repository.CategoryRepository
import com.sherif.ledger.core.domain.repository.InsightsRepository
import com.sherif.ledger.core.domain.repository.MerchantRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(repo: RoomAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(repo: RoomTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(repo: RoomCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindMerchantRepository(repo: RoomMerchantRepository): MerchantRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(repo: RoomBudgetRepository): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindInsightsRepository(repo: RoomInsightsRepository): InsightsRepository
}
