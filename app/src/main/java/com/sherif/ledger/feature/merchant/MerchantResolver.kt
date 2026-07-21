package com.sherif.ledger.feature.merchant

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic raw -> canonical merchant resolution. No AI, no fuzzy ML.
 *
 * Given the RAW merchant string from extraction, it finds the best matching
 * [MerchantProfile] by alias. It never mutates the input: the result carries the
 * original raw text alongside the resolved canonical name, so callers store both
 * separately (raw is never overwritten).
 *
 * Matching rules, applied deterministically:
 *  1. Normalize the raw text to uppercase, collapse whitespace.
 *  2. Exact-match aliases (alias.exact) win first and require full equality —
 *     this protects short ambiguous tokens like "DU" from substring over-match.
 *  3. Substring aliases match if the alias appears as a token/substring. Longer
 *     aliases are tried before shorter ones so "AMAZON PRIME" beats "AMAZON".
 *  4. On no match, returns [MerchantResolution.Unresolved] with the raw text and
 *     a title-cased display fallback. Nothing is invented.
 */
@Singleton
class MerchantResolver @Inject constructor(
    private val registry: MerchantRegistry,
) {

    /** Precomputed (profile, alias, normalizedPattern) triples, longest-first. */
    private data class Entry(val profile: MerchantProfile, val alias: MerchantAlias, val norm: String)

    private val exactEntries: List<Entry>
    private val substringEntries: List<Entry>

    init {
        val all = registry.profiles.flatMap { p ->
            p.aliases.map { a -> Entry(p, a, a.pattern.uppercase().trim()) }
        }
        exactEntries = all.filter { it.alias.exact }
        substringEntries = all.filter { !it.alias.exact }
            .sortedByDescending { it.norm.length }
    }

    fun resolve(rawMerchant: String?): MerchantResolution {
        if (rawMerchant.isNullOrBlank()) {
            return MerchantResolution.Unresolved(rawMerchant ?: "", "", reason = "No merchant text extracted")
        }
        val normalized = rawMerchant.uppercase().replace(Regex("\\s+"), " ").trim()

        // 2. Exact aliases first — matched as a WHOLE TOKEN (word boundary), so a
        // short token like "DU" matches "DU STORE" but never "DUBAI" or "MADU".
        exactEntries.firstOrNull { tokenMatch(it.norm, normalized) }?.let {
            return MerchantResolution.Resolved(
                rawMerchant = rawMerchant,
                profile = it.profile,
                matchedAlias = it.alias.pattern,
                confidence = it.profile.knownConfidence,
                reason = "Exact token match on alias \"${it.alias.pattern}\" for ${it.profile.canonicalName}",
            )
        }

        // 3. Substring aliases, longest-first.
        substringEntries.firstOrNull { normalized.contains(it.norm) }?.let {
            // Slightly lower confidence for substring vs exact-token equality.
            val exactToken = normalized == it.norm
            val confidence = if (exactToken) it.profile.knownConfidence
            else (it.profile.knownConfidence - 3).coerceAtLeast(0)
            val reason = if (exactToken) {
                "Normalized text equals alias \"${it.alias.pattern}\" for ${it.profile.canonicalName}"
            } else {
                "Substring match on alias \"${it.alias.pattern}\" for ${it.profile.canonicalName}"
            }
            return MerchantResolution.Resolved(
                rawMerchant = rawMerchant,
                profile = it.profile,
                matchedAlias = it.alias.pattern,
                confidence = confidence,
                reason = reason,
            )
        }

        // 4. No match: keep raw, provide a display fallback, invent nothing.
        return MerchantResolution.Unresolved(rawMerchant, titleCase(rawMerchant), reason = "No registry alias matched \"$normalized\"")
    }

    /** True if [alias] appears as a whole whitespace-delimited token in [text]. */
    private fun tokenMatch(alias: String, text: String): Boolean =
        Regex("(?:^|\\s)" + Regex.escape(alias) + "(?:$|\\s)").containsMatchIn(text)

    private fun titleCase(s: String): String =
        s.trim().lowercase().split(" ").joinToString(" ") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
}

/**
 * The outcome of resolution. Always carries the original [rawMerchant]; the raw
 * text is never overwritten.
 */
sealed interface MerchantResolution {
    val rawMerchant: String

    /** The canonical display name to show (canonical if resolved, else fallback). */
    val displayName: String

    data class Resolved(
        override val rawMerchant: String,
        val profile: MerchantProfile,
        val matchedAlias: String,
        val confidence: Int,
        /** RC8: human-readable explanation of why this profile matched — for the Intelligence Inspector, never for business logic. */
        val reason: String,
    ) : MerchantResolution {
        override val displayName: String get() = profile.canonicalName
        val canonicalName: String get() = profile.canonicalName
        val category: MerchantCategory get() = profile.category
    }

    data class Unresolved(
        override val rawMerchant: String,
        val fallbackDisplay: String,
        /** RC8: human-readable explanation of why nothing matched — for the Intelligence Inspector, never for business logic. */
        val reason: String = "No registry alias matched",
    ) : MerchantResolution {
        override val displayName: String get() = fallbackDisplay
    }
}

