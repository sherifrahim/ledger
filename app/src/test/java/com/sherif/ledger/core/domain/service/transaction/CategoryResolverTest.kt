package com.sherif.ledger.core.domain.service.transaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryResolverTest {

    private val resolver = CategoryResolver()

    @Test
    fun `resolves Amazon to Shopping`() {
        assertEquals(1L, resolver.resolve("AMAZON MARKETPLACE", null))
    }

    @Test
    fun `resolves Carrefour to Groceries`() {
        assertEquals(2L, resolver.resolve("Carrefour City", null))
    }

    @Test
    fun `returns null for unknown merchant`() {
        assertNull(resolver.resolve("Random Store", null))
    }
}
