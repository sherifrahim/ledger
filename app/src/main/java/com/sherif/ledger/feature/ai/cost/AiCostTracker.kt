package com.sherif.ledger.feature.ai.cost

import com.sherif.ledger.core.database.dao.AiAuditLogDao
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class AiCostSummary(
    val requestCount: Int,
    val estimatedCostUsd: Double,
    val averageLatencyMs: Long,
)

/**
 * RC5's "AI Cost Tracker" — approximate, on purpose. Pricing is a static
 * table of publicly-listed per-1K-token rates as of this file's authorship;
 * it will drift as vendors change pricing and is never fetched live. This
 * is for user cost AWARENESS ("am I racking up real spend?"), not billing —
 * never present [AiPricingTable] figures as authoritative.
 */
object AiPricingTable {
    /** USD per 1,000 tokens, blended input/output estimate. Providers/models absent here (Groq, Ollama, LM Studio, unrecognized models) are treated as free/unknown, never guessed. */
    private val blendedUsdPer1kTokens: Map<String, Double> = mapOf(
        "gpt-4o" to 0.005,
        "gpt-4o-mini" to 0.00015,
        "gpt-4.1" to 0.003,
        "claude-sonnet-4-5" to 0.006,
        "claude-opus-4-1" to 0.03,
        "claude-3-5-haiku-latest" to 0.001,
        "gemini-2.0-flash" to 0.0001,
        "gemini-1.5-pro" to 0.0025,
        "gemini-1.5-flash" to 0.00007,
    )

    /** Null when the model isn't in the table — callers must show "unknown," never fall back to a guessed number. */
    fun estimateCostUsd(model: String, tokens: Int): Double? =
        blendedUsdPer1kTokens[model]?.let { it * tokens / 1000.0 }
}

@Singleton
class AiCostTracker @Inject constructor(
    private val dao: AiAuditLogDao,
) {
    suspend fun todaySummary(): AiCostSummary {
        val startOfDayMillis = LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val entries = dao.getSince(startOfDayMillis)
        val estimatedCost = entries.sumOf { entry ->
            val tokens = entry.tokensUsed ?: return@sumOf 0.0
            AiPricingTable.estimateCostUsd(entry.model, tokens) ?: 0.0
        }
        val averageLatency = if (entries.isNotEmpty()) entries.sumOf { it.latencyMs } / entries.size else 0L
        return AiCostSummary(
            requestCount = entries.size,
            estimatedCostUsd = estimatedCost,
            averageLatencyMs = averageLatency,
        )
    }
}
