package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.FinancialEvent
import kotlinx.coroutines.flow.Flow

/**
 * Access to the canonical FinancialEvent store (ADR-0001).
 *
 * Writes are append-only and idempotent (by fingerprint). There is deliberately
 * no update/delete of an existing event: a correction is recorded as a NEW event
 * that supersedes a prior one via [supersede]. In Milestone 2 step 1 this store
 * exists but is not yet written to or read from by any feature.
 */
interface FinancialEventRepository {

    /** Append an event. Idempotent: a duplicate fingerprint is a no-op. */
    suspend fun record(event: FinancialEvent)

    suspend fun findByTransactionId(transactionId: Long): FinancialEvent?

    suspend fun findByFingerprint(fingerprint: String): FinancialEvent?

    /** Only ACTIVE events — the truth-bearing set derived reads will eventually use. */
    fun observeActive(): Flow<List<FinancialEvent>>

    /**
     * Record [correction] and mark the event it replaces SUPERSEDED. History is
     * never mutated in place — the prior event stays, only its lifecycle flag moves.
     */
    suspend fun supersede(supersededEventId: String, correction: FinancialEvent)

    /**
     * Voids the mirror event of a soft-deleted transaction (lifecycle flag → VOID) so
     * event-first reads exclude it, matching the legacy `is_deleted = 0` filter. History
     * is preserved (the row stays); only its status changes.
     */
    suspend fun voidByTransactionId(transactionId: Long)

    suspend fun count(): Int
}
