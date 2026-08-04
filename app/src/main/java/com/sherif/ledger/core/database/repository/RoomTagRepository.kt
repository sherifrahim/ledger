package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.database.dao.TagDao
import com.sherif.ledger.core.database.entity.TagEntity
import com.sherif.ledger.core.database.entity.TransactionTagEntity
import com.sherif.ledger.core.domain.model.Tag
import com.sherif.ledger.core.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomTagRepository @Inject constructor(
    private val tagDao: TagDao,
) : TagRepository {

    override fun observeAllTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeTagsFor(transactionId: Long): Flow<List<Tag>> =
        tagDao.observeTagsFor(transactionId).map { entities -> entities.map { it.toDomain() } }

    override fun observeTagsByTransaction(): Flow<Map<Long, List<Tag>>> =
        combine(tagDao.observeAll(), tagDao.observeAllEdges()) { tags, edges ->
            val byId = tags.associateBy { it.id }
            edges.groupBy { it.transactionId }
                .mapValues { (_, group) ->
                    group.mapNotNull { byId[it.tagId]?.toDomain() }.sortedBy { it.name.lowercase() }
                }
        }

    /**
     * Creating and attaching are one action.
     *
     * The insert uses IGNORE and can therefore return -1 when the tag already
     * exists, which is the normal case the second time a name is used — so the
     * existing row is looked up rather than treated as a failure. Doing it in this
     * order (insert, then look up on conflict) also means two concurrent taggings
     * of the same new name converge on one row instead of racing.
     */
    override suspend fun tagTransaction(transactionId: Long, rawName: String): Tag? {
        val display = Tag.sanitize(rawName) ?: return null
        val normalized = Tag.normalize(display)

        val insertedId = tagDao.insert(
            TagEntity(name = display, normalizedName = normalized),
        )
        val tag = if (insertedId > 0) {
            TagEntity(id = insertedId, name = display, normalizedName = normalized)
        } else {
            tagDao.findByNormalizedName(normalized) ?: run {
                LedgerLogger.e("TagRepository: '$display' neither inserted nor found — not tagging")
                return null
            }
        }

        tagDao.attach(TransactionTagEntity(transactionId = transactionId, tagId = tag.id))
        return tag.toDomain()
    }

    override suspend fun untagTransaction(transactionId: Long, tagId: Long) {
        tagDao.detach(transactionId, tagId)
        // A tag nobody uses is clutter in every picker from here on. Removing the
        // last edge removes the tag itself, so the vocabulary stays the set of
        // labels actually in use rather than everything ever typed.
        if (tagDao.usageCount(tagId) == 0) tagDao.deleteTag(tagId)
    }

    override suspend fun deleteTag(tagId: Long) = tagDao.deleteTag(tagId)

    override suspend fun transactionIdsFor(tagId: Long): List<Long> = tagDao.transactionIdsFor(tagId)

    private fun TagEntity.toDomain() = Tag(id = id, name = name)
}
