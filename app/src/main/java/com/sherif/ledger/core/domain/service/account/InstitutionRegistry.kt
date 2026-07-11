package com.sherif.ledger.core.domain.service.account

import com.sherif.ledger.core.domain.model.CurrencyCode
import javax.inject.Inject

/**
 * A known financial institution, identified by name and its default operating
 * currency. Designed for future multi-currency/multi-institution growth even
 * though only AED institutions are seeded today — [CurrencyCode] is already a
 * first-class dimension of the identity, not bolted on later.
 */
data class InstitutionIdentity(
    val name: String,
    val defaultCurrency: CurrencyCode,
)

/**
 * Dedicated lookup from a notification's package name to the financial
 * institution it belongs to. Deliberately its own component — not embedded inside
 * [com.sherif.ledger.core.domain.service.account.AccountIdentityResolver] — so the
 * seed data has one home and can grow independently of resolver logic, the same
 * separation [com.sherif.ledger.feature.merchant.MerchantRegistry] has from
 * [com.sherif.ledger.feature.merchant.MerchantResolver].
 *
 * Seeded from the same package names already trusted by
 * [com.sherif.ledger.feature.capture.notification.NotificationFilter] — not a
 * second, independently-maintained list.
 */
class InstitutionRegistry @Inject constructor() {

    private val byPackage: Map<String, InstitutionIdentity> = mapOf(
        "com.adcb.nexgen" to InstitutionIdentity("ADCB", CurrencyCode.AED),
        "com.fab.personalbanking" to InstitutionIdentity("FAB", CurrencyCode.AED),
        "com.vipera.ts.starter.MashreqAE" to InstitutionIdentity("Mashreq", CurrencyCode.AED),
        "com.emiratesnbd.android" to InstitutionIdentity("Emirates NBD", CurrencyCode.AED),
    )

    /** Null when the package is unrecognized — an unknown institution is a real,
     *  visible gap the resolver must treat as weak evidence, not guess around. */
    fun resolve(packageName: String?): InstitutionIdentity? =
        packageName?.let { byPackage[it] }
}

