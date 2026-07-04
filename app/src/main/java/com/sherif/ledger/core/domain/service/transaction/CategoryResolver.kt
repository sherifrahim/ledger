package com.sherif.ledger.core.domain.service.transaction

import javax.inject.Inject

/**
 * Service responsible for deterministic category assignment based on merchant context.
 *
 * NOTE: Returns null until the categories table is seeded. Returning a hardcoded
 * category id for a row that does not exist in the categories table causes a
 * foreign-key constraint violation on insert (transactions.category_id references
 * categories.id). A null reference is exempt from the constraint, so transactions
 * persist correctly and remain uncategorized until real category seeding exists.
 */
class CategoryResolver @Inject constructor() {

    fun resolve(rawMerchantText: String, brandId: Long?): Long? {
        return null
    }
}
