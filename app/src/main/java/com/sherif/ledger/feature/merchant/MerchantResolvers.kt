package com.sherif.ledger.feature.merchant

import javax.inject.Inject

/**
 * Resolves a raw merchant string to its [MerchantCategory]. Thin facade over
 * [MerchantResolver]; returns [MerchantCategory.UNKNOWN] when unresolved. No AI.
 */
class MerchantCategoryResolver @Inject constructor(
    private val resolver: MerchantResolver,
) {
    fun categoryOf(rawMerchant: String?): MerchantCategory =
        when (val r = resolver.resolve(rawMerchant)) {
            is MerchantResolution.Resolved -> r.category
            is MerchantResolution.Unresolved -> MerchantCategory.UNKNOWN
        }
}

/** Brand metadata for a resolved merchant, prepared for future UI (colors, logos). */
data class MerchantBrand(
    val canonicalName: String,
    val brandColor: String?,
    val logoAsset: String?,
    val website: String?,
)

/**
 * Resolves a raw merchant string to its brand accent color / logo / website.
 * Facade over [MerchantResolver]; returns null when unresolved. The assets
 * themselves are not loaded this phase — only the metadata is exposed.
 */
class MerchantBrandResolver @Inject constructor(
    private val resolver: MerchantResolver,
) {
    fun brandOf(rawMerchant: String?): MerchantBrand? =
        when (val r = resolver.resolve(rawMerchant)) {
            is MerchantResolution.Resolved -> MerchantBrand(
                canonicalName = r.profile.canonicalName,
                brandColor = r.profile.brandColor,
                logoAsset = r.profile.logoAsset,
                website = r.profile.website,
            )
            is MerchantResolution.Unresolved -> null
        }
}

/**
 * Developer-console record of a resolution attempt. Diagnostics only; nothing
 * downstream depends on it.
 *
 *     Raw Merchant -> Normalized Merchant -> Matched Alias -> Confidence
 */
data class MerchantDiagnostics(
    val rawMerchant: String,
    val normalizedMerchant: String,
    val matchedAlias: String?,
    val canonicalName: String?,
    val category: String?,
    val confidence: Int,
    val resolved: Boolean,
) {
    companion object {
        fun from(resolution: MerchantResolution): MerchantDiagnostics = when (resolution) {
            is MerchantResolution.Resolved -> MerchantDiagnostics(
                rawMerchant = resolution.rawMerchant,
                normalizedMerchant = resolution.canonicalName,
                matchedAlias = resolution.matchedAlias,
                canonicalName = resolution.canonicalName,
                category = resolution.category.name,
                confidence = resolution.confidence,
                resolved = true,
            )
            is MerchantResolution.Unresolved -> MerchantDiagnostics(
                rawMerchant = resolution.rawMerchant,
                normalizedMerchant = resolution.fallbackDisplay,
                matchedAlias = null,
                canonicalName = null,
                category = MerchantCategory.UNKNOWN.name,
                confidence = 0,
                resolved = false,
            )
        }
    }
}

