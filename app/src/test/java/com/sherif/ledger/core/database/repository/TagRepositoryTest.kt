package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.TagDao
import com.sherif.ledger.core.database.entity.TagEntity
import com.sherif.ledger.core.database.entity.TransactionTagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagRepositoryTest {

    /**
     * Behaves like the Room DAO it stands in for, in the two ways that decide
     * whether this repository is correct:
     *
     *  - `insert` is `OnConflict.IGNORE`, so inserting a name that already exists
     *    returns **-1** rather than throwing or replacing. A fake that always
     *    returned a fresh id would hide the entire second-use path.
     *  - `attach` is also IGNORE against a composite primary key, so attaching the
     *    same pair twice is silently a no-op rather than a duplicate row.
     */
    private class FakeTagDao : TagDao {
        private val tags = MutableStateFlow<List<TagEntity>>(emptyList())
        private val edges = MutableStateFlow<List<TransactionTagEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<TagEntity>> = tags.map { it.sortedBy { t -> t.name.lowercase() } }
        override suspend fun findByNormalizedName(normalizedName: String) =
            tags.value.firstOrNull { it.normalizedName == normalizedName }
        override suspend fun findById(id: Long) = tags.value.firstOrNull { it.id == id }

        override suspend fun insert(tag: TagEntity): Long {
            if (tags.value.any { it.normalizedName == tag.normalizedName }) return -1L
            val id = nextId++
            tags.value = tags.value + tag.copy(id = id)
            return id
        }

        override suspend fun deleteTag(id: Long) {
            tags.value = tags.value.filterNot { it.id == id }
            edges.value = edges.value.filterNot { it.tagId == id } // schema cascade
        }

        override suspend fun attach(edge: TransactionTagEntity) {
            if (edges.value.any { it.transactionId == edge.transactionId && it.tagId == edge.tagId }) return
            edges.value = edges.value + edge
        }

        override suspend fun detach(transactionId: Long, tagId: Long) {
            edges.value = edges.value.filterNot { it.transactionId == transactionId && it.tagId == tagId }
        }

        override fun observeTagsFor(transactionId: Long): Flow<List<TagEntity>> =
            edges.map { list ->
                list.filter { it.transactionId == transactionId }
                    .mapNotNull { e -> tags.value.firstOrNull { it.id == e.tagId } }
                    .sortedBy { it.name.lowercase() }
            }

        override suspend fun transactionIdsFor(tagId: Long) =
            edges.value.filter { it.tagId == tagId }.map { it.transactionId }

        override fun observeAllEdges(): Flow<List<TransactionTagEntity>> = edges

        override suspend fun usageCount(tagId: Long) = edges.value.count { it.tagId == tagId }
    }

    private fun repo() = RoomTagRepository(FakeTagDao())

    @Test
    fun `tagging the same name twice reuses one tag rather than minting a second`() = runBlocking {
        val r = repo()
        val first = r.tagTransaction(1L, "Dubai Trip")
        val second = r.tagTransaction(2L, "Dubai Trip")

        assertNotNull(first)
        assertEquals(first!!.id, second!!.id)
        assertEquals(1, r.observeAllTags().first().size)
        assertEquals(listOf(1L, 2L), r.transactionIdsFor(first.id))
    }

    @Test
    fun `case and spacing do not create look-alike tags`() = runBlocking {
        val r = repo()
        val a = r.tagTransaction(1L, "Dubai Trip")
        val b = r.tagTransaction(2L, "  dubai   trip ")

        assertEquals(a!!.id, b!!.id)
        assertEquals(1, r.observeAllTags().first().size)
        // The capitalisation the user first typed is what survives for display.
        assertEquals("Dubai Trip", r.observeAllTags().first().single().name)
    }

    @Test
    fun `tagging the same transaction twice is idempotent`() = runBlocking {
        val r = repo()
        r.tagTransaction(1L, "Reimbursable")
        r.tagTransaction(1L, "Reimbursable")

        assertEquals(1, r.observeTagsFor(1L).first().size)
    }

    @Test
    fun `removing the last use retires the tag from the vocabulary`() = runBlocking {
        // Otherwise every label ever typed accumulates in the picker forever.
        val r = repo()
        val tag = r.tagTransaction(1L, "Typo")!!
        r.untagTransaction(1L, tag.id)

        assertTrue(r.observeAllTags().first().isEmpty())
    }

    @Test
    fun `a tag still used elsewhere survives being removed from one transaction`() = runBlocking {
        val r = repo()
        val tag = r.tagTransaction(1L, "Groceries run")!!
        r.tagTransaction(2L, "Groceries run")

        r.untagTransaction(1L, tag.id)

        assertEquals(1, r.observeAllTags().first().size)
        assertEquals(listOf(2L), r.transactionIdsFor(tag.id))
    }

    @Test
    fun `an empty name is not a tag`() = runBlocking {
        val r = repo()
        assertNull(r.tagTransaction(1L, "   "))
        assertTrue(r.observeAllTags().first().isEmpty())
    }

    @Test
    fun `deleting a tag takes its edges with it`() = runBlocking {
        val r = repo()
        val tag = r.tagTransaction(1L, "Trip")!!
        r.tagTransaction(2L, "Trip")

        r.deleteTag(tag.id)

        assertTrue(r.observeTagsFor(1L).first().isEmpty())
        assertTrue(r.observeTagsFor(2L).first().isEmpty())
    }

    @Test
    fun `the whole tag map comes back keyed by transaction`() = runBlocking {
        val r = repo()
        r.tagTransaction(1L, "Trip")
        r.tagTransaction(1L, "Reimbursable")
        r.tagTransaction(2L, "Trip")

        val map = r.observeTagsByTransaction().first()

        assertEquals(listOf("Reimbursable", "Trip"), map[1L]?.map { it.name })
        assertEquals(listOf("Trip"), map[2L]?.map { it.name })
    }

    @Test
    fun `an over-long name is truncated rather than rejected`() = runBlocking {
        val r = repo()
        val tag = r.tagTransaction(1L, "x".repeat(100))
        assertEquals(com.sherif.ledger.core.domain.model.Tag.MAX_LENGTH, tag!!.name.length)
    }
}
