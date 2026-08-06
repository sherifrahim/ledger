package com.sherif.ledger.feature.capture.source

import android.service.notification.StatusBarNotification
import com.sherif.ledger.feature.capture.notification.FinancialContentHeuristics
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
            text = resolveText(mappedExtras),
            subText = extras.getCharSequence("android.subText")?.toString(),
            timestamp = Instant.ofEpochMilli(raw.postTime),
            notificationKey = raw.key ?: "",
            extras = mappedExtras,
        )
    }

    companion object {
        /**
         * Pure (no Android framework types, so it's unit-testable without
         * Robolectric) resolution of which extras field holds the real
         * notification body. Some apps' notifications carry a redaction
         * placeholder in the standard android.text/android.bigText fields for
         * third-party listeners (observed: ADCB's com.adcb.nexgen posting
         * "Sensitive notification content hidden") while the real transaction
         * text sits in a non-standard extras key — here "content", a common
         * cross-platform push-SDK (FCM/Notifee) payload field, not
         * ADCB-specific. Rather than matching the placeholder string (fragile,
         * OEM/locale-dependent), prefer whichever candidate actually looks
         * financial — the same content-based-admission approach
         * NotificationFilter already uses for bank package-name drift. Falls
         * back to the standard field when nothing looks financial, so ordinary
         * notifications (and BigTextStyle notifications where bigText is
         * simply the fuller version of text) are unaffected either way.
         */
        fun resolveText(extras: Map<String, String>): String {
            val standardText = extras["android.text"] ?: ""
            val bigText = extras["android.bigText"] ?: ""
            return sequenceOf(bigText, standardText, extras["content"] ?: "")
                .firstOrNull { FinancialContentHeuristics.looksLikeFinancialAmount(it) }
                ?: standardText
        }
    }
}
