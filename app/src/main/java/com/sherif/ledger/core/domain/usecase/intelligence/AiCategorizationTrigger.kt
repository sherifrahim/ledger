package com.sherif.ledger.core.domain.usecase.intelligence

/**
 * Fire-and-forget entry point for [AiCategorizationSweepUseCase] from the live
 * capture path ([com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase]).
 * An interface (not the concrete trigger directly) for the same reason
 * [com.sherif.ledger.feature.notification.TransactionNotifier] is one: the
 * real implementation's dependency chain bottoms out in Android's DataStore
 * (via [com.sherif.ledger.feature.ai.settings.AiSettingsRepository]'s
 * `Context`), which plain JVM unit tests (no Robolectric in this project)
 * cannot construct — a fake implementing this interface can stand in without
 * touching the framework.
 */
interface AiCategorizationTrigger {
    /** Safe to call on every capture — the real implementation is a fast no-op when AI is off. */
    fun triggerAsync()
}
