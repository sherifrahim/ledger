package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Brand
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.MerchantRepository
import javax.inject.Inject

/**
 * Resolves or creates the merchant/brand stamped onto a transaction **at capture
 * time** — the "System B" write-path resolver wired into [com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase].
 *
 * Renamed from `MerchantResolver` (ADR-0009 step 1) so it is no longer
 * name-ambiguous with the presentation-side `feature/merchant/MerchantResolver`
 * ("System A"). This is a pure rename — behaviour is unchanged and proven by the
 * corpus regression suite. Deliberately frozen: changes to what brand/category a
 * captured transaction receives need a demonstrated case + corpus fixture
 * (see MERCHANT_ARCHITECTURE.md / ADR-0009).
 */
class CaptureMerchantResolver @Inject constructor(
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
