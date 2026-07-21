package com.sherif.ledger.core.domain.service.account

import com.sherif.ledger.core.domain.model.CurrencyCode
import javax.inject.Inject

/**
 * A known financial institution — name, its default operating currency, and
 * (RC7) which country it operates in plus the alternate spellings/substrings
 * it can be recognized by. [CurrencyCode] is a first-class dimension of the
 * identity, not bolted on later, so multi-currency/multi-country growth (RC7's
 * explicit goal) never requires touching the shape of this class again.
 */
data class InstitutionIdentity(
    val name: String,
    val defaultCurrency: CurrencyCode,
    val country: String = "AE",
    // Upper-cased substrings recognized within a raw package name / SMS sender
    // ID even when it doesn't exactly equal one of InstitutionRegistry's own
    // keys — e.g. a DLT-registered India SMS header commonly arrives as
    // "VM-HDFCBK-S" or "AD-HDFCBKS", never a fixed single string. Matching by
    // "contains an alias" instead of hand-enumerating every header variant is
    // what RC7 Phase A means by "extensible" and "do not scatter regexes."
    val aliases: Set<String> = emptySet(),
)

/**
 * RC7 Phase A: dedicated lookup from a notification's package name OR an SMS
 * sender ID (see [com.sherif.ledger.feature.capture.notification.NotificationEnvelope.packageName]'s
 * own doc comment — it doubles as the SMS sender field) to the financial
 * institution it belongs to. Deliberately its own component — not embedded
 * inside [AccountIdentityResolver] — so the seed data has one home and can
 * grow independently of resolver logic, the same separation
 * [com.sherif.ledger.feature.merchant.MerchantRegistry] has from
 * [com.sherif.ledger.feature.merchant.MerchantResolver].
 *
 * This is the single source of truth for bank identity in this codebase.
 * Before RC7, three separate hardcoded package-name lists existed for the same
 * four UAE banks and had drifted out of sync with each other (confirmed via
 * grep, not assumed): this registry, [com.sherif.ledger.feature.capture.notification.NotificationFilter]
 * (already correct, matches this one), and two independently-wrong copies —
 * [com.sherif.ledger.feature.capture.parsing.AdcbParser]'s stale
 * `com.adcb.mobileapp` and the debug-only `DebugConsoleViewModel.getPackageForBank`.
 * Both were corrected to read the same values seeded here instead of
 * maintaining their own list (see those files' own comments).
 *
 * Also the concrete root cause of the confirmed HDFC Bank currency-mixing bug
 * (see BalanceCalculator's currency guard, RC6): HDFC was never in this map at
 * all, so every HDFC message resolved to institution=null and fell back to
 * whatever the default (AED) account happened to be. RC7 adds India's five
 * largest retail banks here so that gap closes at the source, and (Phase B)
 * changes what happens for any institution that's STILL unrecognized so it can
 * never again silently land in an unrelated account.
 */
class InstitutionRegistry @Inject constructor() {

    private val byIdentifier: Map<String, InstitutionIdentity> = mapOf(
        // ---- United Arab Emirates ----
        "com.adcb.nexgen" to InstitutionIdentity("ADCB", CurrencyCode.AED, "AE", aliases = setOf("ADCB")),
        "com.fab.personalbanking" to InstitutionIdentity("FAB", CurrencyCode.AED, "AE", aliases = setOf("FAB")),
        "com.vipera.ts.starter.MashreqAE" to InstitutionIdentity("Mashreq", CurrencyCode.AED, "AE", aliases = setOf("MASHREQ")),
        "com.emiratesnbd.android" to InstitutionIdentity("Emirates NBD", CurrencyCode.AED, "AE", aliases = setOf("EMIRATESNBD", "ENBD")),
        // No known Android package seeded yet (SMS-only sender ID recognition for now).
        "RAKBANK" to InstitutionIdentity("RAKBANK", CurrencyCode.AED, "AE", aliases = setOf("RAKBANK", "RAKBK")),

        // ---- India ---- (SMS-sender-ID recognition; these banks have no
        // dedicated Android-app package seeded here since Ledger's live usage
        // to date has only ever observed them arriving as SMS)
        "HDFCBK" to InstitutionIdentity("HDFC Bank", CurrencyCode.INR, "IN", aliases = setOf("HDFCBK", "HDFC")),
        "ICICIB" to InstitutionIdentity("ICICI Bank", CurrencyCode.INR, "IN", aliases = setOf("ICICIB", "ICICI")),
        "SBIINB" to InstitutionIdentity("State Bank of India", CurrencyCode.INR, "IN", aliases = setOf("SBIINB", "SBIPSG", "SBIBNK")),
        "AXISBK" to InstitutionIdentity("Axis Bank", CurrencyCode.INR, "IN", aliases = setOf("AXISBK", "AXISB")),
        "KOTAKB" to InstitutionIdentity("Kotak Mahindra Bank", CurrencyCode.INR, "IN", aliases = setOf("KOTAKB", "KOTAK")),
    )

    /**
     * Null when nothing recognizes the identifier — an unknown institution is
     * a real, visible gap the resolver must treat as weak evidence (RC7 Phase
     * B: routed to a Candidate Account), never guessed around.
     *
     * Resolution order: (1) exact match against a seeded key — covers real
     * Android package names; (2) an alias substring contained within the
     * identifier, case-insensitively — covers SMS sender-ID header variance
     * (carriers/DLT templates prefix/suffix a bank's core code, e.g.
     * "VM-HDFCBK-S" contains "HDFCBK").
     */
    fun resolve(packageName: String?): InstitutionIdentity? {
        if (packageName.isNullOrBlank()) return null
        byIdentifier[packageName]?.let { return it }
        val upper = packageName.uppercase()
        return byIdentifier.values.firstOrNull { identity -> identity.aliases.any { upper.contains(it) } }
    }
}
