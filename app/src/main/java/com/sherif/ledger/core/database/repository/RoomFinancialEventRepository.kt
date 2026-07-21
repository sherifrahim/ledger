package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.database.dao.FinancialEventDao
import com.sherif.ledger.core.database.mapper.toDomain
import com.sherif.ledger.core.database.mapper.toEntity
import com.sherif.ledger.core.domain.model.FinancialEvent
import com.sherif.ledger.core.domain.repository.FinancialEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [FinancialEventRepository] (ADR-0001).
 *
 * Append-only and idempotent: [record] delegates to an IGNORE-on-conflict insert
 * keyed by the unique fingerprint. [supersede] appends the correction and flips
 * the prior event's lifecycle flag — it never rewrites a stored event's financial
 * fields.
 */
class RoomFinancialEventRepository @Inject constructor(
    private val dao: FinancialEventDao,
) : FinancialEventRepository {

    override suspend fun record(event: FinancialEvent) {
        dao.insert(event.toEntity())
    }

    override suspend fun findByTransactionId(transactionId: Long): FinancialEvent? =
        dao.getByTransactionId(transactionId)?.toDomain()

    override suspend fun findByFingerprint(fingerprint: String): FinancialEvent? =
        dao.getByFingerprint(fingerprint)?.toDomain()

    override fun observeActive(): Flow<List<FinancialEvent>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override suspend fun supersede(supersededEventId: String, correction: FinancialEvent) {
        dao.insert(correction.toEntity())
        dao.markSuperseded(supersededEventId)
    }

    override suspend fun count(): Int = dao.count()
}
