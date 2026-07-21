package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * RC5's "AI Audit Log" — every AI call, recorded. Deliberately excludes:
 * the API key (never stored here — see SecureApiKeyStore), the prompt text,
 * and the raw response content. Recording those would (a) risk leaking
 * financial detail into a table that's exported in diagnostic bundles later
 * if a collector is ever added, and (b) isn't what debugging AI behavior
 * actually needs — provider/model/latency/tokens/confidence/outcome is.
 */
@Entity(tableName = "ai_audit_log")
data class AiAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp_millis")
    val timestampMillis: Long,

    @ColumnInfo(name = "capability")
    val capability: String,

    @ColumnInfo(name = "provider_id")
    val providerId: String,

    @ColumnInfo(name = "model")
    val model: String,

    @ColumnInfo(name = "latency_ms")
    val latencyMs: Long,

    @ColumnInfo(name = "tokens_used")
    val tokensUsed: Int?,

    @ColumnInfo(name = "success")
    val success: Boolean,

    @ColumnInfo(name = "confidence_percent")
    val confidencePercent: Int?,

    @ColumnInfo(name = "error_summary")
    val errorSummary: String?,
)
