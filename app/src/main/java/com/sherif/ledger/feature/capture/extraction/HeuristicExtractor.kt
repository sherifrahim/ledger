package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.extraction.ExtractionHelpers
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Intent-aware heuristic extractor.
 *
 * It first answers "is this a financial EVENT?" before attempting field
 * extraction. A message with financial language but promotional intent (EMI
 * offers, loan offers, cashback, credit-limit increases, marketing) is NOT a
 * transaction and returns [ExtractionResult.Ignore] with diagnostics, never a
 * candidate.
 *
 * Intent is decided by scoring positive evidence (currency amount, transaction
 * verb, card/account tail, merchant) against negative evidence (promotion
 * phrases) using the shared [FinancialPhraseLibrary]. OTP and statement messages
 * are terminal ignores. Only when the score clears the threshold does it build a
 * candidate.
 *
 * This is still the swappable FinancialExtractor: a future Gemma/Phi model
 * replaces this class and DI binding, nothing else.
 */
class HeuristicExtractor @Inject constructor(
    private val normalizer: TextNormalizer,
    private val merchantNormalizer: MerchantNormalizer,
    private val phrases: FinancialPhraseLibrary,
) : FinancialExtractor {

    override val name: String = "heuristic"

    /** Score at or above which a message is treated as a real transaction. */
    private val transactionThreshold = 70

    private val atMerchant = Pattern.compile(
        "\\bat\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-,\\.]+?)(?:\\s+on\\b|\\.|,[A-Z]{2}\\b|$)",
        Pattern.CASE_INSENSITIVE,
    )
    private val toMerchant = Pattern.compile(
        "\\bto\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)\\s+with\\b",
        Pattern.CASE_INSENSITIVE,
    )

    override fun canAttempt(envelope: NotificationEnvelope): Boolean = true

    override suspend fun extract(envelope: NotificationEnvelope): ExtractionResult {
        val text = normalizer.normalize("${envelope.title} ${envelope.text}")

        // ----- Terminal ignores: OTP and statements -----
        val otp = phrases.matches(phrases.otpPhrases, text)
        if (otp.isNotEmpty()) {
            return ExtractionResult.Ignore(
                reason = "OTP detected",
                extractorName = name,
                category = "OTP",
                matchedPhrases = otp,
                confidence = 96,
            )
        }
        val statement = phrases.matches(phrases.statementPhrases, text)
        if (statement.isNotEmpty()) {
            return ExtractionResult.Ignore(
                reason = "Statement detected",
                extractorName = name,
                category = "Statement",
                matchedPhrases = statement,
                confidence = 90,
            )
        }

        // ----- Score financial-event intent -----
        val amount = ExtractionHelpers.extractAmountMinor(text)
        val tail = ExtractionHelpers.extractAccountHint(text)
        val currency = ExtractionHelpers.extractCurrency(text)
        val verbs = phrases.matches(phrases.transactionVerbs, text)
        val promos = phrases.matches(phrases.promotionPhrases, text)
        val merchant = extractMerchant(text, detectType(text.lowercase()))

        val positive = mutableListOf<String>()
        var score = 0
        if (amount != null) { score += 40; positive += "amount" }
        if (verbs.isNotEmpty()) { score += 30; positive += "verb:${verbs.first()}" }
        if (tail != null) { score += 15; positive += "tail" }
        if (merchant != null) { score += 10; positive += "merchant" }

        // Negative evidence: each promotion phrase strongly reduces the score.
        promos.forEach { score -= 25 }

        // ----- Decision -----
        if (promos.isNotEmpty() && score < transactionThreshold) {
            return ExtractionResult.Ignore(
                reason = "Promotion detected",
                extractorName = name,
                category = "Promotion",
                matchedPhrases = promos,
                confidence = (100 - score).coerceIn(0, 100),
            )
        }

        if (amount == null || score < transactionThreshold) {
            return ExtractionResult.Ignore(
                reason = if (amount == null) "No transaction amount" else "Insufficient transaction evidence (score $score)",
                extractorName = name,
                category = "Unknown",
                matchedPhrases = promos,
                confidence = 0,
            )
        }

        // ----- Real transaction: build candidate -----
        val type = detectType(text.lowercase())
        val reasoning = buildList {
            add("Detected amount $amount")
            if (currency != null) add("Detected currency $currency")
            if (verbs.isNotEmpty()) add("Transaction verb: ${verbs.first()}")
            if (tail != null) add("Detected tail $tail")
            if (merchant != null) add("Detected merchant $merchant")
            add("Inferred type $type")
            add("Intent score $score >= $transactionThreshold")
        }

        val confidence = ExtractionConfidence(score.coerceIn(0, 100))

        val candidate = TransactionCandidate(
            source = envelope.source,
            rawText = text,
            merchantName = merchant,
            amountMinor = amount,
            currencyCode = currency,
            timestamp = envelope.timestamp,
            accountHint = tail,
            transactionType = type,
        )

        return ExtractionResult.Extracted(
            candidate = candidate,
            confidence = confidence,
            fields = ExtractedFields(
                amountMinor = amount,
                currency = currency,
                merchant = merchant,
                transactionType = type,
                accountTail = tail,
                cardTail = tail,
            ),
            reasoning = reasoning,
            positiveEvidence = positive,
            extractorName = name,
        )
    }

    private fun detectType(lower: String): TransactionType = when {
        lower.contains("salary") -> TransactionType.INCOME
        lower.contains("refund") || lower.contains("reversed") -> TransactionType.REFUND
        lower.contains("transferred") || lower.contains("transfer") -> TransactionType.TRANSFER
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
        return when (type) {
            TransactionType.INCOME -> "Income"
            TransactionType.TRANSFER -> "Transfer"
            TransactionType.REFUND -> "Refund"
            else -> null
        }
    }
}
