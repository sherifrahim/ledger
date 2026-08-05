package com.sherif.ledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sherif.ledger.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE id = :id AND is_deleted = 0")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY timestamp_millis DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE is_deleted = 0 ORDER BY timestamp_millis ASC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE account_id = :accountId AND is_deleted = 0 ORDER BY timestamp_millis DESC")
    fun observeTransactionsForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp_millis >= :start AND timestamp_millis <= :end AND is_deleted = 0 ORDER BY timestamp_millis DESC")
    fun observeTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("UPDATE transactions SET is_deleted = 1, deleted_at = :timestamp WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE transactions SET note = :note, note_updated_at = :timestamp WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?, timestamp: Long): Int

    /** Grouped, indexed count of transactions sharing an exact origin signature,
     *  by which account they currently sit on. Bounded by the (small) set of
     *  distinct accounts a signature has ever touched — not a full scan. */
    @Query(
        """
        SELECT account_id AS accountId, COUNT(*) AS count
        FROM transactions
        WHERE origin_package_name = :packageName AND card_tail = :cardTail AND is_deleted = 0
        GROUP BY account_id
        """
    )
    suspend fun countByOriginSignature(packageName: String, cardTail: String): List<AccountOriginCountRow>

    /** Bounded, single-statement reassignment. Idempotent: a second call matches
     *  zero rows once the first has moved everything off [fromAccountId]. */
    @Query(
        """
        UPDATE transactions SET account_id = :toAccountId
        WHERE account_id = :fromAccountId AND origin_package_name = :packageName
          AND card_tail = :cardTail AND is_deleted = 0
        """
    )
    suspend fun reassignByOriginSignature(
        fromAccountId: Long,
        packageName: String,
        cardTail: String,
        toAccountId: Long,
    ): Int

    /**
     * Reassigns transactions from the same institution that quote NO account
     * number. A bank sends both shapes — "…from acc. no. XXX920001…" and messages
     * with no account number at all — and the tail-less ones can never be matched
     * by [reassignByOriginSignature]. Left behind, they strand part of one real
     * account on the fallback account.
     *
     * Deliberately narrow: same origin package, card_tail IS NULL, and only ever
     * called when that institution has exactly one confirmed account, so there is
     * no second card the message could have belonged to.
     */
    @Query(
        """
        UPDATE transactions SET account_id = :toAccountId
        WHERE account_id = :fromAccountId AND origin_package_name = :packageName
          AND card_tail IS NULL AND is_deleted = 0
        """
    )
    suspend fun reassignUntailedByOrigin(
        fromAccountId: Long,
        packageName: String,
        toAccountId: Long,
    ): Int

    /**
     * Every transaction on [fromAccountId], regardless of origin signature —
     * unlike [reassignByOriginSignature]/[reassignUntailedByOrigin], which only
     * ever move what a specific identity match justifies. This is the blanket
     * move a user-initiated account merge needs: "everything on this account now
     * belongs to that one," not "everything matching this one signature."
     */
    @Query("UPDATE transactions SET account_id = :toAccountId WHERE account_id = :fromAccountId AND is_deleted = 0")
    suspend fun reassignAllByAccount(fromAccountId: Long, toAccountId: Long): Int
}

/** Room projection for [countByOriginSignature]. */
data class AccountOriginCountRow(val accountId: Long, val count: Int)



