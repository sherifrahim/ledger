package com.sherif.ledger.feature.merchant

/**
 * Merchant Intelligence — a deterministic merchant knowledge layer.
 *
 * NOT AI, NOT parsing. This is a downstream, post-processing layer that maps the
 * RAW merchant string produced by extraction to a canonical merchant profile. It
 * never overwrites the raw merchant: callers keep both the raw text and the
 * resolved canonical name separately.
 *
 * This layer is additive and standalone: nothing in the frozen extraction /
 * persistence pipeline calls it yet. It is ready to be wired in later (when a
 * canonical-merchant column and the use-case seam are unfrozen), and it is
 * benchmarked on its own corpus in the meantime.
 *
 * Note on naming: the existing `MerchantNormalizer` (in the frozen extractor path)
 * is unrelated and untouched. Canonicalization here lives in [MerchantResolver].
 */

/** Coarse spending category for a merchant. Diagnostics / future grouping. */
enum class MerchantCategory {
    SHOPPING,
    GROCERIES,
    TRANSPORT,
    FOOD_DELIVERY,
    DINING,
    UTILITIES,
    TELECOM,
    ENTERTAINMENT,
    TRAVEL,
    HEALTH,
    FINANCE,
    GOVERNMENT,
    FUEL,
    EDUCATION,
    UNKNOWN,
}

/**
 * A single alias for a merchant. [pattern] is matched case-insensitively as an
 * uppercased substring/token against the raw merchant text. [exact] requires the
 * normalized raw to equal the pattern (used for short, ambiguous tokens that
 * would over-match as substrings, e.g. "DU").
 */
data class MerchantAlias(
    val pattern: String,
    val exact: Boolean = false,
)

/**
 * Canonical knowledge about one merchant. Fields for logos, brand colors,
 * websites, and country are present so the registry is ready for those features,
 * but nothing in this phase renders or persists them.
 */
data class MerchantProfile(
    val canonicalName: String,
    val aliases: List<MerchantAlias>,
    val category: MerchantCategory,
    /** Hex brand accent color (e.g. "#FF9900"), prepared for future UI. */
    val brandColor: String? = null,
    /** Prepared for future logo assets; not loaded this phase. */
    val logoAsset: String? = null,
    val website: String? = null,
    /** ISO country hint (e.g. "AE", "IN"), optional. */
    val country: String? = null,
    /** Confidence that a match to this profile is correct, 0..100. */
    val knownConfidence: Int = 95,
)

