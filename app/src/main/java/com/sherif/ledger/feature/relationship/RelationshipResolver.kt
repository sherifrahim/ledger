package com.sherif.ledger.feature.relationship

import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.feature.merchant.MerchantResolution
import com.sherif.ledger.feature.merchant.MerchantResolver
import kotlin.math.abs
import com.sherif.ledger.core.domain.model.merchantOrRawText

/**
 * The extensible contract. Each resolver owns ONE relationship family, inspects
 * the (chronologically sorted) transaction list, and emits the relationships it
 * recognizes — computing its OWN confidence. Adding a relationship type later
 * means adding a new resolver and registering it; no existing resolver changes.
 *
 * Resolvers are pure: they read transactions and return relationships. They never
 * mutate, persist, or re-identify anything.
 */
interface RelationshipResolver {
    /** Stable id used in diagnostics and relationship-id generation. */
    val key: String

    fun resolve(context: RelationshipContext): List<FinancialRelationship>
}

/**
 * Read-only inputs shared by all resolvers. Wraps the transaction list plus the
 * frozen [MerchantResolver] (used strictly read-only for canonical merchant
 * names — the engine never owns merchant normalization).
 */
class RelationshipContext(
    /** Transactions sorted ascending by timestamp. */
    val transactions: List<Transaction>,
    private val merchantResolver: MerchantResolver,
    val engineVersion: Int,
) {
    /**
     * Canonical merchant name via the frozen resolver, preferred over rawText.
     * Falls back to null when the resolver cannot confidently resolve, so callers
     * can then fall back to rawText reasoning. Never duplicates normalization.
     */
    fun canonicalMerchant(txn: Transaction): String? =
        when (val r = merchantResolver.resolve(txn.merchantOrRawText)) {
            is MerchantResolution.Resolved -> r.canonicalName
            is MerchantResolution.Unresolved -> null
        }

    /** True when both resolve to the same canonical merchant (preferred signal). */
    fun sameCanonicalMerchant(a: Transaction, b: Transaction): Boolean {
        val ca = canonicalMerchant(a) ?: return false
        val cb = canonicalMerchant(b) ?: return false
        return ca.equals(cb, ignoreCase = true)
    }

    /** rawText fallback similarity: shared significant token. Used only when
     *  canonical resolution is unavailable for either side. */
    fun rawTextOverlap(a: Transaction, b: Transaction): Boolean {
        val ta = significantTokens(a.merchantOrRawText)
        val tb = significantTokens(b.merchantOrRawText)
        if (ta.isEmpty() || tb.isEmpty()) return false
        return ta.any { it in tb }
    }

    private fun significantTokens(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.uppercase()
            .split(Regex("[^A-Z0-9]+"))
            .filter { it.length >= 4 && it.toIntOrNull() == null }
            .toSet()
    }
}

/** Shared numeric helpers for resolvers. */
internal object RelationshipMath {
    fun secondsBetween(a: Transaction, b: Transaction): Long =
        abs(a.timestamp.epochSecond - b.timestamp.epochSecond)

    fun amountDiffMinor(a: Transaction, b: Transaction): Long =
        abs(a.amount.minorUnits - b.amount.minorUnits)

    fun sameAmount(a: Transaction, b: Transaction): Boolean =
        a.amount.minorUnits == b.amount.minorUnits &&
            a.amount.currencyCode == b.amount.currencyCode

    fun sameCard(a: Transaction, b: Transaction): Boolean =
        !a.cardTail.isNullOrBlank() && a.cardTail == b.cardTail

    fun sameAccount(a: Transaction, b: Transaction): Boolean =
        a.accountId == b.accountId

    fun daysBetween(a: Transaction, b: Transaction): Long =
        abs(a.timestamp.epochSecond - b.timestamp.epochSecond) / 86_400L
}

