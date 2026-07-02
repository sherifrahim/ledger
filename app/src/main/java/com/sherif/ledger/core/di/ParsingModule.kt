package com.sherif.ledger.core.di

import com.sherif.ledger.feature.capture.parsing.AdcbParser
import com.sherif.ledger.feature.capture.parsing.BankParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ParsingModule {

    @Binds
    @IntoSet
    abstract fun bindAdcbParser(parser: AdcbParser): BankParser
}
