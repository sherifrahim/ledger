package com.sherif.ledger.feature.capture.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase
import com.sherif.ledger.feature.capture.source.NotificationSourceAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Android service that captures incoming notifications and initiates the ingestion pipeline.
 */
@AndroidEntryPoint
class LedgerNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var processNotificationUseCase: ProcessNotificationUseCase

    @Inject
    lateinit var notificationSourceAdapter: NotificationSourceAdapter

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        LedgerLogger.d("NotificationListener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        LedgerLogger.d("NotificationListener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Forensic Logging BEFORE filtering
        val notification = sbn.notification
        val extras = notification.extras
        
        val forensics = buildString {
            appendLine("--- NOTIFICATION FORENSICS ---")
            appendLine("Package: ${sbn.packageName}")
            appendLine("ID: ${sbn.id}")
            appendLine("Tag: ${sbn.tag}")
            appendLine("Category: ${notification.category}")
            appendLine("Channel ID: ${notification.channelId}")
            appendLine("Ticker: ${notification.tickerText}")
            appendLine("Title: ${extras.getCharSequence("android.title")}")
            appendLine("Text: ${extras.getCharSequence("android.text")}")
            appendLine("BigText: ${extras.getCharSequence("android.bigText")}")
            appendLine("SummaryText: ${extras.getCharSequence("android.summaryText")}")
            appendLine("Extras KeySet: ${extras.keySet().joinToString(", ")}")
            appendLine("------------------------------")
        }
        LedgerLogger.d(forensics)

        val envelope = notificationSourceAdapter.toEnvelope(sbn)

        serviceScope.launch {
            try {
                processNotificationUseCase.execute(envelope)
            } catch (e: Exception) {
                LedgerLogger.e("Pipeline crash in NotificationListener", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        LedgerLogger.d("Notification removed: ${sbn?.packageName}")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
