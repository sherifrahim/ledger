package com.sherif.ledger.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the notification's "Undo" action. No new domain logic — calls the
 * already-existing, already-soft-deleting TransactionRepository.deleteTransaction,
 * the same one every other "delete a transaction" path in the app already uses.
 */
@AndroidEntryPoint
class UndoTransactionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidTransactionCaptureNotifier.ACTION_UNDO) return
        val transactionId = intent.getLongExtra(AndroidTransactionCaptureNotifier.EXTRA_TRANSACTION_ID, -1L)
        if (transactionId <= 0) return

        NotificationManagerCompat.from(context).cancel(transactionId.toInt())

        val pendingResult = goAsync()
        scope.launch {
            try {
                transactionRepository.deleteTransaction(transactionId)
                LedgerLogger.d("UndoTransactionReceiver: undone txn#$transactionId")
            } catch (e: Exception) {
                LedgerLogger.e("UndoTransactionReceiver: failed for txn#$transactionId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

