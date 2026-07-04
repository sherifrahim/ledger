package com.sherif.ledger.feature.capture.sms

import android.content.Context
import android.provider.Telephony
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.notification.IngestionSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for scanning the system SMS provider for historical financial data.
 */
@Singleton
class SmsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processNotificationUseCase: ProcessNotificationUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend fun importHistoricalSms() = withContext(Dispatchers.IO) {
        val lastImportDate = userPreferencesRepository.lastSmsImportDate.first()
        var maxDate = lastImportDate
        com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsImporter: Historical Import Started. LastImportDate=$lastImportDate")

        val resolver = context.contentResolver
        val cursor = resolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(lastImportDate.toString()),
            "${Telephony.Sms.DATE} ASC"
        )

        val count = cursor?.count ?: 0
        com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsImporter: Messages Found = $count")

        cursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Sms._ID)
            val threadIdIndex = it.getColumnIndex(Telephony.Sms.THREAD_ID)
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            var processedCount = 0
            while (it.moveToNext()) {
                processedCount++
                val id = it.getString(idIndex)
                val threadId = it.getString(threadIdIndex)
                val sender = it.getString(addressIndex) ?: "Unknown"
                val body = it.getString(bodyIndex) ?: ""
                val date = it.getLong(dateIndex)
                val timestamp = Instant.ofEpochMilli(date)
                
                if (date > maxDate) maxDate = date

                val envelope = NotificationEnvelope(
                    packageName = sender,
                    title = "Imported SMS",
                    text = body,
                    subText = null,
                    timestamp = timestamp,
                    notificationKey = "sms_import_${id}_$sender",
                    source = IngestionSource.SMS,
                    extras = mapOf(
                        "_id" to id,
                        "thread_id" to threadId,
                        "address" to sender,
                        "timestamp" to date.toString()
                    )
                )

                com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsImporter: Processing Envelope $processedCount/$count (Sender=$sender)")
                processNotificationUseCase.execute(envelope)
            }
        }
        
        com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsImporter: Import Cycle Finished. MaxDate=$maxDate")
        userPreferencesRepository.setLastSmsImportDate(maxDate)
    }
}
