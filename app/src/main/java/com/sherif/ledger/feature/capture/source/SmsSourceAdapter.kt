package com.sherif.ledger.feature.capture.source

import android.telephony.SmsMessage
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import java.time.Instant
import javax.inject.Inject

/**
 * PUSH adapter for live incoming SMS. Single translation boundary between an
 * Android [SmsMessage] and the canonical [NotificationEnvelope]. SmsReceiver
 * invokes this instead of constructing envelopes itself.
 *
 * The bank is identified downstream from message CONTENT, not the sender.
 */
class SmsSourceAdapter @Inject constructor() : PushSourceAdapter<SmsMessage> {

    override val channel: SourceChannel = SourceChannel.SMS

    override fun toEnvelope(raw: SmsMessage): NotificationEnvelope? {
        val body = raw.messageBody ?: return null
        val sender = raw.originatingAddress ?: "Unknown"
        return NotificationEnvelope(
            packageName = sender,
            title = "SMS",
            text = body,
            subText = null,
            timestamp = Instant.ofEpochMilli(raw.timestampMillis),
            notificationKey = "sms_${raw.timestampMillis}_$sender",
            source = IngestionSource.SMS,
            extras = mapOf(
                "address" to sender,
                "timestamp" to raw.timestampMillis.toString(),
            ),
        )
    }
}
