package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.TransactionCandidate

/**
 * Common contract for bank-specific notification parsers.
 */
interface BankParser {
    /**
     * Attempts to parse raw notification text into a TransactionCandidate.
     */
    fun parse(packageName: String, title: String, text: String): LedgerResult<TransactionCandidate>
}
