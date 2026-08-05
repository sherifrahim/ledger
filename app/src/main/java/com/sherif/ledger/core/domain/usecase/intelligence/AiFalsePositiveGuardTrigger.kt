package com.sherif.ledger.core.domain.usecase.intelligence

import com.sherif.ledger.core.domain.model.Transaction

/**
 * Fire-and-forget entry point for [AiFalsePositiveGuardUseCase] from the live
 * capture path. An interface for the same reason [AiCategorizationTrigger]
 * is one: the real implementation's dependency chain bottoms out in Android's
 * DataStore, which plain JVM unit tests (no Robolectric in this project)
 * cannot construct — a fake implementing this interface can stand in without
 * touching the framework.
 */
interface AiFalsePositiveGuardTrigger {
    /** Safe to call for any capture — the real implementation is a fast no-op when AI is off. */
    fun reviewAsync(transaction: Transaction, senderIdentifier: String, deterministicReasoning: List<String>)
}
