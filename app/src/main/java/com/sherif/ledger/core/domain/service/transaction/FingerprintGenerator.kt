package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Service responsible for generating unique transaction fingerprints for deduplication.
 *
 * The fingerprint identifies a real-world money movement, NOT a database row, so it
 * deliberately excludes `accountId`. Two consequences, both intended:
 *
 *  1. **Dedup precedes account attribution.**
 *     [com.sherif.ledger.feature.capture.reconciliation.ReconciliationEngine] runs
 *     before [com.sherif.ledger.core.domain.service.account.AccountIdentityResolver],
 *     so at reconciliation time there is no account yet — it fingerprinted the
 *     candidate with `accountId = 0` while every persisted row was fingerprinted
 *     with its real account id. The two could therefore never be equal, which made
 *     the engine's exact-match branch dead code that silently never fired. Removing
 *     the field is what makes that branch actually work.
 *  2. **The unique index becomes a real cross-account guard.** The same purchase
 *     arriving through two channels (the bank's own app, plus Google Messages or
 *     Truecaller mirroring the same SMS) resolves to two different accounts, so an
 *     account-scoped fingerprint let both rows in — one purchase counted twice, on
 *     accounts that could then never reconcile against each other. Verified against
 *     the owner's real database, not hypothesised.
 *
 * This is a strict tightening: within a single account the collision set is exactly
 * what it always was. Only the cross-account case changes, which is the bug.
 *
 * Merchant text is still part of the key, so this catches the same message arriving
 * twice verbatim. It does NOT catch the same event described differently by two
 * channels — that is deliberately [ReconciliationEngine]'s job, which matches
 * structurally on amount, time and card tail instead of on wording.
 */
class FingerprintGenerator @Inject constructor() {

    fun generate(params: InsertTransactionUseCase.Params): String {
        val bucket = params.timestamp.toEpochMilli() / (3600 * 1000) // 1-hour bucket
        val raw = "${params.amountMinor}|${params.currencyCode}|${bucket}|${params.rawMerchantText}"
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
