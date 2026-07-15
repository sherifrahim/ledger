package com.sherif.ledger.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the notification's inline "Add Note" reply — the note is typed
 * directly in the notification shade, no app launch. Writes through
 * TransactionRepository.updateNote, the same path Transaction Details' own
 * note editing uses.
 */
@AndroidEntryPoint
class AddNoteReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidTransactionCaptureNotifier.ACTION_ADD_NOTE) return
        val transactionId = intent.getLongExtra(AndroidTransactionCaptureNotifier.EXTRA_TRANSACTION_ID, -1L)
        val noteText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(AndroidTransactionCaptureNotifier.REMOTE_INPUT_NOTE_KEY)
            ?.toString()
        if (transactionId <= 0 || noteText.isNullOrBlank()) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                transactionRepository.updateNote(transactionId, noteText)
                LedgerLogger.d("AddNoteReceiver: note saved for txn#$transactionId")

                // Acknowledge the reply in-place so the notification shows the
                // note was saved, rather than sitting with a stale action.
                val repliedNotification = NotificationCompat.Builder(context, AndroidTransactionCaptureNotifier.CHANNEL_ID)
                    .setSmallIcon(com.sherif.ledger.R.mipmap.ic_launcher)
                    .setContentText("Note saved: $noteText")
                    .build()
                NotificationManagerCompat.from(context).notify(transactionId.toInt(), repliedNotification)
            } catch (e: Exception) {
                LedgerLogger.e("AddNoteReceiver: failed for txn#$transactionId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}


