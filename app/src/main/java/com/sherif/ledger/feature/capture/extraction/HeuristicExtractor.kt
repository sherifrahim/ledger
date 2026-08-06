package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.model.TransactionOrigin
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
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
    //
    // "is"/"was" and "(" were added after real captures ran the merchant name into
    // the sentence that followed it — "Your AED 160.00 purchase at Ounass UAE is
    // confirmed..." captured "Ounass Uae Is Confirmed" because nothing stopped
    // before the verb, and "ETISALAT HEAD OFFICE (PAY" (a message truncated
    // mid-abbreviation) failed to match at all, because "(" was outside the
    // capturable character class with no boundary to stop at instead — losing the
    // merchant entirely and rendering the row "Unknown".
    private val stop = "(?:\\s+(?:on|using|via|from|with|to|of|card|account|a\\/c|for|ref|dated|avl|available|is|was)\\b|,\\s*[A-Z]{2,3}\\b|\\(|\\.|$)"
    // The "at MERCHANT" pattern deliberately does NOT stop on "to": a real store
    // name can contain it ("DAY TO DAY HYPMKT"), and "at X to Y" is not a shape
    // this pattern otherwise needs to defend against. Every other pattern here
    // captures text that follows the word "to" itself, so removing it from their
    // boundary would risk swallowing a genuine second "to" clause; "at" is the one
    // pattern where "to" was never a real delimiter, only a false stop.
    private val stopAt = "(?:\\s+(?:on|using|via|from|with|of|card|account|a\\/c|for|ref|dated|avl|available|is|was)\\b|,\\s*[A-Z]{2,3}\\b|\\(|\\.|$)"
    private val merchantPatterns: List<Pattern> = listOf(
        Pattern.compile("\\breceived\\s+from\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsent\\s+to\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bupi\\s+to\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpayment\\s+to\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-]+?)$stop", Pattern.CASE_INSENSITIVE),
        // Comma is allowed here (and only here) because "at MERCHANT, CITY." is a
        // real message shape — GenericBankParser's equivalent pattern already
        // permits it. Without it, "at LULU HYPERMARKET WAHDA, ABUDHABI." could not
        // match at all: the comma fell outside the capturable characters with no
        // stop boundary positioned there either, so the whole pattern failed and
        // the row lost its merchant.
        Pattern.compile("\\bat\\s+([A-Za-z0-9][A-Za-z0-9 &'\\-\\.,]+?)$stopAt", Pattern.CASE_INSENSITIVE),
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
        // A confirmation needs a STRONG acknowledgement phrase (payment received /
        // outstanding balance / balance restored). Weak signals like "thank you"
        // alone appear in ordinary purchase receipts and must not classify here.
        val confirmPhrases = phrases.matches(phrases.strongConfirmationPhrases, text)
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

        // A promotional message whose only "amount" is a percentage (e.g. "10%
        // cashback") has no anchored currency amount. Reject it as an offer even
        // when a spend verb like "purchase" is present, so the bare number cannot
        // score as a transaction amount.
        if (offers.isNotEmpty() && !hasAnchoredAmount(text)) {
            return ExtractionResult.Ignore(
                reason = "Promotion/offer detected (no anchored amount)",
                extractorName = name,
                category = classifyOffer(lower),
                matchedPhrases = offers,
                confidence = 92,
            )
        }

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
        // A declined/failed transaction moved no money, but scores like a real one
        // (it quotes an amount, a card tail and a merchant), so it must be excluded
        // before the score is consulted.
        if (com.sherif.ledger.feature.capture.parsing.extraction.ExtractionHelpers
                .describesNonExecutedTransaction(lower)
        ) {
            return ExtractionResult.Ignore(
                reason = "Transaction did not execute (declined/failed)",
                extractorName = name,
                category = "Declined",
                matchedPhrases = emptyList(),
                confidence = 0,
            )
        }
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
            transferDirection = inferDirection(type, lower),
            origin = TransactionOrigin(envelope.packageName, null),
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
    private val anchoredAmount = Pattern.compile(
        "(?:AED|USD|INR|Rs\\.?|DIRHAM)\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE,
    )

    /** True only when a currency-anchored amount is present (not a bare percentage). */
    private fun hasAnchoredAmount(text: String): Boolean = anchoredAmount.matcher(text).find()

    private fun inferType(lower: String): TransactionType = when {
        phrases.containsAny(phrases.salaryPhrases, lower) -> TransactionType.INCOME
        // Real-device bug (2026-08-06, cross-checked against the owner's actual
        // captured notification via adb): "A Cr. transaction of AED 500.00 on
        // your account no. XXX920001 was successful." -- a real incoming ADCB
        // transfer from another person -- matched creditIndicatorPhrases
        // ("cr. transaction") and was recorded as INCOME, with merchantName
        // falling back to the literal word "Income" for lack of a real name.
        // salaryPhrases is checked FIRST (above) and found nothing salary-
        // specific in this text, so by the time creditIndicatorPhrases matches,
        // all we actually know is "money moved in, source unstated" -- TRANSFER
        // is the honest label for that, not INCOME, which claims earnings the
        // message never claimed. This is the bank-agnostic vocabulary (per its
        // own doc comment), so the fix applies to every bank using this same
        // generic "Cr./credited" template, not only ADCB.
        phrases.containsAny(phrases.creditIndicatorPhrases, lower) -> TransactionType.TRANSFER
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

    /**
     * Direction is decided HERE, once, alongside the type decision that already
     * reads this text — never re-derived downstream by BalanceCalculator or
     * analytics. A credit-card payment (money paid TOWARDS a card) is definitionally
     * outgoing regardless of wording, so it is special-cased using the SAME
     * cardPaymentPhrases signal [inferType] already checked, rather than guessed.
     * Any other transfer defers to the shared, bank-agnostic direction resolver.
     */
    private fun inferDirection(
        type: TransactionType,
        lower: String,
    ): TransferDirection? {
        if (type != TransactionType.TRANSFER) return null
        return if (phrases.containsAny(phrases.cardPaymentPhrases, lower) && lower.contains("card")) {
            TransferDirection.OUTGOING
        } else {
            ExtractionHelpers.inferTransferDirection(lower)
        }
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



