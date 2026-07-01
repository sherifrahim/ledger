package com.sherif.ledger.core.database.mapper

import com.sherif.ledger.core.database.entity.AccountEntity
import com.sherif.ledger.core.database.entity.BrandEntity
import com.sherif.ledger.core.database.entity.CategoryEntity
import com.sherif.ledger.core.database.entity.TransactionEntity
import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.Brand
import com.sherif.ledger.core.domain.model.Category
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import java.time.Instant

/**
 * Data mappers for converting between Room Entities and Domain Models.
 */

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type,
    balance = Money.zero(currencyCode), // Balance is a derived value, defaults to zero
    accountNumberTail = accountNumberTail,
    bankBrandId = bankBrandId
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type,
    currencyCode = balance.currencyCode,
    accountNumberTail = accountNumberTail,
    bankBrandId = bankBrandId
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

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    parentId = parentId,
    iconKey = iconKey,
    colorHue = colorHue
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    parentId = parentId,
    iconKey = iconKey,
    colorHue = colorHue
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
    fingerprint = fingerprint
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
    fingerprint = fingerprint
)
