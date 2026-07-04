package com.sherif.ledger.feature.capture.sms

import android.content.Context
import android.provider.Telephony
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase
import com.sherif.ledger.feature.capture.source.SmsImportSourceAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans the system SMS provider for historical financial data. Delegates envelope
 * construction to [SmsImportSourceAdapter] (the single translation boundary).
 */
@Singleton
class SmsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processNotificationUseCase: ProcessNotificationUseCase,
    private val smsImportSourceAdapter: SmsImportSourceAdapter,
    private val userPreferencesRepository: UserPreferencesRepository,
) {

    suspend fun importHistoricalSms() = withContext(Dispatchers.IO) {
        val lastImportDate = userPreferencesRepository.lastSmsImportDate.first()
        var maxDate = lastImportDate
        LedgerLogger.d("SmsImporter: Historical Import Started. LastImportDate=$lastImportDate")

        val resolver = context.contentResolver
        val cursor = resolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
            ),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(lastImportDate.toString()),
            "${Telephony.Sms.DATE} ASC",
        )

        val count = cursor?.count ?: 0
        LedgerLogger.d("SmsImporter: Messages Found = $count")

        cursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Sms._ID)
            val threadIdIndex = it.getColumnIndex(Telephony.Sms.THREAD_ID)
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            var processedCount = 0
            while (it.moveToNext()) {
                processedCount++
                val date = it.getLong(dateIndex)
                if (date > maxDate) maxDate = date

                val row = SmsImportSourceAdapter.ImportedSms(
                    id = it.getString(idIndex),
                    threadId = it.getString(threadIdIndex),
                    sender = it.getString(addressIndex) ?: "Unknown",
                    body = it.getString(bodyIndex) ?: "",
                    timestampMillis = date,
                )
                val envelope = smsImportSourceAdapter.convert(row)

                LedgerLogger.d("SmsImporter: Processing Envelope $processedCount/$count (Sender=${row.sender})")
                processNotificationUseCase.execute(envelope, smsImportSourceAdapter.channel)
            }
        }

        LedgerLogger.d("SmsImporter: Import Cycle Finished. MaxDate=$maxDate")
        userPreferencesRepository.setLastSmsImportDate(maxDate)
    }
}
