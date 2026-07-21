package com.sherif.ledger.feature.ai.cache

import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.domain.AIContext
import com.sherif.ledger.feature.ai.domain.AISuggestion
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private data class CacheEntry(val suggestion: AISuggestion, val cachedAtMillis: Long)

/**
 * RC6's "Caching" — avoids repeated AI requests for the same
 * (capability, context) pair, e.g. the same raw merchant string appearing on
 * multiple transactions in one import run. In-memory only (process lifetime),
 * not persisted — a cache is an optimization, not a source of truth, and
 * doesn't need to survive a restart. Key is the capability plus the
 * `AIContext` data class's own `toString()` (stable across identical field
 * values) — deliberately not a real merchant/name hash, since this is meant
 * to be simple and swappable, not cryptographically anything.
 */
@Singleton
class AiSuggestionCache @Inject constructor() {
    private val entries = ConcurrentHashMap<String, CacheEntry>()
    private val hits = AtomicInteger(0)
    private val misses = AtomicInteger(0)

    /** Configurable per call, per RC6's "cache invalidation should be configurable" — no single hardcoded TTL. */
    fun get(capability: AICapability, context: AIContext, ttlMillis: Long): AISuggestion? {
        val entry = entries[keyOf(capability, context)]
        if (entry == null || System.currentTimeMillis() - entry.cachedAtMillis > ttlMillis) {
            entries.remove(keyOf(capability, context))
            misses.incrementAndGet()
            return null
        }
        hits.incrementAndGet()
        return entry.suggestion
    }

    fun put(capability: AICapability, context: AIContext, suggestion: AISuggestion) {
        entries[keyOf(capability, context)] = CacheEntry(suggestion, System.currentTimeMillis())
    }

    fun clear() = entries.clear()

    fun size(): Int = entries.size

    /** Null when nothing has ever been looked up yet — never a fabricated 0%. */
    fun hitRatePercent(): Int? {
        val total = hits.get() + misses.get()
        return if (total == 0) null else (hits.get() * 100) / total
    }

    private fun keyOf(capability: AICapability, context: AIContext) = "${capability.name}:${context.hashCode()}"
}
