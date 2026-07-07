#!/bin/bash
set -e
echo "Phase 4F — Corpus-Driven Extraction Hardening (fixes 3 known gaps + adds UPI fixtures)..."

mkdir -p "app/src/main/java/com/sherif/ledger/feature/capture/extraction"
cat > "app/src/main/java/com/sherif/ledger/feature/capture/extraction/FinancialPhraseLibrary.kt" << 'LEDGEREOF'
package com.sherif.ledger.feature.capture.extraction

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reusable financial LANGUAGE library. Not a registry, not a subsystem — a plain
 * injectable holder of concept vocabularies with simple matching helpers, so
 * every extractor (and a future on-device model, for prompt grounding) shares
 * the same financial-language knowledge base instead of hardcoding bank templates.
 *
 * The goal is bank-agnostic understanding: recognize CONCEPTS (salary credit,
 * outward transfer, POS purchase, card payment, loan disbursement, EMI deduction,
 * fee, interest, refund, reversal) regardless of which bank sent the message.
 *
 * INTENT POLARITY is the key design point. Some words appear in both a real event
 * and a marketing message ("loan", "emi", "cashback"); disambiguation comes from
 * the VERB/CONTEXT, not the noun:
 *  - [eventVerbs] (broad) are positive evidence of a real money movement.
 *  - [strongEventVerbs] (narrow: money LEAVING for a NEW event) veto a
 *    confirmation classification only — they exclude "payment of"/"received" so
 *    payment-received acknowledgements still classify as confirmations.
 *  - [hardOfferPhrases] (eligible / apply now / pre-approved ...) are strong
 *    negative evidence even when a transaction verb is also present.
 *
 * Extend the lists here to teach Ledger new financial language; no extractor
 * code needs to change.
 */
@Singleton
class FinancialPhraseLibrary @Inject constructor() {

    // ---- Money-movement verbs (broad; positive evidence of a real event) ----
    val eventVerbs: List<String> = listOf(
        "debited", "credited", "spent", "withdrawn", "deposited", "transferred",
        "paid", "charged", "used for", "used at", "purchase", "purchased",
        "payment of", "debit of", "credit of", "transaction of", "disbursed",
        "refunded", "reversed", "posted to", "sent to", "fund transfer",
        "transfer of", "transfer to", "remittance", "received from",
        "using your card", "using card",
    )

    /** Kept for backward compatibility (Phase 4B). Alias of the broad verbs. */
    val transactionVerbs: List<String> get() = eventVerbs

    /**
     * Narrow "money leaving for a NEW event" verbs. Used ONLY to veto a
     * confirmation. Deliberately excludes "payment of" / "received".
     */
    val strongEventVerbs: List<String> = listOf(
        "debited", "spent", "withdrawn", "transferred", "charged",
        "used for", "used at", "purchased", "disbursed", "sent to",
    )

    // ---- Offer / marketing (negative evidence) ----
    val promotionPhrases: List<String> = listOf(
        "eligible", "apply now", "apply for", "pre-approved", "preapproved",
        "pre-qualified", "instant approval", "limited time", "offer", "avail",
        "upgrade your", "increase your credit", "special offer", "exclusive",
        "hurry", "don't miss", "book now", "redeem your", "win ", "voucher",
        "discount", "% off", "% cashback on", "reward points", "promotion",
        "promo", "activate now", "claim your", "interest-free for",
        "convert to easy emi", "convert to emi", "easy emi", "lucky draw",
    )

    /** Offer phrases so strongly promotional they outweigh a transaction verb. */
    val hardOfferPhrases: List<String> = listOf(
        "eligible", "apply now", "apply for", "pre-approved", "preapproved",
        "pre-qualified", "instant approval",
    )

    // ---- Terminal ignores ----
    val otpPhrases: List<String> = listOf(
        "otp", "verification code", "one-time password", "one time password",
        "secure code", "security code",
    )

    val statementPhrases: List<String> = listOf(
        "statement is ready", "statement generated", "download statement",
        "e-statement", "statement available", "monthly statement",
        "statement has been generated",
    )

    // ---- Confirmation (acknowledges an EXISTING payment; Phase 4C) ----
    val confirmationPhrases: List<String> = listOf(
        "payment received", "payment posted", "payment credited",
        "payment successfully received", "payment acknowledged",
        "payment has been posted", "has been received",
        "we have received your payment", "thank you", "outstanding balance",
        "minimum amount due", "available credit restored",
        "receipt of your payment", "successfully processed your payment",
    )

    /**
     * STRONG confirmation phrases: unambiguous acknowledgements of an existing
     * payment or balance-state updates. Unlike the full [confirmationPhrases]
     * list, this excludes weak signals like "thank you" that also appear in
     * ordinary purchase receipts. A confirmation is only classified when a strong
     * phrase is present, preventing purchase receipts ("Thank you for using your
     * card ... at MERCHANT") from being mistaken for confirmations.
     */
    val strongConfirmationPhrases: List<String> = listOf(
        "payment received", "payment posted", "payment credited",
        "payment successfully received", "payment acknowledged",
        "payment has been posted", "we have received your payment",
        "outstanding balance", "minimum amount due",
        "available credit restored", "receipt of your payment",
        "successfully processed your payment", "balance restored",
        "outstanding updated", "credit card outstanding",
    )

    /**
     * Credit indicators: bank shorthand for a credit to the account that the
     * verb list misses (e.g. "Cr. transaction"). Signals Income. Bank-agnostic;
     * many banks abbreviate credit/debit as Cr./Dr.
     */
    val creditIndicatorPhrases: List<String> = listOf(
        "cr. transaction", "cr transaction", "a/c credited", "acct credited",
        "amount credited", "credit transaction",
    )

    // ---- Concept vocabularies for TYPE inference (bank-agnostic) ----
    val salaryPhrases: List<String> = listOf(
        "salary", "payroll", "wps", "end of service", "gratuity",
    )
    val atmPhrases: List<String> = listOf(
        "atm", "cash withdrawal", "cash withdrawn",
    )
    val cardPaymentPhrases: List<String> = listOf(
        "towards", "card payment", "credit card payment", "bill payment",
        "towards your card", "towards credit card",
    )
    val transferPhrases: List<String> = listOf(
        "transferred", "transfer to", "transfer of", "imps", "neft", "rtgs",
        "upi", "sent to", "remittance", "fund transfer", "inward", "outward",
    )
    val refundPhrases: List<String> = listOf(
        "refund", "refunded", "reversed", "reversal", "merchant refund",
        "chargeback",
    )
    val loanDisbursePhrases: List<String> = listOf(
        "loan disbursed", "loan credited", "disbursement of",
        "loan amount credited", "has been disbursed",
    )
    val emiDeductionPhrases: List<String> = listOf(
        "emi of", "emi debited", "installment of", "instalment debited",
        "emi deducted",
    )
    val feePhrases: List<String> = listOf(
        "fee", "service charge", "annual fee", "processing charge",
    )
    val interestPhrases: List<String> = listOf(
        "interest earned", "interest credited", "profit credited",
        "interest charged", "profit earned",
    )
    val depositPhrases: List<String> = listOf(
        "deposited", "cash deposit", "cheque deposit", "cheque credited",
    )

    /**
     * Returns the phrases from [group] that appear (case-insensitive substring)
     * in [text]. Empty list means no match.
     */
    fun matches(group: List<String>, text: String): List<String> {
        val lower = text.lowercase()
        return group.filter { lower.contains(it) }
    }

    /** Convenience: does [text] contain any phrase from [group]? */
    fun containsAny(group: List<String>, text: String): Boolean =
        matches(group, text).isNotEmpty()
}

LEDGEREOF

mkdir -p "app/src/main/java/com/sherif/ledger/feature/capture/extraction"
cat > "app/src/main/java/com/sherif/ledger/feature/capture/extraction/HeuristicExtractor.kt" << 'LEDGEREOF'
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
    private val anchoredAmount = Pattern.compile(
        "(?:AED|USD|INR|Rs\\.?|DIRHAM)\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE,
    )

    /** True only when a currency-anchored amount is present (not a bare percentage). */
    private fun hasAnchoredAmount(text: String): Boolean = anchoredAmount.matcher(text).find()

    private fun inferType(lower: String): TransactionType = when {
        phrases.containsAny(phrases.salaryPhrases, lower) -> TransactionType.INCOME
        phrases.containsAny(phrases.creditIndicatorPhrases, lower) -> TransactionType.INCOME
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

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/axis"
cat > "app/src/test/resources/financial-corpus/india/axis/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your axis account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your axis card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/bob"
cat > "app/src/test/resources/financial-corpus/india/bob/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your bob account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your bob card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/canara"
cat > "app/src/test/resources/financial-corpus/india/canara/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your canara account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your canara card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/federal"
cat > "app/src/test/resources/financial-corpus/india/federal/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your federal account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your federal card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/hdfc"
cat > "app/src/test/resources/financial-corpus/india/hdfc/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs. 500.00 debited from a/c XXXX1234 for UPI to JOHN on 05-Jul. Avl bal Rs 12000.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 500.0}},
  {"rawMessage": "Salary of Rs 60000 credited to your HDFC account XXXX1234.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 60000.0}},
  {"rawMessage": "Your HDFC OTP is 112233. Valid for 5 minutes.", "expected": {"decision": "Ignored", "category": "OTP"}},
  {"rawMessage": "Rs.250.00 debited via UPI to john@okhdfcbank on 05-Jul-26. Ref 512345678901. Not you? Call HDFC.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 250.0}},
  {"rawMessage": "You've paid Rs 120 to Cafe Coffee Day via PhonePe UPI. UPI Ref 218765432109.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 120.0}},
  {"rawMessage": "Rs 500 received from RAHUL via Google Pay UPI to a/c XXXX1234.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 500.0}},
  {"rawMessage": "Payment of Rs 89.00 to Zomato successful via Paytm UPI. UPI transaction ID 334455667788.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 89.0}},
  {"rawMessage": "Rs 1500.00 sent to MEERA using BHIM UPI from account ending 7788.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 1500.0, "cardTail": "7788"}},
  {"rawMessage": "Get cashback up to Rs 100 on your next PhonePe transaction. Offer valid till Sunday.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/icici"
cat > "app/src/test/resources/financial-corpus/india/icici/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 1500.00 spent at BIG BAZAAR using card ending 4321.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 1500.0, "cardTail": "4321"}},
  {"rawMessage": "Interest of Rs 320.00 credited to your ICICI savings account XXXX7788.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 320.0}},
  {"rawMessage": "INR 450.00 debited via UPI to swiggy@icici through Google Pay. Ref 445566778899.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 450.0}},
  {"rawMessage": "Rs 2000 transferred to PRIYA via PhonePe UPI from your ICICI account XXXX9012.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 2000.0}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/idfc"
cat > "app/src/test/resources/financial-corpus/india/idfc/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your idfc account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your idfc card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/kotak"
cat > "app/src/test/resources/financial-corpus/india/kotak/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your kotak account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your kotak card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/pnb"
cat > "app/src/test/resources/financial-corpus/india/pnb/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your pnb account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your pnb card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/sbi"
cat > "app/src/test/resources/financial-corpus/india/sbi/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 250.00 debited via NEFT to RAVI from account XXXX5678.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 250.0}},
  {"rawMessage": "You are pre-approved for an SBI personal loan. Apply now!", "expected": {"decision": "Ignored", "category": "Loan Offer"}},
  {"rawMessage": "Dear Customer, Rs 300.00 debited from A/c XXXX5678 via UPI/GPay to grocery@oksbi on 05Jul26.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 300.0}},
  {"rawMessage": "Rs 75 paid to Auto Driver via BHIM UPI. UPI Ref No 998877665544.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 75.0}},
  {"rawMessage": "Win Rs 50 cashback on your first Paytm UPI payment this week. Apply now.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/adcb"
cat > "app/src/test/resources/financial-corpus/uae/adcb/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Your salary AED6000.00 has been credited to your account no. XXX920001 on Jul 3 2026 2:12PM. The available balance is AED9079.30.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 6000.0, "currency": "AED"}},
  {"rawMessage": "Notification Purchase of AED 50.00 at COSTA COFFEE with card ending 1234.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 50.0, "currency": "AED", "cardTail": "1234"}},
  {"rawMessage": "AED700.00 transferred via ADCB Personal Internet Banking / Mobile App from acc. no. XXX920001 on Jun 29 2026 5:06PM. Avl. bal. AED 1630.30.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 700.0, "currency": "AED"}},
  {"rawMessage": "AED3000.00 withdrawn from acc. XXX920001 on Jun 24 2026 6:01PM at ATM-Index EXC Hamdaan. Avl.Bal.AED958.80.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 3000.0, "currency": "AED"}},
  {"rawMessage": "Your debit card XXX5986 linked to acc. XXX920001 was used for USD21.00 on Jul 3 2026 3:25PM at ANTHROPIC CLAUD,US. Avl.Bal AED 8999.38.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 21.0}},
  {"rawMessage": "A Cr. transaction of AED 1245.00 on your account no. XXX920001 was successful.Available balance is AED2975.80.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 1245.0, "currency": "AED"}},
  {"rawMessage": "Your OTP for ADCB login is 445566. Do not share with anyone.", "expected": {"decision": "Ignored", "category": "OTP"}},
  {"rawMessage": "Your ADCB monthly statement is ready. Total due AED 500.", "expected": {"decision": "Ignored", "category": "Statement"}},
  {"rawMessage": "You are eligible for a personal loan of AED 50,000. Apply now for instant approval.", "expected": {"decision": "Ignored", "category": "Loan Offer"}},
  {"rawMessage": "AED 200 debited from your account towards FAB Credit Card.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 200.0}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/adib"
cat > "app/src/test/resources/financial-corpus/uae/adib/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "AED 120.50 debited from your ADIB account XXXX4455 at CARREFOUR.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 120.5}},
  {"rawMessage": "Annual fee of AED 100.00 debited from card ending 5566.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 100.0, "cardTail": "5566"}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/cbd"
cat > "app/src/test/resources/financial-corpus/uae/cbd/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Cheque of AED 3000.00 deposited to your CBD account XXXX1122.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 3000.0}},
  {"rawMessage": "EMI of AED 1,200 debited from your account XXXX1234 towards loan.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 1200.0}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/dib"
cat > "app/src/test/resources/financial-corpus/uae/dib/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "AED 75.00 sent to AHMED via fund transfer from A/C XXXX2233.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 75.0}},
  {"rawMessage": "Profit of AED 45.20 credited to your DIB savings account XXXX9012.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 45.2}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/enbd"
cat > "app/src/test/resources/financial-corpus/uae/enbd/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Purchase of AED 37.75 with Credit Card ending 8165 at Noon Minutes, Dubai. Avl Cr. Limit is AED 12,441.91", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 37.75, "cardTail": "8165"}},
  {"rawMessage": "Payment of AED 2.25 to Noon Food with Credit Card ending 8165. Avl Cr. Limit is AED 11,779.66.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 2.25, "cardTail": "8165"}},
  {"rawMessage": "Your Emirates NBD verification code is 998877. Do not share.", "expected": {"decision": "Ignored", "category": "OTP"}}
]


LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/fab"
cat > "app/src/test/resources/financial-corpus/uae/fab/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Credit Card Purchase Card No XXXX6989 AED 54.69 Amazon Grocery Dubai ARE 15/06/26 20:01 Avl Bal AED 2077.96", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 54.69, "cardTail": "6989"}},
  {"rawMessage": "Credit Card Purchase Card No XXXX6989 AED 12.00 CARS TAXI ABU DHABI ARE 16/05/26 00:36 Avl Bal AED 10532.65", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 12.0, "cardTail": "6989"}},
  {"rawMessage": "Payment of AED 200 received. Outstanding balance AED 3450.00. Thank you for using FAB.", "expected": {"decision": "Confirmation"}},
  {"rawMessage": "Payment received for card ending 1959. Outstanding balance AED 1,200.00.", "expected": {"decision": "Confirmation"}},
  {"rawMessage": "Get 10% cashback on your next FAB card purchase. Avail now. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/mashreq"
cat > "app/src/test/resources/financial-corpus/uae/mashreq/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Mashreq Credit Card ending 1959 was used for a transaction of AED 3.00 at FRESH WAY BAQALA on Sunday, 5 July 2026, 3:51 pm. Available limit: AED 7,869.08", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 3.0, "cardTail": "1959"}},
  {"rawMessage": "Thank you for using your card ending 1959 for AED 30.50 at SEA SHELL CORNICHE on 04-JUL-2026 01:22 AM. Avl.Limit: AED 7,980.08", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 30.5, "cardTail": "1959"}},
  {"rawMessage": "Increase your Mashreq credit limit instantly. You are pre-approved.", "expected": {"decision": "Ignored", "category": "Credit Limit Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/wio"
cat > "app/src/test/resources/financial-corpus/uae/wio/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "AED 250.00 spent at STARBUCKS using card ending 9999.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 250.0, "cardTail": "9999"}},
  {"rawMessage": "Your loan of AED 50,000 has been disbursed and credited to account XXXX1234.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 50000.0}}
]


LEDGEREOF

echo "Phase 4F applied. Run: ./gradlew testDebugUnitTest --tests \"*CorpusRegressionTest\" --tests \"*BenchmarkReportTest\" --tests \"*HeuristicIntentTest\" --tests \"*FinancialLanguageTest\""
