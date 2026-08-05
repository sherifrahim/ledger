package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.domain.usecase.intelligence.AiCategorizationTrigger

/** Shared across every test in this package that constructs ProcessNotificationUseCase — see FakeTransactionNotifier for why this is one non-private top-level object rather than duplicated per file. */
internal object FakeAiCategorizationTrigger : AiCategorizationTrigger {
    override fun triggerAsync() {
        // no-op: the real trigger's dependency chain bottoms out in Android's
        // DataStore (Context), out of scope for these plain JVM tests
    }
}
