package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.database.dao.ParticipantDao
import com.sherif.ledger.core.database.entity.ParticipantEntity
import com.sherif.ledger.core.database.mapper.toDomain
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Participant
import com.sherif.ledger.core.domain.repository.ParticipantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class RoomParticipantRepository @Inject constructor(
    private val participantDao: ParticipantDao,
) : ParticipantRepository {

    override fun observeAll(): Flow<LedgerResult<List<Participant>>> =
        participantDao.observeAll()
            .map { entities -> LedgerResult.Success(entities.map { it.toDomain() }) as LedgerResult<List<Participant>> }
            .catch { e -> emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error"))) }

    override suspend fun getOrCreateSelf(): LedgerResult<Participant> = try {
        val existing = participantDao.getSelf()
        if (existing != null) {
            LedgerResult.Success(existing.toDomain())
        } else {
            val entity = ParticipantEntity(
                id = UUID.randomUUID().toString(),
                name = "You",
                isSelf = true,
                createdAt = System.currentTimeMillis(),
            )
            participantDao.insert(entity)
            LedgerResult.Success(entity.toDomain())
        }
    } catch (e: Exception) {
        LedgerLogger.e("RoomParticipantRepository: getOrCreateSelf failed", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun createParticipant(name: String): LedgerResult<Participant> {
        if (name.isBlank()) return LedgerResult.Failure(LedgerError.Unknown("Participant name cannot be blank"))
        return try {
            val entity = ParticipantEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                isSelf = false,
                createdAt = System.currentTimeMillis(),
            )
            participantDao.insert(entity)
            LedgerResult.Success(entity.toDomain())
        } catch (e: Exception) {
            LedgerLogger.e("RoomParticipantRepository: createParticipant failed", e)
            LedgerResult.Failure(LedgerError.DatabaseFailure)
        }
    }

    override suspend fun deleteParticipant(id: String): LedgerResult<Unit> = try {
        participantDao.delete(id)
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerLogger.e("RoomParticipantRepository: deleteParticipant failed", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }
}

