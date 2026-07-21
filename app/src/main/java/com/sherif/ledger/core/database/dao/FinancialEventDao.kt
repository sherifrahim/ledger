package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sherif.ledger.core.database.entity.FinancialEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the canonical `financial_events` table (ADR-0001).
 *
 * Inserts IGNORE on conflict so recording is idempotent by the unique
 * `fingerprint` — the same guarantee the transaction path relies on. No update
 * or delete methods are exposed: events are immutable, and a correction is a new
 * event that supersedes a prior one (status transition, never an in-place edit).
 */
@Dao
interface FinancialEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: FinancialEventEntity): Long

    @Query("SELECT * FROM financial_events WHERE id = :id")
    suspend fun getById(id: String): FinancialEventEntity?

    @Query("SELECT * FROM financial_events WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getByTransactionId(transactionId: Long): FinancialEventEntity?

    @Query("SELECT * FROM financial_events WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getByFingerprint(fingerprint: String): FinancialEventEntity?

    @Query("SELECT * FROM financial_events WHERE status = 'ACTIVE' ORDER BY timestamp_millis ASC")
    fun observeActive(): Flow<List<FinancialEventEntity>>

    /** Marks a prior event superseded by a correcting one. Immutable-safe: only a
     *  lifecycle flag changes, the event's financial fields are never rewritten. */
    @Query("UPDATE financial_events SET status = 'SUPERSEDED' WHERE id = :id")
    suspend fun markSuperseded(id: String): Int

    /** Voids the mirror event(s) of a soft-deleted transaction so event-first reads
     *  exclude it, matching the legacy `is_deleted = 0` filter. Lifecycle flag only. */
    @Query("UPDATE financial_events SET status = 'VOID' WHERE transaction_id = :transactionId")
    suspend fun voidByTransactionId(transactionId: Long): Int

    @Query("SELECT COUNT(*) FROM financial_events")
    suspend fun count(): Int
}
