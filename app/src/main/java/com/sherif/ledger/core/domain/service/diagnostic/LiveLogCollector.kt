package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.core.common.logging.LedgerLogBuffer
import javax.inject.Inject

/** The one collector whose output is log-format text, not JSON — chronological
 *  replay is the point, and re-flattening it into JSON would only make it
 *  harder to read for exactly the audience this is for. */
class LiveLogCollector @Inject constructor() : DiagnosticCollector {

    override val id: String = "ledger"

    override suspend fun collect(): DiagnosticSection =
        DiagnosticSection.LogText(id, LedgerLogBuffer.snapshotLogcatText())
}



