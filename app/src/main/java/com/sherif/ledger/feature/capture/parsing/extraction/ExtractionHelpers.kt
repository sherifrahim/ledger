package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.TransferDirection
import java.util.regex.Pattern

/**
 * Reusable regex-based extraction units to avoid monolithic patterns.
 */
object ExtractionHelpers {
    // Currency tokens the amount may be anchored to. Deliberately broad: a bank
    // message about a FOREIGN purchase states the amount in that currency while
    // still quoting an AED balance, so a narrow list (AED|USD|INR only) caused the
    // transaction amount to be skipped and a later AED figure — the running balance
    // — to be captured instead. Real-world failure: a KZT purchase recorded as the
    // card's "Available limit: AED 8,225.16". Curated ISO codes rather than a bare
    // [A-Z]{3} so ordinary words are never mistaken for a currency.
    private const val CURRENCY_TOKENS =
        "AED|USD|EUR|GBP|INR|SAR|KWD|QAR|OMR|BHD|JOD|EGP|KZT|UZS|TRY|PKR|BDT|LKR|NPR|" +
            "PHP|THB|MYR|SGD|HKD|CNY|JPY|KRW|AUD|NZD|CAD|CHF|SEK|NOK|DKK|ZAR|RUB|IDR|VND|" +
            "MAD|TND|LBP|IQD|Rs\\.?|DIRHAM"

    // Balance/limit clauses. These state what REMAINS, never what was transacted, so
    // any amount inside one must never be read as the transaction amount. Stripped
    // before amount extraction. Covers "Available limit: AED 8,093.63",
    // "Available balance is AED1568.52", "Avl. bal. AED 1493.52", "Bal: AED 10.00".
    private val BALANCE_CLAUSE_PATTERN = Pattern.compile(
        "(?:avail(?:able)?\\s*(?:balance|limit|credit)|avl\\.?\\s*bal\\.?|" +
            "remaining\\s*(?:balance|limit)|bal(?:ance)?)\\s*(?:is|of|:|=)?\\s*" +
            "(?:$CURRENCY_TOKENS)?\\s*\\d[\\d,]*(?:\\.\\d{1,2})?",
        Pattern.CASE_INSENSITIVE,
    )

    // Currency-anchored: the amount must follow a currency token. Prevents card/account
    // numbers (which often precede the amount) from being mis-read as the amount.
    private val AMOUNT_ANCHORED_PATTERN = Pattern.compile("(?:$CURRENCY_TOKENS)\\s*(\\d[\\d,]*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    // Fallback for texts with no currency token: first number-like token.
    private val AMOUNT_FALLBACK_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?|\\d+\\.\\d{1,2}|\\d+)")
    private val CARD_PATTERN = Pattern.compile("(?:card no\\.?\\s*[Xx*]*|[Xx*]{2,4}|(?:card |account |a\\/c )?ending(?: in)? |account(?: no)?\\.?\\s*[Xx*]*|a\\/c\\s*[Xx*]*)(\\d{4,})", Pattern.CASE_INSENSITIVE)

    /**
     * The text an amount may legitimately be read from: the message with every
     * balance/limit clause removed. A running balance is never the transaction
     * amount, so removing those clauses first makes "first currency-anchored
     * amount" the transacted amount even when the message quotes a balance in a
     * different (usually home) currency afterwards.
     */
    private fun amountSearchText(text: String): String {
        // Card/account references are digits that are NEVER an amount. Removing them
        // stops the un-anchored fallback from reading a card tail as money (e.g.
        // "card ending 1959 ..." yielding an "AED 1,959.00" charge).
        val withoutCards = CARD_PATTERN.matcher(text).replaceAll(" ")
        return BALANCE_CLAUSE_PATTERN.matcher(withoutCards).replaceAll(" ")
    }

    /** The currency token that anchors the transacted amount, e.g. "AED", "KZT". */
    private fun anchoringCurrencyToken(text: String): String? {
        val m = AMOUNT_ANCHORED_PATTERN.matcher(amountSearchText(text))
        if (!m.find()) return null
        // group(0) is "<TOKEN><spaces><digits>"; strip the numeric tail.
        return m.group(0)?.dropLastWhile { it.isDigit() || it == ',' || it == '.' || it == ' ' }?.trim()
    }

    private fun tokenToCurrency(token: String?): CurrencyCode? {
        val t = token?.uppercase()?.trimEnd('.') ?: return null
        return when (t) {
            "AED", "DIRHAM" -> CurrencyCode.AED
            "INR", "RS" -> CurrencyCode.INR
            "USD" -> CurrencyCode.USD
            "EUR" -> CurrencyCode.EUR
            "GBP" -> CurrencyCode.GBP
            "SAR" -> CurrencyCode.SAR
            "KZT" -> CurrencyCode.KZT
            else -> null // a real currency we cannot represent yet — fail closed
        }
    }

    fun extractAmountMinor(text: String): Long? {
        val searchable = amountSearchText(text)
        val anchored = AMOUNT_ANCHORED_PATTERN.matcher(searchable)
        val matchedValue = if (anchored.find()) {
            // Fail CLOSED on a currency we cannot represent. The amount is real, but
            // recording a foreign figure under the account's own currency would state
            // a false number (e.g. a KZT 7,500 purchase becoming "AED 7,500"). Better
            // to capture nothing than to capture something wrong.
            if (tokenToCurrency(anchoringCurrencyToken(text)) == null) return null
            anchored.group(1)
        } else {
            val fallback = AMOUNT_FALLBACK_PATTERN.matcher(searchable)
            if (fallback.find()) fallback.group(1) else null
        } ?: return null

        val cleaned = matchedValue.replace(",", "")
        return if (cleaned.contains(".")) {
            val parts = cleaned.split(".")
            val major = parts[0].toLong()
            val minorText = parts[1].padEnd(2, '0').take(2)
            major * 100 + minorText.toLong()
        } else {
            cleaned.toLong() * 100
        }
    }

    /**
     * The currency of the TRANSACTED amount — the token anchoring it, not merely the
     * first currency word anywhere in the message. A foreign purchase quotes the
     * home-currency balance too, and reading that would mislabel the transaction.
     */
    fun extractCurrency(text: String): CurrencyCode? {
        tokenToCurrency(anchoringCurrencyToken(text))?.let { return it }
        // No anchored amount (fallback-amount messages): fall back to a text scan.
        val upper = amountSearchText(text).uppercase()
        return when {
            upper.contains("AED") || upper.contains("DIRHAM") -> CurrencyCode.AED
            upper.contains("INR") || upper.contains("RS") || upper.contains("₹") -> CurrencyCode.INR
            else -> null
        }
    }

    fun extractAccountHint(text: String): String? {
        val matcher = CARD_PATTERN.matcher(text)
        return if (matcher.find()) matcher.group(1) else null
    }

    // A bank also messages you when money did NOT move: a declined card, a failed
    // payment, insufficient funds. No money left the account, so recording one as a
    // transaction invents spending the user never did — found in real captured data,
    // where a repeatedly-declined AED 27.30 payment had been booked eight times.
    private val nonExecutedPhrases = listOf(
        "has been rejected", "was rejected", "is rejected", "rejected",
        "has been declined", "was declined", "is declined", "declined",
        "was unsuccessful", "not successful", "unsuccessful",
        "could not be processed", "cannot be processed",
        "transaction failed", "payment failed", "has failed",
        "insufficient funds", "insufficient balance",
        "do not honour", "do not honor",
    )

    /**
     * True when the message describes a transaction that did NOT execute. Such a
     * message still looks financial (it quotes an amount, a card and a merchant), so
     * it must be excluded explicitly or it is captured as real spending.
     */
    fun describesNonExecutedTransaction(lower: String): Boolean =
        nonExecutedPhrases.any { lower.contains(it) }

    // Incoming-transfer indicators: money ARRIVING at the account in question. Bank-
    // agnostic, deliberately narrow. Used ONLY to determine direction of a transfer
    // that has ALREADY been identified as a transfer by the caller's own type
    // detection (this function does not decide "is this a transfer").
    private val incomingTransferPhrases = listOf(
        "received from", "received via", "credited via", "credited from",
        "credited into your account", "credited into account", "credited to your account",
        "credited to account", "deposited into your account", "deposited to your account",
        "amount received", "funds received", "money received",
        // L7: bank shorthand for a credit-to-account that the phrases above missed
        // (e.g. HDFC "Rs 500 credited to a/c XX1234"). These mirror the established
        // credit indicators already curated as income signals in FinancialPhraseLibrary,
        // so a "credited" transfer is no longer mis-defaulted to OUTGOING.
        "a/c credited", "acct credited", "amount credited", "credited to a/c", "credited to ac",
    )

    // Explicit money-LEAVING indicators. Used only as a guard so a bare
    // "credited"/"deposited" (money arriving) isn't reclassified as incoming when
    // the message is actually describing a send whose counterparty was credited
    // (e.g. "transferred to Ali, credited to payee").
    private val outgoingTransferIndicators = listOf(
        "debited", "transferred to", "sent to", "sent via", "paid to", "paid towards",
        "towards your card", "spent", "withdrawn",
    )

    /**
     * Determines transfer direction from the SAME text already inspected by the
     * caller when it decided the message is a transfer. This is the ONE place
     * direction is inferred from raw text — it exists so the decision is made ONCE,
     * upstream, and recorded as structured data. Callers downstream of extraction
     * (balance calculation, analytics) MUST consume the resulting field and never
     * call this or re-parse text themselves.
     *
     * Defaults to OUTGOING when no incoming indicator is present, matching the
     * dominant real-world pattern this corpus has observed: a bank message
     * describing a transfer is overwhelmingly the account holder sending money out
     * ("transferred to", "sent via UPI", "paid towards your card").
     */
    fun inferTransferDirection(lower: String): TransferDirection =
        when {
            incomingTransferPhrases.any { lower.contains(it) } -> TransferDirection.INCOMING
            // A bare "credited"/"deposited" (money ARRIVING) with no money-leaving
            // verb present is incoming — this catches bank shorthands the phrase
            // list above doesn't enumerate (e.g. "INR 500 credited"), so a
            // "credited" transfer is never mis-defaulted to OUTGOING. Guarded by the
            // absence of any outgoing indicator so genuine sends stay outgoing.
            (lower.contains("credited") || lower.contains("deposited")) &&
                outgoingTransferIndicators.none { lower.contains(it) } -> TransferDirection.INCOMING
            else -> TransferDirection.OUTGOING
        }
}


