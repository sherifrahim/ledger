package com.sherif.ledger.feature.capture.source

import android.telephony.SmsMessage
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import java.time.Instant
import javax.inject.Inject

/**
 * PUSH adapter for live incoming SMS.
 *
 * Maps an [SmsMessage] to an envelope. The sender address becomes packageName-like
 * routing context; NotificationFilter and the parser identify the bank from the
 * message body (content), not the sender, so no per-sender whitelist is required
 * here. A future SmsReceiver (BroadcastReceiver) invokes this adapter.
 */
class SmsSourceAdapter @Inject constructor() : PushSourceAdapter<SmsMessage> {

    override val channel: SourceChannel = SourceChannel.SMS

    override fun toEnvelope(raw: SmsMessage): NotificationEnvelope? {
        val body = raw.messageBody ?: return null
        if (body.isBlank()) return null
        val sender = raw.originatingAddress ?: "sms"
        return NotificationEnvelope(
            packageName = "sms:$sender",
            title = sender,
            text = body,
            subText = null,
            timestamp = Instant.ofEpochMilli(raw.timestampMillis),
            notificationKey = "sms:$sender:${raw.timestampMillis}",
            extras = emptyMap(),
        )
    }
}
