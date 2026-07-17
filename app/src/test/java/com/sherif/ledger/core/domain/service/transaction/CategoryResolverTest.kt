package com.sherif.ledger.core.domain.service.transaction

import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Fixed: the two removed tests (`resolves Amazon to Shopping` / `resolves
 * Carrefour to Groceries`) asserted hardcoded category IDs that predate
 * CategoryResolver's own documented behavior change -- see the class KDoc:
 * resolve() deliberately always returns null until the categories table is
 * seeded, because returning an ID for a row that doesn't exist violates the
 * categories foreign-key constraint on insert. Confirmed via diff against the
 * original Phase 10 base that this was already true before any RC1-RC4 work;
 * the tests were simply never updated to match. Not restored under a
 * different name -- there is nothing to assert about specific merchant-to-
 * category mappings while the table is unseeded.
 */
class CategoryResolverTest {

    private val resolver = CategoryResolver()

    @Test
    fun `returns null for any merchant while categories table is unseeded`() {
        assertNull(resolver.resolve("AMAZON MARKETPLACE", null))
        assertNull(resolver.resolve("Carrefour City", null))
        assertNull(resolver.resolve("Random Store", null))
    }
}

