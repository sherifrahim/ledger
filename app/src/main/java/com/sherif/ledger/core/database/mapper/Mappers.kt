package com.sherif.ledger.core.database.mapper

import com.sherif.ledger.core.database.entity.AccountEntity
import com.sherif.ledger.core.database.entity.BrandEntity
import com.sherif.ledger.core.database.entity.ParticipantEntity
import com.sherif.ledger.core.database.entity.SplitEntity
import com.sherif.ledger.core.database.entity.SplitShareEntity
import com.sherif.ledger.core.database.entity.TransactionEntity
import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.Brand
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Participant
import com.sherif.ledger.core.domain.model.Split
import com.sherif.ledger.core.domain.model.SplitShare
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionOrigin
import java.time.Instant

/**
 * Data mappers for converting between Room Entities and Domain Models.
 */

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type,
    openingBalance = Money(openingBalanceMinor, currencyCode),
    openingBalanceAsOf = openingBalanceAsOfMillis?.let { java.time.Instant.ofEpochMilli(it) },
    accountNumberTail = accountNumberTail,
    bankBrandId = bankBrandId,
    isCandidate = isCandidate,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type,
    openingBalanceMinor = openingBalance.minorUnits,
    openingBalanceAsOfMillis = openingBalanceAsOf?.toEpochMilli(),
    currencyCode = openingBalance.currencyCode,
    accountNumberTail = accountNumberTail,
    bankBrandId = bankBrandId,
    isCandidate = isCandidate,
)

fun BrandEntity.toDomain(): Brand = Brand(
    id = id,
    name = name,
    brandKey = brandKey,
    defaultCategoryId = defaultCategoryId
)

fun Brand.toEntity(): BrandEntity = BrandEntity(
    id = id,
    name = name,
    brandKey = brandKey,
    defaultCategoryId = defaultCategoryId
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    accountId = accountId,
    brandId = brandId,
    categoryId = categoryId,
    amount = Money(amountMinor, currencyCode),
    type = type,
    timestamp = Instant.ofEpochMilli(timestampEpochMillis),
    source = source,
    rawText = rawText,
    merchantText = merchantText,
    cardTail = cardTail,
    fingerprint = fingerprint,
    transferDirection = transferDirection,
    origin = if (originPackageName != null || originSenderIdentity != null) {
        TransactionOrigin(originPackageName, originSenderIdentity)
    } else null,
    note = note,
    noteUpdatedAt = noteUpdatedAt?.let { Instant.ofEpochMilli(it) },
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    accountId = accountId,
    brandId = brandId,
    categoryId = categoryId,
    amountMinor = amount.minorUnits,
    currencyCode = amount.currencyCode,
    type = type,
    timestampEpochMillis = timestamp.toEpochMilli(),
    source = source,
    rawText = rawText,
    merchantText = merchantText,
    cardTail = cardTail,
    fingerprint = fingerprint,
    transferDirection = transferDirection,
    originPackageName = origin?.packageName,
    originSenderIdentity = origin?.senderIdentity,
    note = note,
    noteUpdatedAt = noteUpdatedAt?.toEpochMilli(),
)

fun ParticipantEntity.toDomain(): Participant = Participant(
    id = id,
    name = name,
    isSelf = isSelf,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Participant.toEntity(): ParticipantEntity = ParticipantEntity(
    id = id,
    name = name,
    isSelf = isSelf,
    createdAt = createdAt.toEpochMilli(),
)

fun SplitEntity.toDomain(): Split = Split(
    id = id,
    transactionId = transactionId,
    splitType = splitType,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun SplitShareEntity.toDomain(): SplitShare = SplitShare(
    id = id,
    splitId = splitId,
    participantId = participantId,
    shareAmountMinor = shareAmountMinor,
    percentage = percentage,
    isSettled = isSettled,
    settledAt = settledAt?.let { Instant.ofEpochMilli(it) },
)



