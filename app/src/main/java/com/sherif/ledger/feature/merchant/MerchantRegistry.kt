package com.sherif.ledger.feature.merchant

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The deterministic merchant knowledge base. A plain injectable holder of
 * [MerchantProfile]s for the UAE and India target markets. Extend [profiles] to
 * teach Ledger new merchants; no resolver code changes.
 *
 * Aliases are ordered from most specific to least within each profile; the
 * resolver checks exact-match aliases before substring aliases to avoid short
 * tokens over-matching.
 */
@Singleton
class MerchantRegistry @Inject constructor() {

    val profiles: List<MerchantProfile> = listOf(
        MerchantProfile(
            canonicalName = "Amazon",
            aliases = listOf(
                MerchantAlias("AMAZON"), MerchantAlias("AMZN"),
                MerchantAlias("AMAZON.AE"), MerchantAlias("AMAZON MARKETPLACE"),
                MerchantAlias("AMAZON PRIME"), MerchantAlias("AMAZON GROCERY"),
            ),
            category = MerchantCategory.SHOPPING,
            brandColor = "#FF9900",
            website = "https://www.amazon.ae",
            country = "AE",
            knownConfidence = 98,
        ),
        MerchantProfile(
            canonicalName = "Careem",
            aliases = listOf(
                MerchantAlias("CAREEM PAY"), MerchantAlias("CAREEM*"),
                MerchantAlias("CAREEM"),
            ),
            category = MerchantCategory.TRANSPORT,
            brandColor = "#4BB543",
            website = "https://www.careem.com",
            country = "AE",
            knownConfidence = 97,
            subcategory = "Ride-hailing",
        ),
        MerchantProfile(
            canonicalName = "Uber",
            aliases = listOf(MerchantAlias("UBER*"), MerchantAlias("UBER")),
            category = MerchantCategory.TRANSPORT,
            brandColor = "#000000",
            website = "https://www.uber.com",
            knownConfidence = 96,
            subcategory = "Ride-hailing",
        ),
        MerchantProfile(
            canonicalName = "Carrefour",
            aliases = listOf(MerchantAlias("CARREFOUR"), MerchantAlias("MAF CARREFOUR")),
            category = MerchantCategory.GROCERIES,
            brandColor = "#004E9F",
            country = "AE",
            knownConfidence = 97,
        ),
        MerchantProfile(
            canonicalName = "Lulu Hypermarket",
            aliases = listOf(MerchantAlias("LULU HYPERMARKET"), MerchantAlias("LULU")),
            category = MerchantCategory.GROCERIES,
            country = "AE",
            knownConfidence = 95,
        ),
        MerchantProfile(
            canonicalName = "Noon",
            aliases = listOf(
                MerchantAlias("NOON MINUTES"), MerchantAlias("NOON FOOD"),
                MerchantAlias("NOON.COM"), MerchantAlias("NOON"),
            ),
            category = MerchantCategory.SHOPPING,
            brandColor = "#FEEE00",
            website = "https://www.noon.com",
            country = "AE",
            knownConfidence = 95,
        ),
        MerchantProfile(
            canonicalName = "Talabat",
            aliases = listOf(MerchantAlias("TALABAT")),
            category = MerchantCategory.FOOD_DELIVERY,
            brandColor = "#FF5A00",
            country = "AE",
            knownConfidence = 96,
        ),
        MerchantProfile(
            canonicalName = "Zomato",
            aliases = listOf(MerchantAlias("ZOMATO")),
            category = MerchantCategory.FOOD_DELIVERY,
            brandColor = "#E23744",
            country = "IN",
            knownConfidence = 96,
        ),
        MerchantProfile(
            canonicalName = "Swiggy",
            aliases = listOf(MerchantAlias("SWIGGY")),
            category = MerchantCategory.FOOD_DELIVERY,
            brandColor = "#FC8019",
            country = "IN",
            knownConfidence = 96,
        ),
        MerchantProfile(
            canonicalName = "Costa Coffee",
            aliases = listOf(MerchantAlias("COSTA COFFEE"), MerchantAlias("COSTA")),
            category = MerchantCategory.DINING,
            brandColor = "#6E1E32",
            knownConfidence = 94,
        ),
        MerchantProfile(
            canonicalName = "Starbucks",
            aliases = listOf(MerchantAlias("STARBUCKS")),
            category = MerchantCategory.DINING,
            brandColor = "#00704A",
            knownConfidence = 95,
        ),
        MerchantProfile(
            canonicalName = "DEWA",
            aliases = listOf(MerchantAlias("DEWA", exact = true)),
            category = MerchantCategory.UTILITIES,
            country = "AE",
            knownConfidence = 97,
        ),
        MerchantProfile(
            canonicalName = "Etisalat",
            aliases = listOf(MerchantAlias("ETISALAT"), MerchantAlias("E& ")),
            category = MerchantCategory.TELECOM,
            brandColor = "#E30613",
            country = "AE",
            knownConfidence = 95,
        ),
        MerchantProfile(
            canonicalName = "Du",
            aliases = listOf(MerchantAlias("DU", exact = true)),
            category = MerchantCategory.TELECOM,
            country = "AE",
            knownConfidence = 90,
        ),
        MerchantProfile(
            canonicalName = "Netflix",
            aliases = listOf(MerchantAlias("NETFLIX")),
            category = MerchantCategory.ENTERTAINMENT,
            brandColor = "#E50914",
            knownConfidence = 97,
            subcategory = "Streaming subscription",
        ),
        MerchantProfile(
            canonicalName = "Shein",
            aliases = listOf(MerchantAlias("SHEIN")),
            category = MerchantCategory.SHOPPING,
            country = "AE",
            knownConfidence = 94,
        ),
        MerchantProfile(
            canonicalName = "Spinneys",
            aliases = listOf(MerchantAlias("SPINNEYS")),
            category = MerchantCategory.GROCERIES,
            country = "AE",
            knownConfidence = 95,
        ),
        MerchantProfile(
            canonicalName = "Big Bazaar",
            aliases = listOf(MerchantAlias("BIG BAZAAR")),
            category = MerchantCategory.GROCERIES,
            country = "IN",
            knownConfidence = 94,
        ),
        MerchantProfile(
            canonicalName = "ADNOC",
            aliases = listOf(MerchantAlias("ADNOC")),
            category = MerchantCategory.FUEL,
            country = "AE",
            knownConfidence = 96,
        ),
    )
}

