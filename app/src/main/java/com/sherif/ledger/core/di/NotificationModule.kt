package com.sherif.ledger.core.di

import com.sherif.ledger.feature.notification.AndroidTransactionCaptureNotifier
import com.sherif.ledger.feature.notification.TransactionNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindTransactionNotifier(impl: AndroidTransactionCaptureNotifier): TransactionNotifier
}

