package com.sherif.ledger.feature.capture.sms

import android.content.Context
import android.provider.Telephony
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.datastore.ImportSummary
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationOutcome
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase
import com.sherif.ledger.feature.capture.source.SmsImportSourceAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans the system SMS provider for historical financial data. Delegates envelope
 * construction to [SmsImportSourceAdapter] (the single translation boundary).
 *
 * RC2: the watermark advances after EACH message, not only once the whole cursor
 * completes. If the app is killed or backgrounded mid-import — routine Android OS
 * behavior, not an edge case — the previous once-at-the-end watermark meant the
 * entire historical backlog was reprocessed from scratch on the next launch. That
 * reprocessing wasn't a pure function of the SMS data alone: AccountIdentityResolver's
 * repeated-observation counter depends on what's already in the database at each
 * step, so replaying the same batch could cross the auto-creation threshold at a
 * different message than the first (interrupted) pass did, producing a different
 * set of accounts from identical input. Advancing per-message bounds an
 * interruption to reprocessing at most the one message in flight.
 *
 * [importStartDate]/[importEndDate] bound WHICH messages this run considers at
 * all (Part 2/3: user-chosen onboarding range, e.g. "This Week" vs "Last 12
 * Months") — never hardcoded here. [lastSmsImportDate] remains a separate,
 * narrower watermark so a second call (e.g. a retry after interruption) never
 * reprocesses messages already handled, even if called again with the same
 * (or a wider) date range.
 */
@Singleton
class SmsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processNotificationUseCase: ProcessNotificationUseCase,
    private val smsImportSourceAdapter: SmsImportSourceAdapter,
    private val userPreferencesRepository: UserPreferencesRepository,
) {

    suspend fun importHistoricalSms(
        importStartDate: Instant,
        importEndDate: Instant = Instant.now(),
        windowLabel: String = "",
    ): ImportResult = withContext(Dispatchers.IO) {
        val lastImportDate = userPreferencesRepository.lastSmsImportDate.first()
        val effectiveStartMillis = maxOf(importStartDate.toEpochMilli(), lastImportDate)
        val endMillis = importEndDate.toEpochMilli()
        LedgerLogger.d(
            "SmsImporter: Historical Import Started. window=[$importStartDate, $importEndDate] " +
                "lastImportDate=$lastImportDate effectiveStart=$effectiveStartMillis",
        )

        val resolver = context.contentResolver

        // Total inbox size, unfiltered — the denominator for "SMS Ignored
        // (Outside Window)" in Developer Console diagnostics (Part 4). A
        // separate, cheap projection-only query; never loaded into memory.
        val totalScanned = resolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            null,
            null,
            null,
        )?.use { it.count } ?: 0

        val cursor = resolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
            ),
            "${Telephony.Sms.DATE} > ? AND ${Telephony.Sms.DATE} <= ?",
            arrayOf(effectiveStartMillis.toString(), endMillis.toString()),
            "${Telephony.Sms.DATE} ASC",
        )

        val count = cursor?.count ?: 0
        LedgerLogger.d("SmsImporter: Messages Found = $count (of $totalScanned total in inbox)")

        var matched = 0
        var created = 0
        var merged = 0
        var discarded = 0

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

                val row = SmsImportSourceAdapter.ImportedSms(
                    id = it.getString(idIndex),
                    threadId = it.getString(threadIdIndex),
                    sender = it.getString(addressIndex) ?: "Unknown",
                    body = it.getString(bodyIndex) ?: "",
                    timestampMillis = date,
                )
                val envelope = smsImportSourceAdapter.convert(row)

                LedgerLogger.d("SmsImporter: Processing Envelope $processedCount/$count (Sender=${row.sender})")
                try {
                    // Historical import replays months of messages in one pass, so it
                    // must stay silent — the user asked to backfill history, not to be
                    // alerted about every transaction they already know about.
                    val outcome = processNotificationUseCase.execute(
                        envelope,
                        smsImportSourceAdapter.channel,
                        notifyUser = false,
                    )
                    if (outcome.filterAccepted) matched++
                    when (outcome.category) {
                        ProcessNotificationOutcome.Category.CREATED -> created++
                        ProcessNotificationOutcome.Category.MERGED -> merged++
                        ProcessNotificationOutcome.Category.DISCARDED -> discarded++
                    }
                } catch (e: Exception) {
                    LedgerLogger.e("SmsImporter: Failed to process SMS from ${row.sender}", e)
                    discarded++
                }

                // Advance the watermark now that this specific message has been
                // through the pipeline (success or handled failure) — not only
                // after the whole cursor completes. Bounds reprocessing on
                // interruption to at most this one message, not the full backlog.
                userPreferencesRepository.setLastSmsImportDate(date)
            }
        }

        userPreferencesRepository.setImportSummary(
            ImportSummary(
                windowLabel = windowLabel,
                windowStartMillis = importStartDate.toEpochMilli(),
                windowEndMillis = endMillis,
                smsScanned = totalScanned.toLong(),
                smsWithinWindow = count.toLong(),
                smsIgnoredOutsideWindow = (totalScanned - count).toLong().coerceAtLeast(0L),
                smsMatched = matched.toLong(),
                transactionsCreated = created.toLong(),
                transactionsMerged = merged.toLong(),
                transactionsDiscarded = discarded.toLong(),
            ),
        )

        LedgerLogger.d("SmsImporter: Import Cycle Finished. created=$created merged=$merged discarded=$discarded")
        ImportResult(found = count)
    }

    data class ImportResult(val found: Int)
}




