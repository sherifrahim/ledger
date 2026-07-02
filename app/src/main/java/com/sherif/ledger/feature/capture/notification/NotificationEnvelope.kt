package com.sherif.ledger.feature.capture.notification

import java.time.Instant

/**
 * Canonical representation of an Android notification within the Ledger ecosystem.
 * Isolates the parsing layer from platform-specific APIs (StatusBarNotification, Bundle).
 */
data class NotificationEnvelope(
    val packageName: String,
    val title: String,
    val text: String,
    val subText: String?,
    val timestamp: Instant,
    val notificationKey: String,
    val extras: Map<String, String> = emptyMap()
)
