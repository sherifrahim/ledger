package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sherif.ledger.core.database.entity.LearnedDecisionEntity

@Dao
interface LearnedDecisionDao {
    @Query("SELECT * FROM learned_decisions")
    suspend fun getAll(): List<LearnedDecisionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LearnedDecisionEntity)
}
