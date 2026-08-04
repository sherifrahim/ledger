package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Tags, and which transactions carry them.
 *
 * Deliberately small: creating a tag and attaching one are the same gesture from
 * the user's point of view ("tag this as X"), so [tagTransaction] does both and
 * callers never have to decide whether the tag already exists.
 */
interface TagRepository {

    fun observeAllTags(): Flow<List<Tag>>

    fun observeTagsFor(transactionId: Long): Flow<List<Tag>>

    /** Every transaction-to-tag edge, as a map keyed by transaction id. */
    fun observeTagsByTransaction(): Flow<Map<Long, List<Tag>>>

    /**
     * Attaches [rawName] to [transactionId], creating the tag if this is the first
     * time it has been used. Returns the tag, or null if the name was empty.
     */
    suspend fun tagTransaction(transactionId: Long, rawName: String): Tag?

    suspend fun untagTransaction(transactionId: Long, tagId: Long)

    /** Removes the tag entirely, and with it every edge (the schema cascades). */
    suspend fun deleteTag(tagId: Long)

    suspend fun transactionIdsFor(tagId: Long): List<Long>
}
