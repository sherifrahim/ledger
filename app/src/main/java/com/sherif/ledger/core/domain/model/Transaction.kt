package com.sherif.ledger.core.domain.model

import java.time.Instant

/**
 * Domain model for a financial transaction.
 */
data class Transaction(
    val id: Long,
    val accountId: Long,
    val brandId: Long?,
    val categoryId: Long?,
    val amount: Money,
    val type: TransactionType,
    val timestamp: Instant,
    val source: IngestionSource,
    /**
     * The captured message, verbatim — the whole bank SMS or notification body.
     * This is evidence, and nothing in the pipeline may overwrite it: extraction
     * reads it, it does not get to replace it with its own output. See
     * [merchantText], and prefer [merchantOrRawText] whenever you want "the
     * merchant", which is almost always.
     */
    val rawText: String?,
    /**
     * The merchant/description extracted out of [rawText], or null for a row
     * written before the two were separated (in which case [rawText] still holds
     * the old merchant-only value). Never read this directly to display or match a
     * merchant — use [merchantOrRawText], which handles both eras.
     */
    val merchantText: String? = null,
    val cardTail: String? = null,
    /**
     * The card's remaining spending headroom as stated by the bank in the message
     * this transaction came from. Evidence, not a derived figure — see
     * [com.sherif.ledger.core.domain.service.transaction.AccountBalanceService],
     * which pairs the most recent one with the account's credit limit to get the
     * outstanding balance.
     */
    val availableCreditMinor: Long? = null,
    val fingerprint: String,
    // See TransactionCandidate.transferDirection: normalized once upstream, never
    // re-derived downstream.
    val transferDirection: TransferDirection? = null,
    // Provenance evidence for AccountIdentityResolver. See TransactionOrigin.
    val origin: TransactionOrigin? = null,
    // User-authored annotation. Single note per transaction, editable in place —
    // never touched by any Financial Truth computation (balance, analytics,
    // relationships). Purely descriptive metadata the user attaches.
    val note: String? = null,
    val noteUpdatedAt: Instant? = null,
)

/**
 * The merchant string for this transaction, whichever era it was written in.
 *
 * Before the two were separated, [rawText] WAS the merchant — extraction wrote its
 * output over its own input, so the database held "Kfc" and "Transfer" where the
 * bank's message had been. Existing rows still look like that and cannot be
 * recovered, so every merchant-shaped read goes through here: new rows answer from
 * [merchantText], old rows from [rawText], and neither the display name, the
 * category, nor any relationship match changes behaviour across the boundary.
 *
 * Use [Transaction.rawText] directly only when you genuinely want the captured
 * message — diagnostics, and full-text search.
 */
val Transaction.merchantOrRawText: String?
    get() = merchantText ?: rawText




