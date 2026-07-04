package com.sherif.ledger.feature.capture.source

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import javax.inject.Inject

/**
 * PULL adapter STUB for a bank API. Not implemented.
 *
 * Declares its channel for registry + diagnostics. [fetchEnvelopes] throws until
 * the BankApi transport is built. Present so the registration mechanism is complete
 * and adding the real implementation later touches only this file.
 */
class BankApiSourceAdapter @Inject constructor() : PullSourceAdapter {
    override val channel: SourceChannel = SourceChannel.BANK_API
    override suspend fun fetchEnvelopes(): List<NotificationEnvelope> =
        throw NotImplementedError("BankApiSourceAdapter not yet implemented")
}
