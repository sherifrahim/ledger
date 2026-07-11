package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.repository.TransactionRunner
import javax.inject.Inject

/**
 * Explicitly reassigns transactions matching an exact origin signature (package +
 * card tail) from one account to another.
 *
 * Deliberately NOT invoked automatically anywhere in the ingestion pipeline.
 * [com.sherif.ledger.core.domain.service.account.DeterministicAccountIdentityResolver]
 * only ever binds NEW transactions correctly going forward once an identity
 * crosses its evidence bar — it never rewrites history as a side effect. This use
 * case is the separate, explicit mechanism for that migration, meant to be called
 * from a future user-facing confirmation flow (not built in this phase), or
 * directly when the correction is known to be safe.
 *
 * - Bounded: only rows matching the exact (fromAccountId, packageName, cardTail)
 *   signature are touched — never a broad scan.
 * - Transactional: wrapped in [TransactionRunner.runInTransaction] — all rows move
 *   or none do.
 * - Idempotent: a second call with the same arguments matches zero rows, since no
 *   transactions with that signature remain on [fromAccountId] after the first
 *   successful run.
 */
class ReassignHistoricalTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionRunner: TransactionRunner,
) {
    suspend fun execute(
        fromAccountId: Long,
        packageName: String,
        cardTail: String,
        toAccountId: Long,
    ): Result {
        if (fromAccountId == toAccountId) return Result(0)
        val rowsAffected = transactionRunner.runInTransaction {
            transactionRepository.reassignTransactions(fromAccountId, packageName, cardTail, toAccountId)
        }
        return Result(rowsAffected)
    }

    data class Result(val transactionsReassigned: Int)
}

