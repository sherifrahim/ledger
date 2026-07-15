package com.sherif.ledger.feature.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.sherif.ledger.MainActivity
import com.sherif.ledger.R
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the "transaction captured" notification with contextual actions —
 * greenfield infrastructure, Ledger posts no notification of its own anywhere
 * else. Split opens the app (there's no split-creation screen to deep-link
 * into yet — a deliberate scope boundary for this delivery, not an oversight).
 * Add Note replies inline via RemoteInput, no app launch needed. Undo fires a
 * bare broadcast straight to the existing, already-soft-deleting
 * TransactionRepository.deleteTransaction — no new domain logic either way.
 */
@Singleton
class AndroidTransactionCaptureNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : TransactionNotifier {
    companion object {
        const val CHANNEL_ID = "transaction_captured"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val ACTION_UNDO = "com.sherif.ledger.action.UNDO_TRANSACTION"
        const val ACTION_ADD_NOTE = "com.sherif.ledger.action.ADD_NOTE"
        const val REMOTE_INPUT_NOTE_KEY = "note_text"
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Transaction captured",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Shown right after Ledger captures a new transaction"
        }
        manager.createNotificationChannel(channel)
    }

    /** No-op, silently, if POST_NOTIFICATIONS was never granted (Android 13+) —
     *  the app functions normally without this convenience either way. */
    override fun notifyCaptured(transaction: Transaction, merchantOrDescription: String, formattedAmount: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            LedgerLogger.d("TransactionCaptureNotifier: POST_NOTIFICATIONS not granted, skipping")
            return
        }
        ensureChannel()

        val notificationId = transaction.id.toInt()
        val requestCodeBase = transaction.id.toInt() * 10

        val splitIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transaction.id)
        }
        val splitPendingIntent = PendingIntent.getActivity(
            context, requestCodeBase + 1, splitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val undoIntent = Intent(context, UndoTransactionReceiver::class.java).apply {
            action = ACTION_UNDO
            putExtra(EXTRA_TRANSACTION_ID, transaction.id)
        }
        val undoPendingIntent = PendingIntent.getBroadcast(
            context, requestCodeBase + 2, undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val noteIntent = Intent(context, AddNoteReceiver::class.java).apply {
            action = ACTION_ADD_NOTE
            putExtra(EXTRA_TRANSACTION_ID, transaction.id)
        }
        val remoteInput = RemoteInput.Builder(REMOTE_INPUT_NOTE_KEY)
            .setLabel("Add a note")
            .build()
        val notePendingIntent = PendingIntent.getBroadcast(
            context, requestCodeBase + 3, noteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE, // RemoteInput requires a mutable PendingIntent
        )
        val addNoteAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit, "Add Note", notePendingIntent,
        ).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(merchantOrDescription)
            .setContentText("Captured $formattedAmount")
            .setAutoCancel(true)
            .setContentIntent(splitPendingIntent)
            .addAction(android.R.drawable.ic_menu_share, "Split", splitPendingIntent)
            .addAction(addNoteAction)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Undo", undoPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            LedgerLogger.e("TransactionCaptureNotifier: notify failed despite permission check", e)
        }
    }
}

