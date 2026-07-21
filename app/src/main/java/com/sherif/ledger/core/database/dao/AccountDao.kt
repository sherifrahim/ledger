package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sherif.ledger.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    // RC7 Phase B: tightened to also exclude candidates — a no-op change until
    // the Account Resolver actually creates one (is_candidate was always 0
    // before RC7), so every existing caller's behavior is unchanged until a
    // real candidate exists. See observeCandidateAccounts() below for the
    // dedicated, separate view Developer Console uses.
    @Query("SELECT * FROM accounts WHERE is_deleted = 0 AND is_candidate = 0")
    fun observeAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id AND is_deleted = 0")
    suspend fun getAccountById(id: Long): AccountEntity?

    /** Accounts excluded from every balance/net-worth computation (Balance Inspector, Part 3: "every excluded account, why it was excluded"). */
    @Query("SELECT * FROM accounts WHERE is_deleted = 1")
    suspend fun getDeletedAccounts(): List<AccountEntity>

    /** RC7 Phase B: unresolved-institution accounts awaiting promotion or dismissal — never included in observeAllAccounts()/any balance figure. */
    @Query("SELECT * FROM accounts WHERE is_deleted = 0 AND is_candidate = 1")
    fun observeCandidateAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity): Int

    @Query("UPDATE accounts SET is_deleted = 1 WHERE id = :id")
    suspend fun softDeleteAccount(id: Long): Int
}
