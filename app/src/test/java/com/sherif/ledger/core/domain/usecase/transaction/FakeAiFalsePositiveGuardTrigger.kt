package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.usecase.intelligence.AiFalsePositiveGuardTrigger

/** Shared across every test in this package that constructs ProcessNotificationUseCase — see FakeAiCategorizationTrigger for why this is one non-private top-level object rather than duplicated per file. */
internal object FakeAiFalsePositiveGuardTrigger : AiFalsePositiveGuardTrigger {
    override fun reviewAsync(transaction: Transaction, senderIdentifier: String, deterministicReasoning: List<String>) {
        // no-op: the real trigger's dependency chain bottoms out in Android's
        // DataStore (Context), out of scope for these plain JVM tests
    }
}
