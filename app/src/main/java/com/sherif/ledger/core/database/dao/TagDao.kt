package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sherif.ledger.core.database.entity.TagEntity
import com.sherif.ledger.core.database.entity.TransactionTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TagEntity?

    /** IGNORE, not REPLACE: re-inserting an existing name must not mint a new id
     *  and orphan every edge already pointing at the old one. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    // ---- edges ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun attach(edge: TransactionTagEntity)

    @Query("DELETE FROM transaction_tags WHERE transaction_id = :transactionId AND tag_id = :tagId")
    suspend fun detach(transactionId: Long, tagId: Long)

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN transaction_tags tt ON tt.tag_id = t.id
        WHERE tt.transaction_id = :transactionId
        ORDER BY t.name COLLATE NOCASE
        """,
    )
    fun observeTagsFor(transactionId: Long): Flow<List<TagEntity>>

    @Query("SELECT transaction_id FROM transaction_tags WHERE tag_id = :tagId")
    suspend fun transactionIdsFor(tagId: Long): List<Long>

    /** Every edge at once, for callers that need the whole tag map in one read
     *  rather than a query per transaction (the search screen, and later the graph). */
    @Query("SELECT * FROM transaction_tags")
    fun observeAllEdges(): Flow<List<TransactionTagEntity>>

    @Query("SELECT COUNT(*) FROM transaction_tags WHERE tag_id = :tagId")
    suspend fun usageCount(tagId: Long): Int
}
