package com.sherif.ledger.core.di

import com.sherif.ledger.core.common.diagnostics.PipelineTracker
import com.sherif.ledger.core.common.diagnostics.RealPipelineTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticModule {

    @Binds
    @Singleton
    abstract fun bindPipelineTracker(tracker: RealPipelineTracker): PipelineTracker
}
