package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope

/**
 * Common contract for bank-specific notification parsers.
 */
interface BankParser {
    /**
     * Ordering hint: parsers are tried from lowest priority value to highest.
     * Specific bank parsers use a low value; the generic fallback uses a high one.
     */
    val priority: Int get() = 0

    /**
     * Determines if this parser can handle the given notification.
     */
    fun supports(envelope: NotificationEnvelope): Boolean

    /**
     * Attempts to parse a NotificationEnvelope into a ParseResult.
     */
    fun parse(envelope: NotificationEnvelope): ParseResult
}
