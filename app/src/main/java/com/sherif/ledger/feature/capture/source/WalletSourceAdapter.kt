package com.sherif.ledger.feature.capture.source

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import javax.inject.Inject

/**
 * PULL adapter STUB for Google Wallet. Not implemented.
 *
 * Declares its channel for registry + diagnostics. [fetchEnvelopes] throws until
 * the Wallet transport is built. Present so the registration mechanism is complete
 * and adding the real implementation later touches only this file.
 */
class WalletSourceAdapter @Inject constructor() : PullSourceAdapter {
    override val channel: SourceChannel = SourceChannel.WALLET
    override suspend fun fetchEnvelopes(): List<NotificationEnvelope> =
        throw NotImplementedError("WalletSourceAdapter not yet implemented")
}
