package com.sherif.ledger.core.domain.usecase.transaction

import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.feature.notification.TransactionNotifier

/**
 * Shared across every test in this package that constructs
 * ProcessNotificationUseCase. Previously declared as a separate
 * `private object FakeTransactionNotifier` inside both
 * ProcessNotificationUseCaseTest.kt and
 * ProcessNotificationUseCaseIntentRoutingTest.kt — that failed to compile:
 * `private` on a top-level object restricts which file can *reference* the
 * symbol, but does not scope the class Kotlin generates for it (still named
 * FakeTransactionNotifier in this package's namespace either way), so two
 * files in the same package declaring the same top-level object name collide
 * regardless of visibility. One shared, non-private declaration is the fix.
 */
internal object FakeTransactionNotifier : TransactionNotifier {
    override fun notifyCaptured(
        transaction: Transaction,
        merchantOrDescription: String,
        formattedAmount: String,
    ) {
        // no-op: notification posting requires the Android framework, out of
        // scope for these plain JVM tests
    }
}


