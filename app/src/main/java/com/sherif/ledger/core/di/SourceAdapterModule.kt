package com.sherif.ledger.core.di

import com.sherif.ledger.feature.capture.source.BankApiSourceAdapter
import com.sherif.ledger.feature.capture.source.EmailSourceAdapter
import com.sherif.ledger.feature.capture.source.NotificationSourceAdapter
import com.sherif.ledger.feature.capture.source.SmsImportSourceAdapter
import com.sherif.ledger.feature.capture.source.SmsSourceAdapter
import com.sherif.ledger.feature.capture.source.SourceAdapter
import com.sherif.ledger.feature.capture.source.SourceChannel
import com.sherif.ledger.feature.capture.source.SourceChannelKey
import com.sherif.ledger.feature.capture.source.WalletSourceAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

/**
 * Registers every SourceAdapter into a Map<SourceChannel, SourceAdapter>.
 *
 * Adding a new source = write one adapter + add one @Binds @IntoMap line here.
 * The transport layer looks its adapter up by channel; diagnostics label any
 * event by channel. Registration is the ONLY wiring a new source needs.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SourceAdapterModule {

    @Binds
    @IntoMap
    @SourceChannelKey(SourceChannel.NOTIFICATION)
    abstract fun bindNotification(a: NotificationSourceAdapter): SourceAdapter

    @Binds
    @IntoMap
    @SourceChannelKey(SourceChannel.SMS)
    abstract fun bindSms(a: SmsSourceAdapter): SourceAdapter

    @Binds
    @IntoMap
    @SourceChannelKey(SourceChannel.SMS_IMPORT)
    abstract fun bindSmsImport(a: SmsImportSourceAdapter): SourceAdapter

    @Binds
    @IntoMap
    @SourceChannelKey(SourceChannel.WALLET)
    abstract fun bindWallet(a: WalletSourceAdapter): SourceAdapter

    @Binds
    @IntoMap
    @SourceChannelKey(SourceChannel.EMAIL)
    abstract fun bindEmail(a: EmailSourceAdapter): SourceAdapter

    @Binds
    @IntoMap
    @SourceChannelKey(SourceChannel.BANK_API)
    abstract fun bindBankApi(a: BankApiSourceAdapter): SourceAdapter
}
