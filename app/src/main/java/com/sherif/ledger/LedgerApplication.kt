package com.sherif.ledger

import android.app.Application
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.usecase.event.BackfillFinancialEventsUseCase
import com.sherif.ledger.core.domain.usecase.event.ReadParityHarness
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LedgerApplication : Application() {

    @Inject lateinit var backfillFinancialEvents: BackfillFinancialEventsUseCase
    @Inject lateinit var readParityHarness: ReadParityHarness

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // ADR-0001 P5/P6: idempotent, self-healing FinancialEvent backfill, followed by a
        // read-parity check. Both run off the main thread; neither blocks startup nor
        // touches Financial Truth. When the event store is already complete the backfill
        // is a no-op scan; the parity harness then logs a domain-level comparison report.
        appScope.launch {
            try {
                val report = backfillFinancialEvents.execute()
                if (report.created > 0 || report.failures > 0) {
                    LedgerLogger.d("Startup backfill: ${report.summary()}")
                }
                val parity = readParityHarness.execute()
                if (!parity.proven) {
                    LedgerLogger.e("Read parity has UNEXPECTED differences: ${parity.summary()}")
                }
            } catch (e: Exception) {
                LedgerLogger.e("Startup event migration/parity failed (non-fatal)", e)
            }
        }
    }
}
