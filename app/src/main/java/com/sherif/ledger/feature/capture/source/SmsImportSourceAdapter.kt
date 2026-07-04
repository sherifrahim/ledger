package com.sherif.ledger.feature.capture.source

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import java.time.Instant
import javax.inject.Inject

/**
 * PULL adapter for historical SMS import (bulk backfill from the device SMS store).
 *
 * A future importer queries content://sms and passes rows to [convert]. The no-arg
 * [fetchEnvelopes] returns empty until that importer is wired, so the adapter is
 * registrable today with no ContentResolver dependency and no mutable state.
 */
class SmsImportSourceAdapter @Inject constructor() : PullSourceAdapter {

    override val channel: SourceChannel = SourceChannel.SMS_IMPORT

    override suspend fun fetchEnvelopes(): List<NotificationEnvelope> = emptyList()

    /** Normalize imported SMS rows (supplied by the importer transport) to envelopes. */
    fun convert(rows: List<ImportedSms>): List<NotificationEnvelope> =
        rows.mapNotNull { row ->
            if (row.body.isBlank()) null
            else NotificationEnvelope(
                packageName = "sms_import:${row.sender}",
                title = row.sender,
                text = row.body,
                subText = null,
                timestamp = Instant.ofEpochMilli(row.timestampMillis),
                notificationKey = "sms_import:${row.id}",
                extras = emptyMap(),
            )
        }

    data class ImportedSms(
        val id: Long,
        val sender: String,
        val body: String,
        val timestampMillis: Long,
    )
}
