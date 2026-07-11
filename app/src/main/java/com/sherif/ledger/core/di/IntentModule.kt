package com.sherif.ledger.core.di

import com.sherif.ledger.feature.semantic.DeterministicFinancialIntentClassifier
import com.sherif.ledger.feature.semantic.FinancialIntentClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the [FinancialIntentClassifier]. THE SWAP POINT for Phase 7: replacing the
 * deterministic classifier with a future on-device model (Gemma/Phi) is a single
 * change here — bind the new implementation instead of
 * [DeterministicFinancialIntentClassifier]. No downstream code changes, because the
 * pipeline depends only on the interface.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class IntentModule {

    @Binds
    abstract fun bindFinancialIntentClassifier(
        classifier: DeterministicFinancialIntentClassifier,
    ): FinancialIntentClassifier
}

