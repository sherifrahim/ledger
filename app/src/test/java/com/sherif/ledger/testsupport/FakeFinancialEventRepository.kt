package com.sherif.ledger.testsupport

import com.sherif.ledger.core.domain.model.FinancialEvent
import com.sherif.ledger.core.domain.model.FinancialEventStatus
import com.sherif.ledger.core.domain.repository.FinancialEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [FinancialEventRepository] for unit tests. `internal` so a single copy
 * is shared across every test file in the module (top-level test doubles with the
 * same name in different files would otherwise collide in the package namespace).
 * Idempotent by fingerprint, mirroring the real DAO's IGNORE-on-conflict.
 */
internal class FakeFinancialEventRepository : FinancialEventRepository {
    private val state = MutableStateFlow<List<FinancialEvent>>(emptyList())
    val events: List<FinancialEvent> get() = state.value

    override suspend fun record(event: FinancialEvent) {
        if (state.value.none { it.fingerprint == event.fingerprint }) {
            state.value = state.value + event
        }
    }

    override suspend fun findByTransactionId(transactionId: Long): FinancialEvent? =
        state.value.firstOrNull { it.transactionId == transactionId }

    override suspend fun findByFingerprint(fingerprint: String): FinancialEvent? =
        state.value.firstOrNull { it.fingerprint == fingerprint }

    override fun observeActive(): Flow<List<FinancialEvent>> =
        state.map { list -> list.filter { it.status == FinancialEventStatus.ACTIVE } }

    override suspend fun supersede(supersededEventId: String, correction: FinancialEvent) {
        record(correction)
        state.value = state.value.map {
            if (it.id == supersededEventId) it.copy(status = FinancialEventStatus.SUPERSEDED) else it
        }
    }

    override suspend fun count(): Int = state.value.size
}
