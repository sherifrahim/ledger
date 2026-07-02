package com.sherif.ledger.core.domain.repository

/**
 * Interface for executing operations within a single atomic database transaction.
 * Ensures that either all operations succeed or none are committed.
 */
interface TransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}
