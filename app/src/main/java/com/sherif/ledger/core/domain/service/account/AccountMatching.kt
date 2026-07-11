package com.sherif.ledger.core.domain.service.account

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.CurrencyCode

/**
 * The single, shared "does this evidence point at that account" check. Used by
 * [DeterministicAccountIdentityResolver] when binding a new transaction, and by
 * the balance service when it needs to know which liability account a credit-card
 * payment recorded on a DIFFERENT account actually targets. One definition, two
 * callers — not a second, independently-drifting copy of the same logic.
 */
object AccountMatching {

    fun matches(
        institution: InstitutionIdentity?,
        tail: String?,
        currency: CurrencyCode,
        account: Account,
    ): Boolean {
        if (institution == null || tail == null) return false
        val institutionMatch = account.name.contains(institution.name, ignoreCase = true)
        val tailMatch = account.accountNumberTail == tail
        val currencyMatch = account.openingBalance.currencyCode == currency
        return institutionMatch && tailMatch && currencyMatch
    }
}

