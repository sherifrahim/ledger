package com.sherif.ledger.core.domain.model

import java.time.Instant

/**
 * The canonical financial event (ADR-0001).
 *
 * A `FinancialEvent` is the immutable, source-agnostic record that *something
 * financial happened*. It is introduced **additively, alongside** [Transaction]
 * (canonical decision D2) — it is NOT a rename of Transaction, and the Balance
 * Engine and Financial Truth are preserved.
 *
 * Immutability & correction: an event is never mutated. A correction is a NEW
 * event that supersedes a prior one (see [supersedesEventId]); the superseded
 * event's [status] becomes [FinancialEventStatus.SUPERSEDED]. Derived reads that
 * eventually move onto events consider only [FinancialEventStatus.ACTIVE] events.
 *
 * During coexistence each event mirrors a [Transaction] 1:1 via [transactionId];
 * that link is nullable so a later, event-first capture can exist without one.
 */
data class FinancialEvent(
    /** Stable, source-agnostic identity (UUID). */
    val id: String,
    /** Originating Transaction during coexistence; null for event-first records. */
    val transactionId: Long?,
    val accountId: Long,
    val brandId: Long?,
    val categoryId: Long?,
    /** Money is always Long minor units; currency travels with the event. */
    val amount: Money,
    val type: TransactionType,
    val timestamp: Instant,
    val source: IngestionSource,
    /** Canonical confidence ladder, 0..100 (D5). */
    val confidence: Int,
    val status: FinancialEventStatus,
    /** Correction linkage: the id of the event this one replaces, if any. */
    val supersedesEventId: String?,
    /** Idempotency key — mirrors Transaction's unique fingerprint. */
    val fingerprint: String,
    val rawText: String?,
    val createdAt: Instant,
    /**
     * Mirrors [Transaction.transferDirection]. Added after [transaction_id] display
     * reads (Dashboard/Transactions/Story recent-activity rows) started consuming
     * [com.sherif.ledger.core.domain.model.isOutflow] — a direction-less transfer is
     * conservatively treated as outflow, so every mirrored transfer rendered as an
     * outgoing "−" regardless of its real direction. See EventToTransaction.kt.
     */
    val transferDirection: TransferDirection? = null,
)

/**
 * Lifecycle of a [FinancialEvent]. Events are immutable; status transitions are
 * expressed by recording new events, never by editing history.
 */
enum class FinancialEventStatus {
    /** The current, truth-bearing event. Only these feed derived reads. */
    ACTIVE,

    /** Replaced by a later correcting event (see FinancialEvent.supersedesEventId). */
    SUPERSEDED,

    /** Reversed / cancelled and intentionally excluded from derived reads. */
    VOID,
}
