package com.sherif.ledger.core.di

import com.sherif.ledger.core.domain.usecase.intelligence.AiCategorizationTrigger
import com.sherif.ledger.core.domain.usecase.intelligence.AiFalsePositiveGuardTrigger
import com.sherif.ledger.core.domain.usecase.intelligence.RealAiCategorizationTrigger
import com.sherif.ledger.core.domain.usecase.intelligence.RealAiFalsePositiveGuardTrigger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IntelligenceModule {

    @Binds
    @Singleton
    abstract fun bindAiCategorizationTrigger(impl: RealAiCategorizationTrigger): AiCategorizationTrigger

    @Binds
    @Singleton
    abstract fun bindAiFalsePositiveGuardTrigger(impl: RealAiFalsePositiveGuardTrigger): AiFalsePositiveGuardTrigger
}
