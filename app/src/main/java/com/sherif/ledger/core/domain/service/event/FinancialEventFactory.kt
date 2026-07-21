package com.sherif.ledger.core.domain.service.event

import com.sherif.ledger.core.domain.model.FinancialEvent
import com.sherif.ledger.core.domain.model.FinancialEventStatus
import com.sherif.ledger.core.domain.model.Transaction
import java.time.Instant
import java.util.UUID

/**
 * Builds the canonical [FinancialEvent] that mirrors a persisted [Transaction]
 * during coexistence (ADR-0001, Milestone 2 Track B — dual-write).
 *
 * Pure and deterministic given its inputs, so it is unit-testable without the
 * insert pipeline. The event carries the transaction's own financial facts
 * verbatim — same account, money, type, timestamp, source and fingerprint — which
 * is what makes parity between the two stores checkable and the eventual
 * read-migration a no-op in value.
 */
object FinancialEventFactory {

    /** Confidence assigned to a mirror of an already-accepted transaction. The
     *  transaction was persisted as Financial Truth, so its mirror is Deterministic. */
    const val DETERMINISTIC_CONFIDENCE = 100

    fun mirrorOf(
        transaction: Transaction,
        confidence: Int = DETERMINISTIC_CONFIDENCE,
        id: String = UUID.randomUUID().toString(),
        createdAt: Instant = Instant.now(),
    ): FinancialEvent = FinancialEvent(
        id = id,
        transactionId = transaction.id,
        accountId = transaction.accountId,
        brandId = transaction.brandId,
        categoryId = transaction.categoryId,
        amount = transaction.amount,
        type = transaction.type,
        timestamp = transaction.timestamp,
        source = transaction.source,
        confidence = confidence,
        status = FinancialEventStatus.ACTIVE,
        supersedesEventId = null,
        fingerprint = transaction.fingerprint,
        rawText = transaction.rawText,
        createdAt = createdAt,
    )
}
