package com.sherif.ledger.feature.notification

import com.sherif.ledger.core.domain.model.Transaction

/** Posting a "transaction captured" notification, abstracted behind an
 *  interface so ProcessNotificationUseCase's tests can inject a no-op fake
 *  rather than construct the real Android-framework-dependent implementation
 *  ([AndroidTransactionCaptureNotifier]) in a plain JVM test. */
interface TransactionNotifier {
    fun notifyCaptured(transaction: Transaction, merchantOrDescription: String, formattedAmount: String)
}

