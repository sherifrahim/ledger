package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Brand
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.MerchantRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantResolverTest {

    private val merchantRepository = object : MerchantRepository {
        var insertCalled = false
        override suspend fun getAllBrands(): LedgerResult<List<Brand>> = LedgerResult.Success(emptyList())
        override suspend fun getBrandByAlias(rawText: String): LedgerResult<Brand> {
            return if (rawText == "Existing") {
                LedgerResult.Success(Brand(5L, "Existing", "key", null))
            } else {
                LedgerResult.Failure(LedgerError.Unknown(""))
            }
        }
        override suspend fun insertBrand(brand: Brand): LedgerResult<Long> {
            insertCalled = true
            return LedgerResult.Success(6L)
        }
        override suspend fun registerAlias(rawText: String, brandId: Long): LedgerResult<Unit> = LedgerResult.Success(Unit)
    }

    private val resolver = MerchantResolver(merchantRepository)

    @Test
    fun `resolves existing brand`() = runBlocking {
        val brandId = resolver.resolve("Existing")
        assertEquals(5L, brandId)
        assertEquals(false, merchantRepository.insertCalled)
    }

    @Test
    fun `creates brand if not exists`() = runBlocking {
        val brandId = resolver.resolve("New")
        assertEquals(6L, brandId)
        assertEquals(true, merchantRepository.insertCalled)
    }
}
