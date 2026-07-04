package com.sherif.ledger.feature.capture.source

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import javax.inject.Inject

/**
 * PULL adapter STUB for email inbox. Not implemented.
 *
 * Declares its channel for registry + diagnostics. [fetchEnvelopes] throws until
 * the Email transport is built. Present so the registration mechanism is complete
 * and adding the real implementation later touches only this file.
 */
class EmailSourceAdapter @Inject constructor() : PullSourceAdapter {
    override val channel: SourceChannel = SourceChannel.EMAIL
    override suspend fun fetchEnvelopes(): List<NotificationEnvelope> =
        throw NotImplementedError("EmailSourceAdapter not yet implemented")
}
