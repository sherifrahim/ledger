package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.feature.merchant.MerchantResolver

/**
 * The clean, human-readable merchant name to show for a transaction row.
 *
 * A [Transaction] has no merchant-name field of its own — it carries the raw
 * captured text ([Transaction.rawText], often a whole bank SMS) and a [brandId]
 * assigned at capture time. Screens historically showed `rawText` directly, so a
 * real capture surfaced the entire SMS as the row title. This is the single place
 * that turns a transaction into a display name, so every screen agrees:
 *
 *  1. **Brand assigned at capture** — `brandId → Brand.name`. This is what the
 *     capture pipeline already resolved (e.g. "Carrefour"), the authoritative name.
 *  2. **Merchant registry fallback** — deterministic [MerchantResolver] over the
 *     raw text: a canonical name when an alias matches, else a title-cased fallback.
 *     Nothing is invented; the raw text is never mutated.
 *  3. **"Unknown"** only when there is genuinely nothing to show.
 */
object TransactionDisplayName {

    fun resolve(
        transaction: Transaction,
        brandNamesById: Map<Long, String>,
        merchantResolver: MerchantResolver,
    ): String {
        transaction.brandId?.let { id ->
            brandNamesById[id]?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return merchantResolver.resolve(transaction.rawText).displayName
            .takeIf { it.isNotBlank() }
            ?: "Unknown"
    }
}
