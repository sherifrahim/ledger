package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope

/**
 * Strategy for recognizing and extracting transaction facts from specific notification formats.
 */
interface NotificationPattern {
    /** Returns true if the notification text matches this pattern's signature. */
    fun matches(text: String): Boolean

    /** Extracts financial facts from the envelope. Returns Success, Ignore, or Failed. */
    fun extract(envelope: NotificationEnvelope, normalizedText: String): ParseResult
}
