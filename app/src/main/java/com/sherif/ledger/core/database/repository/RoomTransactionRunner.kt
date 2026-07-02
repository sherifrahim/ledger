package com.sherif.ledger.core.database.repository

import androidx.room.withTransaction
import com.sherif.ledger.core.database.LedgerDatabase
import com.sherif.ledger.core.domain.repository.TransactionRunner
import javax.inject.Inject

class RoomTransactionRunner @Inject constructor(
    private val database: LedgerDatabase
) : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return database.withTransaction {
            block()
        }
    }
}
