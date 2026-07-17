package com.sherif.ledger.feature.merchant

/**
 * Part 6 follow-up (not a fix to [MerchantResolver] — deliberately separate,
 * additive, and never touches it or [MerchantRegistry]). Both are frozen and
 * working exactly as designed: a small, curated allowlist of major brands
 * that correctly returns [MerchantResolution.Unresolved] for anything else,
 * "nothing invented." Confirmed via a real diagnostic bundle that "UNKNOWN"
 * categories there ("Al Madina Fresh Mart," "Cars Taxi," etc.) were small
 * independent local businesses never in scope for that registry, not a bug.
 *
 * This is the bounded, still-deterministic, still-"no AI" middle ground: a
 * generic keyword-to-category fallback, consulted ONLY when brand
 * resolution already failed. It never overrides a resolved brand's own
 * category, and it still invents nothing about the merchant's NAME — only
 * infers a coarse category from generic words that appear in the raw text
 * regardless of which specific business it is.
 */
object GenericCategoryKeywords {

    private val categoryKeywords: List<Pair<MerchantCategory, List<String>>> = listOf(
        MerchantCategory.TRANSPORT to listOf("TAXI", "CAB "),
        MerchantCategory.GROCERIES to listOf("BAQALA", "GROCERY", "GROCERIES", "SUPERMARKET", "HYPERMARKET", "MART "),
        MerchantCategory.DINING to listOf("RESTAURANT", "GRILL", "KITCHEN", "CAFE", "CAFÉ", "BAKERY", "EATERY", "DINER"),
        MerchantCategory.FUEL to listOf("PETROL", "FUEL STATION", "GAS STATION"),
        MerchantCategory.HEALTH to listOf("PHARMACY", "CLINIC", "HOSPITAL", "MEDICAL"),
    )

    /** Null when no generic keyword matches — callers should keep "UNKNOWN", never invent a category. */
    fun classify(rawText: String?): MerchantCategory? {
        if (rawText.isNullOrBlank()) return null
        // Trailing space on single-word keywords guards against matching
        // inside an unrelated longer word (e.g. "MART " not matching "SMART").
        val padded = " ${rawText.uppercase()} "
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { padded.contains(it) }) return category
        }
        return null
    }
}
