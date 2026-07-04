package com.sherif.ledger.feature.capture.source

import android.service.notification.StatusBarNotification
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import java.time.Instant
import javax.inject.Inject

/**
 * PUSH adapter for Android notifications. The one fully-implemented adapter today.
 * Behavior-identical to the envelope logic previously inline in the listener.
 */
class NotificationSourceAdapter @Inject constructor() : PushSourceAdapter<StatusBarNotification> {

    override val channel: SourceChannel = SourceChannel.NOTIFICATION

    override fun toEnvelope(raw: StatusBarNotification): NotificationEnvelope {
        val extras = raw.notification.extras
        val mappedExtras = mutableMapOf<String, String>()
        extras.keySet().forEach { key ->
            @Suppress("DEPRECATION")
            extras.get(key)?.toString()?.let { mappedExtras[key] = it }
        }
        return NotificationEnvelope(
            packageName = raw.packageName ?: "unknown",
            title = extras.getCharSequence("android.title")?.toString() ?: "",
            text = extras.getCharSequence("android.text")?.toString() ?: "",
            subText = extras.getCharSequence("android.subText")?.toString(),
            timestamp = Instant.ofEpochMilli(raw.postTime),
            notificationKey = raw.key ?: "",
            extras = mappedExtras,
        )
    }
}
