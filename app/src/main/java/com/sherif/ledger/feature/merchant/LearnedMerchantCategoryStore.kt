package com.sherif.ledger.feature.merchant

import com.sherif.ledger.core.database.dao.MerchantCategoryOverrideDao
import com.sherif.ledger.core.database.entity.MerchantCategoryOverrideEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last fallback in category resolution, after the deterministic brand
 * registry ([MerchantResolver]) and generic keyword classifier
 * ([GenericCategoryKeywords]) both fail: a category the user taught Ledger
 * themselves, from the Review Inbox. Keyed by the SAME normalized form
 * [MerchantResolver] already uses (uppercase, whitespace-collapsed), so a
 * taught category is looked up regardless of how the raw text was cased.
 *
 * [GetFinancialAnalyticsUseCase.transactionStories]/[GetFinancialAnalyticsUseCase.compute]
 * are plain (non-suspend) functions, called from hot UI paths — this store
 * keeps an in-memory cache kept in sync with the DB rather than requiring
 * every category lookup to become suspend. A cache miss briefly after a
 * fresh install (before the initial load completes) just means "no override
 * yet," which is also the correct answer at that point — nothing invented.
 */
@Singleton
class LearnedMerchantCategoryStore @Inject constructor(
    private val dao: MerchantCategoryOverrideDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cache: Map<String, MerchantCategory> = emptyMap()

    init {
        scope.launch { reload() }
    }

    private suspend fun reload() {
        cache = dao.getAll().mapNotNull { entity ->
            runCatching { MerchantCategory.valueOf(entity.category) }.getOrNull()
                ?.let { entity.merchantKey to it }
        }.toMap()
    }

    /** Synchronous — see class doc for why. Null when nothing has been taught for this merchant yet. */
    fun categoryFor(rawMerchantText: String?): MerchantCategory? {
        if (rawMerchantText.isNullOrBlank()) return null
        return cache[normalize(rawMerchantText)]
    }

    suspend fun learn(rawMerchantText: String, category: MerchantCategory) {
        val key = normalize(rawMerchantText)
        dao.upsert(MerchantCategoryOverrideEntity(key, category.name, System.currentTimeMillis()))
        cache = cache + (key to category)
    }

    private fun normalize(rawMerchantText: String): String =
        rawMerchantText.uppercase().replace(Regex("\\s+"), " ").trim()
}
