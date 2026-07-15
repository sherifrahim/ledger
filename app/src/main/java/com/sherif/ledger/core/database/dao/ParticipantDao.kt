package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sherif.ledger.core.database.entity.ParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {

    @Query("SELECT * FROM participants ORDER BY name ASC")
    fun observeAll(): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE id = :id")
    suspend fun getById(id: String): ParticipantEntity?

    @Query("SELECT * FROM participants WHERE is_self = 1 LIMIT 1")
    suspend fun getSelf(): ParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(participant: ParticipantEntity)

    @Query("DELETE FROM participants WHERE id = :id")
    suspend fun delete(id: String)
}

