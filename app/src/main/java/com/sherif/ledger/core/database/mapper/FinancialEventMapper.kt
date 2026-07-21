package com.sherif.ledger.core.database.mapper

import com.sherif.ledger.core.database.entity.FinancialEventEntity
import com.sherif.ledger.core.domain.model.FinancialEvent
import com.sherif.ledger.core.domain.model.Money
import java.time.Instant

/**
 * Entity ↔ domain mappers for the canonical FinancialEvent (ADR-0001).
 * Money is reconstructed from its `amount_minor` + `currency_code` columns, the
 * same pattern the Transaction mapper uses — persistence types never leak into
 * the domain model.
 */
fun FinancialEventEntity.toDomain(): FinancialEvent = FinancialEvent(
    id = id,
    transactionId = transactionId,
    accountId = accountId,
    brandId = brandId,
    categoryId = categoryId,
    amount = Money(amountMinor, currencyCode),
    type = type,
    timestamp = Instant.ofEpochMilli(timestampEpochMillis),
    source = source,
    confidence = confidence,
    status = status,
    supersedesEventId = supersedesEventId,
    fingerprint = fingerprint,
    rawText = rawText,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun FinancialEvent.toEntity(): FinancialEventEntity = FinancialEventEntity(
    id = id,
    transactionId = transactionId,
    accountId = accountId,
    brandId = brandId,
    categoryId = categoryId,
    amountMinor = amount.minorUnits,
    currencyCode = amount.currencyCode,
    type = type,
    timestampEpochMillis = timestamp.toEpochMilli(),
    source = source,
    confidence = confidence,
    status = status,
    supersedesEventId = supersedesEventId,
    fingerprint = fingerprint,
    rawText = rawText,
    createdAt = createdAt.toEpochMilli(),
)
