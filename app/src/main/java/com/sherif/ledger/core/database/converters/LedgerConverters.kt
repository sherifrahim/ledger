package com.sherif.ledger.core.database.converters

import androidx.room.TypeConverter
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.SplitType
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import java.time.Instant

/**
 * Room type converters for Ledger's domain models and system types.
 */
class LedgerConverters {

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(millis: Long?): Instant? = millis?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromIngestionSource(source: IngestionSource): String = source.name

    @TypeConverter
    fun toIngestionSource(value: String): IngestionSource = IngestionSource.valueOf(value)

    @TypeConverter
    fun fromTransferDirection(direction: TransferDirection?): String? = direction?.name

    @TypeConverter
    fun toTransferDirection(value: String?): TransferDirection? = value?.let { TransferDirection.valueOf(it) }

    @TypeConverter
    fun fromSplitType(type: SplitType): String = type.name

    @TypeConverter
    fun toSplitType(value: String): SplitType = SplitType.valueOf(value)
}




