package com.sherif.ledger

import android.app.Application
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.usecase.event.BackfillFinancialEventsUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LedgerApplication : Application() {

    @Inject lateinit var backfillFinancialEvents: BackfillFinancialEventsUseCase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // ADR-0001 P5: idempotent, self-healing FinancialEvent backfill. Runs off the
        // main thread on every launch; when the event store is already complete it is a
        // no-op scan. Never blocks startup and never touches Financial Truth.
        appScope.launch {
            try {
                val report = backfillFinancialEvents.execute()
                if (report.created > 0 || report.failures > 0) {
                    LedgerLogger.d("Startup backfill: ${report.summary()}")
                }
            } catch (e: Exception) {
                LedgerLogger.e("Startup backfill failed (non-fatal)", e)
            }
        }
    }
}
