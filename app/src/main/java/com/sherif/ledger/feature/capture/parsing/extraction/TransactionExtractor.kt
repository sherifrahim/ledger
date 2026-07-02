package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import javax.inject.Inject

/**
 * Orchestrates individual extractors to produce a TransactionCandidate from a notification.
 */
class TransactionExtractor @Inject constructor(
    private val normalizer: TextNormalizer,
    private val amountExtractor: AmountExtractor,
    private val currencyExtractor: CurrencyExtractor,
    private val merchantExtractor: MerchantExtractor,
    private val typeResolver: TransactionTypeResolver,
) {

    fun extract(envelope: NotificationEnvelope): TransactionCandidate {
        val normalizedText = normalizer.normalize("${envelope.title} ${envelope.text}")
        
        return TransactionCandidate(
            source = IngestionSource.SMS, // Default for notification-based ingestion
            rawText = normalizedText,
            merchantName = merchantExtractor.extract(normalizedText),
            amountMinor = amountExtractor.extract(normalizedText),
            currencyCode = currencyExtractor.extract(normalizedText),
            timestamp = envelope.timestamp,
            accountHint = extractAccountHint(normalizedText),
            transactionType = typeResolver.resolve(normalizedText)
        )
    }

    private fun extractAccountHint(text: String): String? {
        // Look for common patterns like X1234 or *1234 or ending in 1234
        val regex = Regex("(?:[Xx*]{1,4}|ending in )(\\d{4})")
        return regex.find(text)?.groupValues?.get(1)
    }
}
