package com.sherif.ledger.feature.capture.notification

import javax.inject.Inject

/**
 * Filter responsible for identifying potentially financial notifications.
 * It does not perform bank-specific logic, only broad eligibility checks.
 */
class NotificationFilter @Inject constructor() {

    // Initial whitelist of potential financial app packages.
    // This will expand as more parsers are added.
    private val financialPackages = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay (India)
        "com.google.android.apps.walletnfcrel", // Google Wallet
        "com.oppo.market", // Example for regional bank apps
        "com.adcb.mobileapp", // ADCB
        "com.mashreq.mobile", // Mashreq
        "com.emiratesnbd.mobile", // Emirates NBD
        "com.google.android.apps.messaging", // Google Messages (SMS bank alerts)
        "com.samsung.android.messaging" // Samsung Messages
    )

    fun shouldProcess(envelope: NotificationEnvelope): Boolean {
        // SMS is handled via the system provider, so we only filter based on content
        // unless it's a notification from a messaging app.
        val isSms = envelope.source == IngestionSource.SMS
        
        if (!isSms && envelope.packageName !in financialPackages) return false
        if (envelope.text.isBlank() && envelope.title.isBlank()) return false
        
        // Broad keyword check to ignore non-transactional alerts (e.g. general marketing)
        val content = "${envelope.title} ${envelope.text}".lowercase()
        val transactionKeywords = listOf("spent", "paid", "received", "credited", "debited", "purchase", "aed", "inr")
        
        return transactionKeywords.any { it in content }
    }
}
