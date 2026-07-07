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
 * Bank-agnostic, intent-aware heuristic extractor (Phase 4D).
 *
 * It understands financial LANGUAGE rather than bank templates. Using the shared
 * [FinancialPhraseLibrary], it decides — for any bank — whether a message is a
 * real financial EVENT, a CONFIRMATION of an existing one, or a non-transaction
 * (promotion, OTP, statement), then extracts amount, tails, merchant, and type
 * from the semantics.
 *
 * Decision order:
 *  1. OTP / statement -> terminal Ignore.
 *  2. Confirmation phrases with NO strong-event verb -> Confirmation (Phase 4C).
 *  3. Score event intent: positive evidence (amount, event verb, tail, merchant)
 *     minus negative evidence (offer phrases, weighted by polarity). Below the
 *     threshold with offer phrases present -> Ignore (Promotion/Offer). Otherwise
 *     -> Extracted with an inferred [TransactionType].
 *
 * Still the swappable FinancialExtractor: a future Gemma/Phi model replaces this
 * class and its DI binding, nothing else.
 */
class HeuristicExtractor @Inject constructor(
    private val normalizer: TextNormalizer,
    private val merchantNormalizer: MerchantNormalizer,
    private val phrases: FinancialPhraseLibrary,
) : FinancialExtractor {

    override val name: String = "heuristic"

    private val transactionThreshold = 70

    // Merchant patterns, bank-agnostic. A shared STOP boundary prevents greedy
    // over-capture (stops before on/using/via/from/with/to/of/card/account/...).
    // Order matters: specific "received from" / "sent to" before generic "to".
    private val stop = "(?:\\s+(?:on|using|via|from|with|to|of|card|account|a\\/c|for|ref|dated|avl|available)\\b|,\\s*[A-Z]{2,3}\\b|\\.|$)"
    private val merchantPatterns: List<Pattern> = listOf(
        Pattern.compile("\\breceived\\s+from\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsent\\s+to\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bupi\\s+to\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpayment\\s+to\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bat\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-\\.]+?)$stop", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:pos|merchant)\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bto\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
    )

    override fun canAttempt(envelope: NotificationEnvelope): Boolean = true

    override suspend fun extract(envelope: NotificationEnvelope): ExtractionResult {
        val text = normalizer.normalize("${envelope.title} ${envelope.text}")
        val lower = text.lowercase()

        // 1. Terminal ignores.
        phrases.matches(phrases.otpPhrases, text).takeIf { it.isNotEmpty() }?.let {
            return ExtractionResult.Ignore("OTP detected", name, "OTP", it, 96)
        }
        phrases.matches(phrases.statementPhrases, text).takeIf { it.isNotEmpty() }?.let {
            return ExtractionResult.Ignore("Statement detected", name, "Statement", it, 90)
        }

        // 2. Confirmation: acknowledges an EXISTING payment. Requires confirmation
        // phrases AND absence of a STRONG (money-leaving) event verb.
        val confirmPhrases = phrases.matches(phrases.confirmationPhrases, text)
        val strongEvent = phrases.containsAny(phrases.strongEventVerbs, text)
        if (confirmPhrases.isNotEmpty() && !strongEvent) {
            return ExtractionResult.Confirmation(
                amountMinor = ExtractionHelpers.extractAmountMinor(text),
                accountTail = ExtractionHelpers.extractAccountHint(text),
                matchedPhrases = confirmPhrases,
                extractorName = name,
                confidence = (60 + confirmPhrases.size * 15).coerceAtMost(97),
            )
        }

        // 3. Score event intent.
        val amount = ExtractionHelpers.extractAmountMinor(text)
        val tail = ExtractionHelpers.extractAccountHint(text)
        val currency = ExtractionHelpers.extractCurrency(text)
        val verbs = phrases.matches(phrases.eventVerbs, text)
        val offers = phrases.matches(phrases.promotionPhrases, text)
        val type = inferType(lower)
        val merchant = extractMerchant(text, type)

        val positive = mutableListOf<String>()
        var score = 0
        if (amount != null) { score += 40; positive += "amount" }
        if (verbs.isNotEmpty()) { score += 35; positive += "verb:${verbs.first()}" }
        if (tail != null) { score += 15; positive += "tail" }
        if (merchant != null) { score += 10; positive += "merchant" }

        // Negative evidence with polarity. If a real event verb is present, only
        // HARD offer phrases (eligible/apply now/pre-approved) still count against
        // it — this separates "loan disbursed/credited" (event) from "eligible for
        // a loan, apply now" (offer).
        val negative = mutableListOf<String>()
        val offersToPenalize = if (verbs.isEmpty()) offers else phrases.matches(phrases.hardOfferPhrases, text)
        offersToPenalize.forEach { score -= 30; negative += it }

        // Decision.
        if (offers.isNotEmpty() && score < transactionThreshold) {
            return ExtractionResult.Ignore(
                reason = "Promotion/offer detected",
                extractorName = name,
                category = classifyOffer(lower),
                matchedPhrases = offers,
                confidence = (100 - score).coerceIn(0, 100),
            )
        }
        if (amount == null || score < transactionThreshold) {
            return ExtractionResult.Ignore(
                reason = if (amount == null) "No transaction amount" else "Insufficient transaction evidence (score $score)",
                extractorName = name,
                category = "Unknown",
                matchedPhrases = offers,
                confidence = 0,
            )
        }

        // Real transaction.
        val reasoning = buildList {
            add("Detected amount $amount")
            if (currency != null) add("Detected currency $currency")
            if (verbs.isNotEmpty()) add("Event verb: ${verbs.first()}")
            if (tail != null) add("Detected tail $tail")
            if (merchant != null) add("Detected merchant $merchant")
            add("Inferred type $type")
            add("Intent score $score >= $transactionThreshold")
        }

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
            confidence = ExtractionConfidence(score.coerceIn(0, 100)),
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

    /** Bank-agnostic type inference from concept vocabularies (order matters). */
    private fun inferType(lower: String): TransactionType = when {
        phrases.containsAny(phrases.salaryPhrases, lower) -> TransactionType.INCOME
        phrases.containsAny(phrases.loanDisbursePhrases, lower) -> TransactionType.INCOME
        phrases.containsAny(phrases.refundPhrases, lower) -> TransactionType.REFUND
        phrases.containsAny(phrases.cardPaymentPhrases, lower) && lower.contains("card") -> TransactionType.TRANSFER
        phrases.containsAny(phrases.emiDeductionPhrases, lower) -> TransactionType.EXPENSE
        phrases.containsAny(phrases.transferPhrases, lower) -> TransactionType.TRANSFER
        phrases.containsAny(phrases.atmPhrases, lower) -> TransactionType.EXPENSE
        phrases.containsAny(phrases.interestPhrases, lower) ->
            if (lower.contains("earned") || lower.contains("credited") || lower.contains("profit")) TransactionType.INCOME
            else TransactionType.EXPENSE
        phrases.containsAny(phrases.feePhrases, lower) -> TransactionType.EXPENSE
        phrases.containsAny(phrases.depositPhrases, lower) && lower.contains("credited") -> TransactionType.INCOME
        lower.contains("credited") -> TransactionType.INCOME
        lower.contains("received") && !lower.contains("card") -> TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }

    /** Distinguish the flavour of offer for diagnostics only. */
    private fun classifyOffer(lower: String): String = when {
        lower.contains("loan") -> "Loan Offer"
        lower.contains("emi") -> "EMI Offer"
        lower.contains("credit limit") -> "Credit Limit Offer"
        lower.contains("insurance") -> "Insurance Offer"
        lower.contains("cashback") || lower.contains("reward") -> "Offer"
        else -> "Promotion"
    }

    private fun extractMerchant(text: String, type: TransactionType): String? {
        for (p in merchantPatterns) {
            val mt = p.matcher(text)
            if (mt.find()) {
                val raw = mt.group(1)?.trim()?.trimEnd(',', '.')
                if (!raw.isNullOrBlank() && raw.length in 2..48) {
                    return merchantNormalizer.normalize(raw)
                }
            }
        }
        return when (type) {
            TransactionType.INCOME -> "Income"
            TransactionType.TRANSFER -> "Transfer"
            TransactionType.REFUND -> "Refund"
            else -> null
        }
    }
}
