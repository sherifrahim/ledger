package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.extraction.ExtractionHelpers
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.PatternEngine
import javax.inject.Inject

/**
 * ADCB-specific coordinator that defines matching patterns and leverages the shared PatternEngine.
 */
class AdcbParser @Inject constructor(
    private val patternEngine: PatternEngine,
    private val merchantNormalizer: MerchantNormalizer
) : BankParser {

    private val adcbPackage = "com.adcb.mobileapp"

    private val patterns = listOf(
        AdcbPurchasePattern(merchantNormalizer),
        AdcbCreditPattern(merchantNormalizer),
        AdcbTransferPattern(merchantNormalizer),
        AdcbRefundPattern(merchantNormalizer),
        AdcbStatementPattern(),
        AdcbBillingPattern(),
        AdcbOtpPattern()
    )

    override fun supports(envelope: NotificationEnvelope): Boolean {
        return envelope.packageName == adcbPackage
    }

    override fun parse(envelope: NotificationEnvelope): ParseResult {
        return patternEngine.extract(envelope, patterns)
    }
}

private class AdcbPurchasePattern(private val normalizer: MerchantNormalizer) : NotificationPattern {
    override fun matches(text: String): Boolean {
        return text.contains("purchase", ignoreCase = true) && text.contains(" at ", ignoreCase = true)
    }

    override fun extract(envelope: NotificationEnvelope, normalizedText: String): ParseResult {
        val amount = ExtractionHelpers.extractAmountMinor(normalizedText)
        val currency = ExtractionHelpers.extractCurrency(normalizedText)
        val account = ExtractionHelpers.extractAccountHint(normalizedText)
        val merchantRaw = normalizedText.substringAfter(" at ").substringBefore(".").trim()
        val merchantCanonical = normalizer.normalize(merchantRaw)

        return ParseResult.Success(
            TransactionCandidate(
                source = IngestionSource.SMS,
                rawText = normalizedText,
                merchantName = merchantCanonical,
                amountMinor = amount,
                currencyCode = currency,
                timestamp = envelope.timestamp,
                accountHint = account,
                transactionType = TransactionType.EXPENSE
            )
        )
    }
}

private class AdcbCreditPattern(private val normalizer: MerchantNormalizer) : NotificationPattern {
    override fun matches(text: String): Boolean {
        return text.contains("received", ignoreCase = true) || text.contains("credited", ignoreCase = true)
    }

    override fun extract(envelope: NotificationEnvelope, normalizedText: String): ParseResult {
        val amount = ExtractionHelpers.extractAmountMinor(normalizedText)
        val currency = ExtractionHelpers.extractCurrency(normalizedText)
        val account = ExtractionHelpers.extractAccountHint(normalizedText)
        val merchantRaw = normalizedText.substringAfter("from ").substringBefore(".").trim()
        val merchantCanonical = normalizer.normalize(merchantRaw)

        return ParseResult.Success(
            TransactionCandidate(
                source = IngestionSource.SMS,
                rawText = normalizedText,
                merchantName = merchantCanonical,
                amountMinor = amount,
                currencyCode = currency,
                timestamp = envelope.timestamp,
                accountHint = account,
                transactionType = TransactionType.INCOME
            )
        )
    }
}

private class AdcbTransferPattern(private val normalizer: MerchantNormalizer) : NotificationPattern {
    override fun matches(text: String): Boolean {
        return text.contains("transfer", ignoreCase = true)
    }

    override fun extract(envelope: NotificationEnvelope, normalizedText: String): ParseResult {
        val amount = ExtractionHelpers.extractAmountMinor(normalizedText)
        val currency = ExtractionHelpers.extractCurrency(normalizedText)
        val account = ExtractionHelpers.extractAccountHint(normalizedText)

        return ParseResult.Success(
            TransactionCandidate(
                source = IngestionSource.SMS,
                rawText = normalizedText,
                merchantName = "Internal Transfer",
                amountMinor = amount,
                currencyCode = currency,
                timestamp = envelope.timestamp,
                accountHint = account,
                transactionType = TransactionType.TRANSFER
            )
        )
    }
}

private class AdcbRefundPattern(private val normalizer: MerchantNormalizer) : NotificationPattern {
    override fun matches(text: String): Boolean {
        return text.contains("refund", ignoreCase = true) || text.contains("reversed", ignoreCase = true)
    }

    override fun extract(envelope: NotificationEnvelope, normalizedText: String): ParseResult {
        val amount = ExtractionHelpers.extractAmountMinor(normalizedText)
        val currency = ExtractionHelpers.extractCurrency(normalizedText)
        val account = ExtractionHelpers.extractAccountHint(normalizedText)
        val merchantRaw = normalizedText.substringAfter("from ").substringBefore(".").trim()
        val merchantCanonical = normalizer.normalize(merchantRaw)

        return ParseResult.Success(
            TransactionCandidate(
                source = IngestionSource.SMS,
                rawText = normalizedText,
                merchantName = merchantCanonical,
                amountMinor = amount,
                currencyCode = currency,
                timestamp = envelope.timestamp,
                accountHint = account,
                transactionType = TransactionType.REFUND
            )
        )
    }
}

private class AdcbStatementPattern : NotificationPattern {
    override fun matches(text: String): Boolean = text.contains("statement", ignoreCase = true)
    override fun extract(envelope: NotificationEnvelope, normalizedText: String): ParseResult = ParseResult.Ignore
}

private class AdcbBillingPattern : NotificationPattern {
    override fun matches(text: String): Boolean = text.contains("bill", ignoreCase = true) || text.contains("due", ignoreCase = true)
    override fun extract(envelope: NotificationEnvelope, normalizedText: String): ParseResult = ParseResult.Ignore
}

private class AdcbOtpPattern : NotificationPattern {
    override fun matches(text: String): Boolean {
        return text.contains("OTP", ignoreCase = true) || text.contains("verification code", ignoreCase = true)
    }

    override fun extract(envelope: NotificationEnvelope, normalizedText: String): ParseResult = ParseResult.Ignore
}
