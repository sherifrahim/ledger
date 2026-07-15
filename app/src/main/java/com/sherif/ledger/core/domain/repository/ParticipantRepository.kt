package com.sherif.ledger.core.domain.repository

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Participant
import kotlinx.coroutines.flow.Flow

interface ParticipantRepository {
    fun observeAll(): Flow<LedgerResult<List<Participant>>>

    /** Returns the single reserved self participant, creating it on first use. */
    suspend fun getOrCreateSelf(): LedgerResult<Participant>

    suspend fun createParticipant(name: String): LedgerResult<Participant>

    suspend fun deleteParticipant(id: String): LedgerResult<Unit>
}


