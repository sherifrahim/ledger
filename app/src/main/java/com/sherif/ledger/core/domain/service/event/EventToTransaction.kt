package com.sherif.ledger.core.domain.service.event

import com.sherif.ledger.core.domain.model.FinancialEvent
import com.sherif.ledger.core.domain.model.Transaction

/**
 * Reconstructs a [Transaction]-shaped domain object from a mirror [FinancialEvent]
 * (ADR-0001, P7 event-first reads; also used by the P6 parity harness).
 *
 * The FinancialEvent intentionally does NOT carry `cardTail`, `transferDirection`,
 * `origin` or `note`; those become null here. Its single text field holds the
 * MERCHANT (see FinancialEventFactory), so it is reconstructed into both `rawText`
 * and `merchantText` — every event-first read goes through `merchantOrRawText` and
 * therefore sees exactly what the legacy Transaction read sees. The captured
 * message is not recoverable from an event and was never meant to be; it lives in
 * transactions.raw_text. This is safe for the reads that are
 * migrated event-first (analytics, stories, merchant, review, search) — none of them
 * consume those fields. The two reads that DO — balance (`BalanceCalculator`, via
 * `AccountBalanceService`) and the transaction-detail record view — remain deliberate,
 * documented legacy Transaction reads. See docs/READ_PARITY_REPORT.md.
 */
fun FinancialEvent.toMirrorTransaction(): Transaction = Transaction(
    id = transactionId ?: 0L,
    accountId = accountId,
    brandId = brandId,
    categoryId = categoryId,
    amount = amount,
    type = type,
    timestamp = timestamp,
    source = source,
    rawText = rawText,
    merchantText = rawText,
    cardTail = null,
    fingerprint = fingerprint,
    transferDirection = null,
    origin = null,
    note = null,
    noteUpdatedAt = null,
)
