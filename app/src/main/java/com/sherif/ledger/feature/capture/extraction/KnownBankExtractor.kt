package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.ParseResult
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import javax.inject.Inject

/**
 * Adapts the existing deterministic [ParserRegistry] into the [FinancialExtractor]
 * contract at high confidence. Keeps behavior byte-for-byte: parsing still runs
 * through the unchanged ParserRegistry.
 */
class KnownBankExtractor @Inject constructor(
    private val parserRegistry: ParserRegistry,
) : FinancialExtractor {

    override val name: String = "known-bank"
    private val confidence = ExtractionConfidence(99)

    override fun canAttempt(envelope: NotificationEnvelope): Boolean = true

    override suspend fun extract(envelope: NotificationEnvelope): ExtractionResult {
        return when (val result = parserRegistry.parse(envelope)) {
            is ParseResult.Success -> {
                val c = result.candidate
                ExtractionResult.Extracted(
                    candidate = c,
                    confidence = confidence,
                    fields = ExtractedFields(
                        amountMinor = c.amountMinor,
                        currency = c.currencyCode,
                        merchant = c.merchantName,
                        transactionType = c.transactionType,
                        accountTail = c.accountHint,
                        cardTail = c.accountHint,
                    ),
                    reasoning = listOf(
                        "Matched deterministic bank pattern",
                        "Merchant: ${c.merchantName}",
                        "Type: ${c.transactionType}",
                    ),
                    positiveEvidence = listOfNotNull(
                        c.amountMinor?.let { "amount" },
                        c.accountHint?.let { "tail" },
                        c.merchantName?.let { "merchant" },
                    ),
                    extractorName = name,
                )
            }
            ParseResult.Ignore ->
                ExtractionResult.Ignore("known-bank ignored", name, category = "Statement")
            is ParseResult.Failed ->
                ExtractionResult.NotApplicable(result.reason, name)
        }
    }
}
