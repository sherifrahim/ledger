package com.sherif.ledger.feature.capture.notification

import com.sherif.ledger.core.domain.model.IngestionSource

import javax.inject.Inject

/**
 * The reasoned outcome of filtering a notification. Exposes WHY a notification was
 * accepted or rejected so the Developer Console can display it. Observability only;
 * business behavior is unchanged (admission is identical to the boolean form).
 */
sealed interface FilterResult {
    val reason: String

    data class Accepted(override val reason: String) : FilterResult
    data class Rejected(override val reason: String) : FilterResult

    val isAccepted: Boolean get() = this is Accepted
}

/**
 * Filter responsible for identifying potentially financial notifications.
 * It does not perform bank-specific logic, only broad eligibility checks.
 *
 * Design: this filter fails OPEN on financial content. The real gate is the
 * downstream extractor + validator + registry, which decide whether a message is
 * a transaction. This filter's only job is to cheaply discard obvious non-
 * financial noise, NOT to be the authority on which apps count.
 *
 * A message is accepted when ANY of the following holds:
 *  1. It is an SMS (routed via the system provider).
 *  2. Its package is a known financial app (a fast-path allowlist — a hint, not a
 *     hard gate).
 *  3. Its content looks financial: a currency-anchored amount plus transaction
 *     vocabulary, and it is not clearly marketing noise.
 *
 * Content admission uses [FinancialContentHeuristics], a filter-side heuristic
 * that detects financial-looking content WITHOUT performing extraction. The filter
 * is deliberately decoupled from the extractor's amount-parsing semantics.
 *
 * Rationale (Jul 2026 regression): bank app package names vary and drift (e.g.
 * ADCB ships as `com.adcb.nexgen`, not a guessed `com.adcb.mobileapp`). Gating
 * solely on a hand-maintained allowlist silently dropped real transactions before
 * extraction. Content-based admission fixes every bank at once with no per-bank
 * maintenance.
 */
class NotificationFilter @Inject constructor() {

    // Fast-path hint list of known financial app packages. NOT a hard gate: a
    // message from an unknown package still passes when its content is financial.
    private val financialPackages = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay (India)
        "com.google.android.apps.walletnfcrel", // Google Wallet
        "com.google.android.apps.messaging", // Google Messages (SMS bank alerts)
        "com.samsung.android.messaging", // Samsung Messages
        "com.adcb.nexgen", // ADCB
        "com.fab.personalbanking", // First Abu Dhabi Bank
        "com.vipera.ts.starter.MashreqAE", // Mashreq
        "com.emiratesnbd.android", // Emirates NBD
    )

    // Transactional vocabulary marking financial content. Note: "balance" is
    // intentionally excluded — a balance-only notification is not a positive
    // transaction signal. The extractor can still recognize balance messages later
    // if needed.
    private val transactionVocab = listOf(
        "spent", "paid", "payment", "received", "credited", "credit", "debited",
        "debit", "purchase", "transferred", "transfer", "withdrawn", "withdrawal",
        "transaction", "refund", "salary", "emi", "installment",
    )

    // Keywords that mark clearly non-transactional noise even when an amount is
    // present (pure marketing). Used only to reject; never to admit.
    private val marketingOnlyHints = listOf(
        "download now", "install the app", "rate us", "refer a friend",
    )

    /** Boolean convenience preserved for existing callers. */
    fun shouldProcess(envelope: NotificationEnvelope): Boolean = evaluate(envelope).isAccepted

    /**
     * Reasoned evaluation for observability (Developer Console). Admission behavior
     * is identical to [shouldProcess].
     */
    fun evaluate(envelope: NotificationEnvelope): FilterResult {
        if (envelope.text.isBlank() && envelope.title.isBlank()) {
            return FilterResult.Rejected("Empty notification (no title or text)")
        }

        val content = "${envelope.title} ${envelope.text}"
        val lower = content.lowercase()

        if (envelope.source == IngestionSource.SMS) {
            return FilterResult.Accepted("SMS source")
        }
        if (envelope.packageName in financialPackages) {
            // A known financial package LOWERS the bar but is not a blanket admit:
            // bank apps also send non-transactional chrome (login, "welcome back",
            // marketing). Admit on any financial signal (amount OR transaction
            // vocabulary), otherwise reject as non-financial noise.
            val hasAmount = FinancialContentHeuristics.looksLikeFinancialAmount(content)
            val vocab = transactionVocab.firstOrNull { it in lower }
            return when {
                hasAmount || vocab != null ->
                    FilterResult.Accepted("Known financial package with financial signal: ${envelope.packageName}")
                else ->
                    FilterResult.Rejected("Known package but no financial signal: ${envelope.packageName}")
            }
        }
        return when (val r = financialContentReason(content, lower)) {
            null -> FilterResult.Rejected("No financial-looking content")
            else -> FilterResult.Accepted(r)
        }
    }

    /**
     * Returns a reason string when the content looks financial, else null. Uses the
     * filter-side [FinancialContentHeuristics] rather than the extractor, so the
     * filter never performs extraction.
     */
    private fun financialContentReason(content: String, lower: String): String? {
        if (!FinancialContentHeuristics.looksLikeFinancialAmount(content)) return null
        val vocab = transactionVocab.firstOrNull { it in lower } ?: return null
        if (marketingOnlyHints.any { it in lower }) return null
        return "Financial content (amount + \"$vocab\")"
    }
}



