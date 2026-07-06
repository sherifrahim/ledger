package com.sherif.ledger.core.di

import com.sherif.ledger.feature.capture.extraction.FinancialExtractor
import com.sherif.ledger.feature.capture.extraction.HeuristicExtractor
import com.sherif.ledger.feature.capture.extraction.KnownBankExtractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registers the [FinancialExtractor]s. THE SWAP POINT: adding an on-device model
 * later is one new class + one @Binds @IntoSet line here. Nothing else changes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractionModule {

    @Binds
    @IntoSet
    abstract fun bindKnownBankExtractor(extractor: KnownBankExtractor): FinancialExtractor

    @Binds
    @IntoSet
    abstract fun bindHeuristicExtractor(extractor: HeuristicExtractor): FinancialExtractor
}
