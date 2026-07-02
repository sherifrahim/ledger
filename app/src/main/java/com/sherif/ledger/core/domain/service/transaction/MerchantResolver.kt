package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Brand
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.MerchantRepository
import javax.inject.Inject

/**
 * Service responsible for resolving or creating merchants based on raw input text.
 */
class MerchantResolver @Inject constructor(
    private val merchantRepository: MerchantRepository
) {

    suspend fun resolve(rawMerchantText: String): Long? {
        val existingBrand = merchantRepository.getBrandByAlias(rawMerchantText)
        if (existingBrand is LedgerResult.Success) {
            return existingBrand.data.id
        }

        // Create new brand if not found (Initial deterministic implementation)
        val newBrand = Brand(
            id = 0,
            name = rawMerchantText,
            brandKey = "manual",
            defaultCategoryId = null
        )
        
        val brandIdResult = merchantRepository.insertBrand(newBrand)
        return if (brandIdResult is LedgerResult.Success) {
            val brandId = brandIdResult.data
            merchantRepository.registerAlias(rawMerchantText, brandId)
            brandId
        } else {
            null
        }
    }
}
