package com.sherif.ledger.feature.capture.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase
import com.sherif.ledger.feature.capture.source.SmsSourceAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver for incoming SMS messages. Delegates envelope construction to
 * [SmsSourceAdapter] (the single translation boundary) and triggers the pipeline.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var processNotificationUseCase: ProcessNotificationUseCase

    @Inject
    lateinit var smsSourceAdapter: SmsSourceAdapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val envelope = smsSourceAdapter.toEnvelope(sms) ?: continue
            scope.launch {
                processNotificationUseCase.execute(envelope, smsSourceAdapter.channel)
            }
        }
    }
}
