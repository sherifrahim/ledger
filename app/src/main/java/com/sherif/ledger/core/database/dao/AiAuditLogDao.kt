package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sherif.ledger.core.database.entity.AiAuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiAuditLogDao {
    @Insert
    suspend fun insert(entry: AiAuditLogEntity)

    @Query("SELECT * FROM ai_audit_log ORDER BY timestamp_millis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AiAuditLogEntity>>

    @Query("SELECT * FROM ai_audit_log WHERE timestamp_millis >= :sinceMillis")
    suspend fun getSince(sinceMillis: Long): List<AiAuditLogEntity>

    @Query("SELECT COUNT(*) FROM ai_audit_log")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM ai_audit_log WHERE success = 1")
    suspend fun getSuccessCount(): Int

    @Query("SELECT AVG(latency_ms) FROM ai_audit_log")
    suspend fun getAverageLatencyMs(): Double?

    @Query("SELECT AVG(tokens_used) FROM ai_audit_log WHERE tokens_used IS NOT NULL")
    suspend fun getAverageTokens(): Double?

    @Query("SELECT capability AS label, COUNT(*) AS count FROM ai_audit_log GROUP BY capability ORDER BY count DESC")
    suspend fun getCapabilityUsage(): List<LabelCount>

    @Query("SELECT provider_id AS label, COUNT(*) AS count FROM ai_audit_log GROUP BY provider_id ORDER BY count DESC")
    suspend fun getProviderUsage(): List<LabelCount>
}

data class LabelCount(val label: String, val count: Int)
