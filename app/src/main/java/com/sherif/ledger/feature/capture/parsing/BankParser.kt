package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope

/**
 * Common contract for bank-specific notification parsers.
 */
interface BankParser {
    /**
     * Determines if this parser can handle the given notification.
     */
    fun supports(envelope: NotificationEnvelope): Boolean

    /**
     * Attempts to parse a NotificationEnvelope into a ParseResult.
     */
    fun parse(envelope: NotificationEnvelope): ParseResult
}
