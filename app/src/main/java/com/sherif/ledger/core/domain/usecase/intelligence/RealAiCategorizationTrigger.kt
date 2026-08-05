package com.sherif.ledger.core.domain.usecase.intelligence

import com.sherif.ledger.core.common.logging.LedgerLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Before this existed, [AiCategorizationSweepUseCase] only ran once per app
 * launch ([com.sherif.ledger.LedgerApplication.onCreate]) — a genuinely
 * UNKNOWN merchant captured five minutes after the user last opened the app
 * sat uncategorised until the next cold start. A notification listener's
 * `execute()` call has to return quickly regardless — an AI HTTP round-trip
 * has no business sitting in front of "transaction captured" — so this
 * launches the sweep on its own scope and returns immediately.
 *
 * [Mutex.tryLock] rather than a queue: if a sweep is already in flight, this
 * capture doesn't need its own — the running sweep reads the full transaction
 * list, so it already picks up whatever was just persisted. Without this
 * guard, a burst of captures (e.g. a backlog of messages arriving at once)
 * would launch one overlapping sweep per message, each racing to be the one
 * that AI-categorises the same still-unresolved merchant — wasted API calls,
 * not wasted correctness (the deterministic tiers skip anything already
 * learned), but wasteful all the same.
 */
@Singleton
class RealAiCategorizationTrigger @Inject constructor(
    private val sweep: AiCategorizationSweepUseCase,
) : AiCategorizationTrigger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    override fun triggerAsync() {
        scope.launch {
            if (!mutex.tryLock()) return@launch
            try {
                val report = sweep.execute()
                if (report.considered > 0) LedgerLogger.d("Live-capture AI trigger: ${report.summary()}")
            } catch (e: Exception) {
                LedgerLogger.e("Live-capture AI trigger failed (non-fatal)", e)
            } finally {
                mutex.unlock()
            }
        }
    }
}
