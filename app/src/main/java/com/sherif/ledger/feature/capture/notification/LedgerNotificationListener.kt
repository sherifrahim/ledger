package com.sherif.ledger.feature.capture.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint

/**
 * Android service that captures incoming notifications for transaction ingestion.
 * Focuses strictly on metadata capture as per DFC-05.1.
 */
@AndroidEntryPoint
class LedgerNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notification = sbn?.notification ?: return
        val extras = notification.extras

        val packageName = sbn.packageName ?: ""
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val timestamp = sbn.postTime

        // Forwarding logic will be implemented in future DFCs.
        // For DFC-05.1, we only capture the metadata.
    }
}
