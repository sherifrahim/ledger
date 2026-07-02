package com.sherif.ledger.feature.capture.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val envelope = sbn?.toEnvelope() ?: return

        serviceScope.launch {
            try {
                processNotificationUseCase.execute(envelope)
            } catch (e: Exception) {
                LedgerLogger.e("Pipeline crash in NotificationListener", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun StatusBarNotification.toEnvelope(): NotificationEnvelope {
        val extras = notification.extras
        val mappedExtras = mutableMapOf<String, String>()
        
        extras.keySet().forEach { key ->
            @Suppress("DEPRECATION")
            extras.get(key)?.toString()?.let { mappedExtras[key] = it }
        }

        return NotificationEnvelope(
            packageName = packageName ?: "unknown",
            title = extras.getCharSequence("android.title")?.toString() ?: "",
            text = extras.getCharSequence("android.text")?.toString() ?: "",
            subText = extras.getCharSequence("android.subText")?.toString(),
            timestamp = Instant.ofEpochMilli(postTime),
            notificationKey = key ?: "",
            extras = mappedExtras
        )
    }
}
