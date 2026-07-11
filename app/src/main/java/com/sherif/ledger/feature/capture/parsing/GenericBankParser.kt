package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionOrigin
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.extraction.ExtractionHelpers
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Content-based fallback parser for any bank whose messages follow the common
 * "AED <amount> ... card/account <tail> ... at/to <merchant>" shape.
 *
 * Bank identity is derived from message CONTENT, not the package. This is what
 * lets one implementation cover Mashreq, Emirates NBD, FAB, and future banks,
 * including when their alerts arrive as SMS via the messaging app rather than
 * from the bank's own app.
 *
 * It supports every package but only actually parses a message that has both a
 * currency-anchored amount and a card/account tail, and is not an OTP or a
 * pure statement/promo. This keeps it from hijacking OTPs or non-financial text.
 *
 * Registered AFTER AdcbParser, so ADCB's specific patterns still win for ADCB.
 */
class GenericBankParser @Inject constructor(
    private val normalizer: TextNormalizer,
    private val merchantNormalizer: MerchantNormalizer,
) : BankParser {

    // "at MERCHANT" up to " on", a period, ",XX" country code, or end.
    private val atMerchant = Pattern.compile(
        "\\bat\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-,\\.]+?)(?:\\s+on\\b|\\.|,[A-Z]{2}\\b|$)",
        Pattern.CASE_INSENSITIVE,
    )
    // "to MERCHANT with"
    private val toMerchant = Pattern.compile(
        "\\bto\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)\\s+with\\b",
        Pattern.CASE_INSENSITIVE,
    )

    override val priority: Int = 100

    override fun supports(envelope: NotificationEnvelope): Boolean = true

    override fun parse(envelope: NotificationEnvelope): ParseResult {
        val text = normalizer.normalize("${envelope.title} ${envelope.text}")
        val lower = text.lowercase()

        // Ignore non-financial or non-transactional messages.
        if (lower.contains("otp") ||
            lower.contains("verification code") ||
            lower.contains("one time password")
        ) {
            return ParseResult.Ignore
        }

        val amount = ExtractionHelpers.extractAmountMinor(text)
        val account = ExtractionHelpers.extractAccountHint(text)

        // Only handle messages that clearly describe a transaction.
        if (amount == null || account == null) {
            return ParseResult.Failed("GenericBankParser: no anchored amount or card/account tail.")
        }

        val currency = ExtractionHelpers.extractCurrency(text)
        val type = detectType(lower)
        val merchant = extractMerchant(text, type)
        // Direction is decided HERE, once, alongside the type decision that already
        // reads this text — never re-derived downstream by BalanceCalculator or
        // analytics.
        val direction = if (type == TransactionType.TRANSFER) {
            ExtractionHelpers.inferTransferDirection(lower)
        } else null

        LedgerLogger.pipeline(
            "GenericBank",
            "amount=$amount currency=$currency card=$account type=$type merchant=$merchant",
        )

        return ParseResult.Success(
            TransactionCandidate(
                source = envelope.source,
                rawText = text,
                merchantName = merchant,
                amountMinor = amount,
                currencyCode = currency,
                timestamp = envelope.timestamp,
                accountHint = account,
                transactionType = type,
                transferDirection = direction,
                origin = TransactionOrigin(envelope.packageName, null),
            ),
        )
    }

    // Transfer signals shared with the semantic language library so the parser
    // and the heuristic agree: UPI/NEFT/IMPS/RTGS/"sent to"/remittance are all
    // transfers regardless of bank. Corpus-driven (Phase 4F): UPI SMS carry an
    // amount and an account tail, so this parser claims them at high confidence
    // and its type must match the transfer semantics.
    private val transferSignals = listOf(
        "transferred", "transfer to", "transfer of", " neft", " imps", " rtgs",
        " upi", "via upi", "via neft", "sent to", "remittance", "fund transfer",
        // Generalized transfer channels (bank-agnostic): a transfer described by
        // the channel it went through rather than a plain "transferred" verb.
        "transferred via", "transferred using", "via internet banking",
        "via mobile app", "personal internet banking",
    )

    private fun detectType(lower: String): TransactionType = when {
        lower.contains("salary") -> TransactionType.INCOME
        lower.contains("refund") || lower.contains("reversed") -> TransactionType.REFUND
        transferSignals.any { lower.contains(it) } -> TransactionType.TRANSFER
        lower.contains("withdrawn") || lower.contains("withdrawal") -> TransactionType.EXPENSE
        lower.contains("cr. transaction") || lower.contains("cr transaction") -> TransactionType.INCOME
        lower.contains("credited") -> TransactionType.INCOME
        lower.contains("received") && !lower.contains("card") -> TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }

    private fun extractMerchant(text: String, type: TransactionType): String? {
        val at = atMerchant.matcher(text)
        if (at.find()) {
            val raw = at.group(1)?.trim()?.trimEnd(',', '.')
            if (!raw.isNullOrBlank()) return merchantNormalizer.normalize(raw)
        }
        val to = toMerchant.matcher(text)
        if (to.find()) {
            val raw = to.group(1)?.trim()
            if (!raw.isNullOrBlank()) return merchantNormalizer.normalize(raw)
        }
        // Sensible fallbacks by type when no merchant delimiter is present.
        return when (type) {
            TransactionType.INCOME -> "Income"
            TransactionType.TRANSFER -> "Transfer"
            TransactionType.REFUND -> "Refund"
            else -> null
        }
    }
}




