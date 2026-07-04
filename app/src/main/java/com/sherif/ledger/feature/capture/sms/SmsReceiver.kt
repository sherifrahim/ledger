package com.sherif.ledger.feature.capture.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.notification.IngestionSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Receiver for incoming SMS messages.
 * Converts Telephony SMS into NotificationEnvelope and triggers the ingestion pipeline.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var processNotificationUseCase: ProcessNotificationUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.originatingAddress ?: "Unknown"
            val body = sms.messageBody ?: ""
            val timestamp = Instant.ofEpochMilli(sms.timestampMillis)
            
            val envelope = NotificationEnvelope(
                packageName = sender,
                title = "SMS",
                text = body,
                subText = null,
                timestamp = timestamp,
                notificationKey = "sms_${sms.timestampMillis}_$sender",
                source = IngestionSource.SMS,
                extras = mapOf(
                    "address" to sender,
                    "timestamp" to sms.timestampMillis.toString()
                )
            )

            scope.launch {
                processNotificationUseCase.execute(envelope)
            }
        }
    }
}
