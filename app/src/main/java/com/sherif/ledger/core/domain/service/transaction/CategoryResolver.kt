package com.sherif.ledger.core.domain.service.transaction

import javax.inject.Inject

/**
 * Service responsible for deterministic category assignment based on merchant context.
 */
class CategoryResolver @Inject constructor() {

    fun resolve(rawMerchantText: String, brandId: Long?): Long? {
        val text = rawMerchantText.uppercase()
        return when {
            "AMAZON" in text -> 1L // Shopping
            "CARREFOUR" in text -> 2L // Groceries
            "COSTA" in text -> 3L // Coffee
            else -> null
        }
    }
}
