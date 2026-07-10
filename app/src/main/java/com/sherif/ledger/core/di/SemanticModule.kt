package com.sherif.ledger.core.di

import com.sherif.ledger.feature.semantic.DeterministicSemanticClassifier
import com.sherif.ledger.feature.semantic.SemanticEventClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the [SemanticEventClassifier]. THE SWAP POINT for Phase 7: replacing the
 * deterministic classifier with a future on-device model (Gemma/Phi) is a single
 * change here — bind the new implementation instead of
 * [DeterministicSemanticClassifier]. No downstream code changes, because the
 * pipeline depends only on the interface.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SemanticModule {

    @Binds
    abstract fun bindSemanticEventClassifier(
        classifier: DeterministicSemanticClassifier,
    ): SemanticEventClassifier
}

