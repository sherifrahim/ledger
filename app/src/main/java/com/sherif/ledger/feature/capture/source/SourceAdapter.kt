package com.sherif.ledger.feature.capture.source

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope

/**
 * Identifies the transport an event arrived through. Distinct from the financial
 * institution, which is identified downstream from envelope CONTENT.
 *
 * Purely diagnostic. MUST NOT influence reconciliation or transaction identity.
 */
enum class SourceChannel {
    NOTIFICATION,
    SMS,
    SMS_IMPORT,
    WALLET,
    EMAIL,
    BANK_API,
}

/**
 * Common marker for every ingestion source. All adapters funnel to the canonical
 * [NotificationEnvelope], which flows through the unchanged pipeline:
 *
 *   SourceAdapter -> NotificationEnvelope -> ProcessNotificationUseCase
 *      -> ParserRegistry -> TransactionCandidate -> ReconciliationEngine -> Room -> UI
 *
 * Adding a source = implement one adapter + one @IntoMap binding. Nothing else moves.
 */
sealed interface SourceAdapter {
    val channel: SourceChannel
}

/**
 * PUSH sources are invoked by the OS with a single native input as it arrives
 * (Notification, live SMS). The transport calls [toEnvelope] per event.
 *
 * @param RawInput source-native type (StatusBarNotification, SmsMessage, ...).
 */
interface PushSourceAdapter<RawInput> : SourceAdapter {
    /**
     * Normalize one native input into an envelope, or null if it cannot be
     * represented (e.g. empty text). Filtering/parsing happen downstream.
     */
    fun toEnvelope(raw: RawInput): NotificationEnvelope?
}

/**
 * PULL sources are invoked by Ledger to fetch a batch (historical SMS import,
 * Wallet, Email, Bank API). The caller drives ingestion and receives envelopes.
 */
interface PullSourceAdapter : SourceAdapter {
    /**
     * Fetch and normalize a batch of events into envelopes. Implementations that
     * are not yet built throw NotImplementedError (they are visible stubs).
     */
    suspend fun fetchEnvelopes(): List<NotificationEnvelope>
}
