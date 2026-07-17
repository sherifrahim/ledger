package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class ImportSummaryDto(
    val windowLabel: String,
    val windowStartMillis: Long,
    val windowEndMillis: Long,
    val smsScanned: Long,
    val smsWithinWindow: Long,
    val smsIgnoredOutsideWindow: Long,
    val smsMatched: Long,
    val transactionsCreated: Long,
    val transactionsMerged: Long,
    val transactionsDiscarded: Long,
)

/**
 * Part 4: makes the historical SMS import explainable after the fact — why a
 * user ended up with a given number of imported transactions. Reads the
 * summary [SmsImporter][com.sherif.ledger.feature.capture.sms.SmsImporter]
 * persists at the end of each run; empty/zeroed fields mean no import has run
 * yet on this install, not an error.
 */
class ImportSummaryCollector @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : DiagnosticCollector {

    override val id: String = "import_summary"

    override suspend fun collect(): DiagnosticSection {
        val summary = userPreferencesRepository.importSummary.first()
        val dto = ImportSummaryDto(
            windowLabel = summary.windowLabel,
            windowStartMillis = summary.windowStartMillis,
            windowEndMillis = summary.windowEndMillis,
            smsScanned = summary.smsScanned,
            smsWithinWindow = summary.smsWithinWindow,
            smsIgnoredOutsideWindow = summary.smsIgnoredOutsideWindow,
            smsMatched = summary.smsMatched,
            transactionsCreated = summary.transactionsCreated,
            transactionsMerged = summary.transactionsMerged,
            transactionsDiscarded = summary.transactionsDiscarded,
        )
        val json = Json { prettyPrint = true }
        return DiagnosticSection.Json(id, json.encodeToString(dto))
    }
}
