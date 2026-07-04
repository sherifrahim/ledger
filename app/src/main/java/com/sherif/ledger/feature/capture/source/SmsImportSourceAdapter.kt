package com.sherif.ledger.feature.capture.source

import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import java.time.Instant
import javax.inject.Inject

/**
 * PULL adapter for historical SMS import. Single translation boundary between a
 * stored SMS row and the canonical [NotificationEnvelope]. SmsImporter queries
 * content://sms and calls [convert] instead of constructing envelopes itself.
 */
class SmsImportSourceAdapter @Inject constructor() : PullSourceAdapter {

    override val channel: SourceChannel = SourceChannel.SMS_IMPORT

    override suspend fun fetchEnvelopes(): List<NotificationEnvelope> = emptyList()

    /** Normalize one imported SMS row (supplied by the importer transport) to an envelope. */
    fun convert(row: ImportedSms): NotificationEnvelope =
        NotificationEnvelope(
            packageName = row.sender,
            title = "Imported SMS",
            text = row.body,
            subText = null,
            timestamp = Instant.ofEpochMilli(row.timestampMillis),
            notificationKey = "sms_import_${row.id}_${row.sender}",
            source = IngestionSource.SMS,
            extras = mapOf(
                "_id" to row.id,
                "thread_id" to row.threadId,
                "address" to row.sender,
                "timestamp" to row.timestampMillis.toString(),
            ),
        )

    data class ImportedSms(
        val id: String,
        val threadId: String,
        val sender: String,
        val body: String,
        val timestampMillis: Long,
    )
}
