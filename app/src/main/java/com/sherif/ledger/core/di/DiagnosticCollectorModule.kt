package com.sherif.ledger.core.di

import com.sherif.ledger.core.domain.service.diagnostic.AccountCollector
import com.sherif.ledger.core.domain.service.diagnostic.AppInfoCollector
import com.sherif.ledger.core.domain.service.diagnostic.DatabaseHealthCollector
import com.sherif.ledger.core.domain.service.diagnostic.DiagnosticCollector
import com.sherif.ledger.core.domain.service.diagnostic.FinancialTraceCollector
import com.sherif.ledger.core.domain.service.diagnostic.LiveLogCollector
import com.sherif.ledger.core.domain.service.diagnostic.NotificationCollector
import com.sherif.ledger.core.domain.service.diagnostic.PipelineCollector
import com.sherif.ledger.core.domain.service.diagnostic.RelationshipCollector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Every diagnostic the bundle exports, in one place. Adding a new one later —
 * for Split, Notes, Budgets, whatever comes next — means adding exactly one
 * @Binds @IntoSet function here; DiagnosticBundleGenerator and the Developer
 * Console's diagnostics tab never need to change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticCollectorModule {

    @Binds
    @IntoSet
    abstract fun bindFinancialTraceCollector(collector: FinancialTraceCollector): DiagnosticCollector

    @Binds
    @IntoSet
    abstract fun bindAccountCollector(collector: AccountCollector): DiagnosticCollector

    @Binds
    @IntoSet
    abstract fun bindRelationshipCollector(collector: RelationshipCollector): DiagnosticCollector

    @Binds
    @IntoSet
    abstract fun bindPipelineCollector(collector: PipelineCollector): DiagnosticCollector

    @Binds
    @IntoSet
    abstract fun bindDatabaseHealthCollector(collector: DatabaseHealthCollector): DiagnosticCollector

    @Binds
    @IntoSet
    abstract fun bindNotificationCollector(collector: NotificationCollector): DiagnosticCollector

    @Binds
    @IntoSet
    abstract fun bindAppInfoCollector(collector: AppInfoCollector): DiagnosticCollector

    @Binds
    @IntoSet
    abstract fun bindLiveLogCollector(collector: LiveLogCollector): DiagnosticCollector
}



