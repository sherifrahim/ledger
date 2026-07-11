package com.sherif.ledger.core.di

import com.sherif.ledger.core.domain.service.account.AccountIdentityResolver
import com.sherif.ledger.core.domain.service.account.DeterministicAccountIdentityResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds [AccountIdentityResolver]. THE SWAP POINT for a future model-assisted
 * implementation: bind a different class here instead of
 * [DeterministicAccountIdentityResolver]. No downstream code changes, because
 * insertion, balance computation, and analytics all depend only on the interface.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountIdentityModule {

    @Binds
    abstract fun bindAccountIdentityResolver(
        resolver: DeterministicAccountIdentityResolver,
    ): AccountIdentityResolver
}

