package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sherif.ledger.core.database.entity.SplitEntity
import com.sherif.ledger.core.database.entity.SplitShareEntity
import com.sherif.ledger.core.domain.model.SplitType
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {

    @Query("SELECT * FROM splits WHERE transaction_id = :transactionId LIMIT 1")
    fun observeByTransaction(transactionId: Long): Flow<SplitEntity?>

    @Query("SELECT * FROM splits WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getByTransaction(transactionId: Long): SplitEntity?

    @Query("SELECT * FROM splits WHERE id = :splitId")
    suspend fun getById(splitId: String): SplitEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSplit(split: SplitEntity)

    @Query("UPDATE splits SET split_type = :type, updated_at = :timestamp WHERE id = :splitId")
    suspend fun updateSplitType(splitId: String, type: SplitType, timestamp: Long)

    @Query("UPDATE splits SET updated_at = :timestamp WHERE id = :splitId")
    suspend fun touch(splitId: String, timestamp: Long)

    @Query("DELETE FROM splits WHERE id = :splitId")
    suspend fun deleteSplit(splitId: String)

    @Query("SELECT * FROM split_shares WHERE split_id = :splitId")
    fun observeShares(splitId: String): Flow<List<SplitShareEntity>>

    @Query("SELECT * FROM split_shares WHERE split_id = :splitId")
    suspend fun getShares(splitId: String): List<SplitShareEntity>

    @Query("SELECT * FROM split_shares WHERE id = :shareId")
    suspend fun getShareById(shareId: String): SplitShareEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: SplitShareEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<SplitShareEntity>)

    @Query("DELETE FROM split_shares WHERE split_id = :splitId AND participant_id = :participantId")
    suspend fun deleteShareForParticipant(splitId: String, participantId: String)

    @Query("UPDATE split_shares SET share_amount_minor = :amountMinor, percentage = :percentage WHERE id = :shareId")
    suspend fun updateShareAmount(shareId: String, amountMinor: Long, percentage: Double?)

    @Query("UPDATE split_shares SET is_settled = :settled, settled_at = :settledAt WHERE id = :shareId")
    suspend fun updateSettled(shareId: String, settled: Boolean, settledAt: Long?)

    @Query("SELECT * FROM split_shares WHERE participant_id = :participantId AND is_settled = 0")
    fun observeOutstandingForParticipant(participantId: String): Flow<List<SplitShareEntity>>
}


