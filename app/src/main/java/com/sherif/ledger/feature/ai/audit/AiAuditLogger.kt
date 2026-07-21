package com.sherif.ledger.feature.ai.audit

import com.sherif.ledger.core.database.dao.AiAuditLogDao
import com.sherif.ledger.core.database.entity.AiAuditLogEntity
import com.sherif.ledger.feature.ai.domain.AICapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AiAuditEntry(
    val timestampMillis: Long,
    val capability: AICapability,
    val providerId: String,
    val model: String,
    val latencyMs: Long,
    val tokensUsed: Int?,
    val success: Boolean,
    val confidencePercent: Int?,
    val errorSummary: String?,
)

/**
 * RC5's "AI Audit Log" — the ONLY writer of ai_audit_log. Called exactly
 * once per AIOrchestrator request, success or failure. Never receives (and
 * so can never accidentally persist) an API key, prompt text, or raw
 * response — see AiAuditLogEntity's doc comment for why those are excluded
 * on purpose.
 */
@Singleton
class AiAuditLogger @Inject constructor(
    private val dao: AiAuditLogDao,
) {
    suspend fun record(
        capability: AICapability,
        providerId: String,
        model: String,
        latencyMs: Long,
        tokensUsed: Int?,
        success: Boolean,
        confidencePercent: Int?,
        errorSummary: String?,
    ) {
        dao.insert(
            AiAuditLogEntity(
                timestampMillis = System.currentTimeMillis(),
                capability = capability.name,
                providerId = providerId,
                model = model,
                latencyMs = latencyMs,
                tokensUsed = tokensUsed,
                success = success,
                confidencePercent = confidencePercent,
                errorSummary = errorSummary,
            ),
        )
    }

    fun observeRecent(limit: Int = 50): Flow<List<AiAuditEntry>> =
        dao.observeRecent(limit).map { entries -> entries.map { it.toDomain() } }

    private fun AiAuditLogEntity.toDomain() = AiAuditEntry(
        timestampMillis = timestampMillis,
        capability = runCatching { AICapability.valueOf(capability) }.getOrDefault(AICapability.MERCHANT_CLASSIFICATION),
        providerId = providerId,
        model = model,
        latencyMs = latencyMs,
        tokensUsed = tokensUsed,
        success = success,
        confidencePercent = confidencePercent,
        errorSummary = errorSummary,
    )
}
