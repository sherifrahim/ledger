package com.sherif.ledger.feature.capture.notification

import com.sherif.ledger.core.domain.model.IngestionSource
import java.time.Instant

/**
 * Canonical representation of an Android notification or SMS within the Ledger ecosystem.
 * Isolates the parsing layer from platform-specific APIs.
 */
data class NotificationEnvelope(
    val packageName: String, // Acts as Sender ID for SMS
    val title: String,
    val text: String,
    val subText: String?,
    val timestamp: Instant,
    val notificationKey: String,
    val extras: Map<String, String> = emptyMap(),
    val source: IngestionSource = IngestionSource.NOTIFICATION
)
