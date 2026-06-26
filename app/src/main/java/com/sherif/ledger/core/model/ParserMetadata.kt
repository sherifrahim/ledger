package com.sherif.ledger.core.model

import java.time.Instant

/**
 * Raw parser provenance attached to an automatically detected transaction.
 *
 * This type intentionally contains only captured parser facts and no business
 * rules or review decisions.
 */
data class ParserMetadata(
    val source: TransactionSource,
    val parserName: String,
    val parsedAt: Instant,
    val confidence: Confidence,
    val rawText: String? = null,
    val externalReference: String? = null,
) {
    init {
        require(parserName.isNotBlank()) { "Parser name cannot be blank." }
    }
}
