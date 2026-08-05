package com.sherif.ledger.core.domain.usecase.intelligence

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unlike [RealAiCategorizationTrigger]'s sweep (where a running pass already
 * covers whatever a second trigger would have found, so [Mutex.tryLock] can
 * safely skip), each call here reviews ONE specific just-inserted
 * transaction — skipping it would mean that transaction never gets checked at
 * all. [Mutex.withLock] instead: concurrent captures queue and run their AI
 * request one at a time (rate-limit friendly, matches the spacing philosophy
 * in [AiCategorizationSweepUseCase]) rather than being dropped.
 */
@Singleton
class RealAiFalsePositiveGuardTrigger @Inject constructor(
    private val guard: AiFalsePositiveGuardUseCase,
) : AiFalsePositiveGuardTrigger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    override fun reviewAsync(transaction: Transaction, senderIdentifier: String, deterministicReasoning: List<String>) {
        scope.launch {
            try {
                mutex.withLock {
                    val report = guard.review(transaction, senderIdentifier, deterministicReasoning)
                    if (report.removed) {
                        LedgerLogger.d("AiFalsePositiveGuard: removed txn #${transaction.id} — ${report.reason}")
                    }
                }
            } catch (e: Exception) {
                LedgerLogger.e("AiFalsePositiveGuard trigger failed (non-fatal)", e)
            }
        }
    }
}
