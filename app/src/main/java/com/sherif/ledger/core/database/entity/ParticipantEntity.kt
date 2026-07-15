package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    @ColumnInfo(name = "is_self", defaultValue = "0")
    val isSelf: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

