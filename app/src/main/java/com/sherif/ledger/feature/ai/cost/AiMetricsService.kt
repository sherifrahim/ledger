package com.sherif.ledger.feature.ai.cost

import com.sherif.ledger.core.database.dao.AiAuditLogDao
import com.sherif.ledger.feature.ai.cache.AiSuggestionCache
import javax.inject.Inject
import javax.inject.Singleton

data class AiMetrics(
    val totalRequests: Int,
    val successRatePercent: Int?,
    val failureRatePercent: Int?,
    val averageLatencyMs: Long,
    val averageTokens: Int,
    val cacheHitRatePercent: Int?,
    val capabilityUsage: List<Pair<String, Int>>,
    val providerUsage: List<Pair<String, Int>>,
)

/**
 * RC6's "AI Metrics" — all-time aggregates (distinct from [AiCostTracker],
 * which is scoped to "today" for cost awareness specifically). Displayed in
 * Developer Console, not the user-facing AI Settings screen. Percentages
 * are null (never a fabricated 0%) when there's no data yet to compute them
 * from.
 */
@Singleton
class AiMetricsService @Inject constructor(
    private val dao: AiAuditLogDao,
    private val cache: AiSuggestionCache,
) {
    suspend fun current(): AiMetrics {
        val total = dao.getTotalCount()
        val successes = dao.getSuccessCount()
        val successRate = if (total == 0) null else (successes * 100) / total
        val failureRate = successRate?.let { 100 - it }
        return AiMetrics(
            totalRequests = total,
            successRatePercent = successRate,
            failureRatePercent = failureRate,
            averageLatencyMs = (dao.getAverageLatencyMs() ?: 0.0).toLong(),
            averageTokens = (dao.getAverageTokens() ?: 0.0).toInt(),
            cacheHitRatePercent = cache.hitRatePercent(),
            capabilityUsage = dao.getCapabilityUsage().map { it.label to it.count },
            providerUsage = dao.getProviderUsage().map { it.label to it.count },
        )
    }
}
