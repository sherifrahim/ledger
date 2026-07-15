package com.sherif.ledger.feature.capture.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.sherif.ledger.core.common.logging.LedgerLogger
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
 *
 * RC3: wraps execute() in try/catch, matching LedgerNotificationListener's
 * existing pattern. Before this, an exception anywhere inside the pipeline for
 * an SMS-sourced message failed completely silently — no log entry, no crash,
 * the transaction simply never appeared. This makes that failure mode visible;
 * it changes no financial logic or decision.
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
                try {
                    processNotificationUseCase.execute(envelope, smsSourceAdapter.channel)
                } catch (e: Exception) {
                    LedgerLogger.e("Pipeline crash in SmsReceiver", e)
                }
            }
        }
    }
}



