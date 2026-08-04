package com.sherif.ledger.core.di

import com.sherif.ledger.core.database.repository.EventSourcedTransactionReadSource
import com.sherif.ledger.core.database.repository.RoomAccountRepository
import com.sherif.ledger.core.database.repository.RoomFinancialEventRepository
import com.sherif.ledger.core.database.repository.RoomMerchantRepository
import com.sherif.ledger.core.database.repository.RoomParticipantRepository
import com.sherif.ledger.core.database.repository.RoomSplitRepository
import com.sherif.ledger.core.database.repository.RoomTagRepository
import com.sherif.ledger.core.database.repository.RoomTransactionRepository
import com.sherif.ledger.core.database.repository.RoomTransactionRunner
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.FinancialEventRepository
import com.sherif.ledger.core.domain.repository.MerchantRepository
import com.sherif.ledger.core.domain.repository.ParticipantRepository
import com.sherif.ledger.core.domain.repository.SplitRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.repository.TransactionRunner
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
    abstract fun bindTagRepository(repo: RoomTagRepository): com.sherif.ledger.core.domain.repository.TagRepository

    @Binds
    @Singleton
    abstract fun bindMerchantRepository(repo: RoomMerchantRepository): MerchantRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRunner(runner: RoomTransactionRunner): TransactionRunner

    @Binds
    @Singleton
    abstract fun bindParticipantRepository(repo: RoomParticipantRepository): ParticipantRepository

    @Binds
    @Singleton
    abstract fun bindSplitRepository(repo: RoomSplitRepository): SplitRepository

    @Binds
    @Singleton
    abstract fun bindFinancialEventRepository(repo: RoomFinancialEventRepository): FinancialEventRepository

    // ADR-0001 P7 — production list reads originate from FinancialEvent.
    @Binds
    @Singleton
    abstract fun bindTransactionReadSource(source: EventSourcedTransactionReadSource): TransactionReadSource
}


