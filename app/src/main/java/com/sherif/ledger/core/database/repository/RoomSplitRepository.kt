package com.sherif.ledger.core.database.repository

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.database.dao.ParticipantDao
import com.sherif.ledger.core.database.dao.SplitDao
import com.sherif.ledger.core.database.entity.SplitEntity
import com.sherif.ledger.core.database.entity.SplitShareEntity
import com.sherif.ledger.core.database.mapper.toDomain
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.ShareWithParticipant
import com.sherif.ledger.core.domain.model.SplitType
import com.sherif.ledger.core.domain.model.SplitWithShares
import com.sherif.ledger.core.domain.repository.ShareInput
import com.sherif.ledger.core.domain.repository.SplitRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.split.SplitCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * Split creation and editing. Reads a transaction's own amount (via
 * [TransactionRepository], read-only) to know the total to divide — it never
 * duplicates that amount into its own storage. Otherwise completely
 * independent of Financial Truth: never calls BalanceCalculator,
 * RelationshipEngine, or GetFinancialAnalyticsUseCase, and every write here
 * only ever touches the splits/split_shares/participants tables.
 */
class RoomSplitRepository @Inject constructor(
    private val splitDao: SplitDao,
    private val participantDao: ParticipantDao,
    private val transactionRepository: TransactionRepository,
    private val splitCalculator: SplitCalculator,
) : SplitRepository {

    override fun observeSplitForTransaction(transactionId: Long): Flow<LedgerResult<SplitWithShares?>> =
        splitDao.observeByTransaction(transactionId).flatMapLatest { splitEntity ->
            if (splitEntity == null) {
                flowOf(LedgerResult.Success(null) as LedgerResult<SplitWithShares?>)
            } else {
                combine(
                    splitDao.observeShares(splitEntity.id),
                    participantDao.observeAll(),
                ) { shareEntities, participantEntities ->
                    val participantsById = participantEntities.associateBy { it.id }
                    val shares = shareEntities.mapNotNull { shareEntity ->
                        val participant = participantsById[shareEntity.participantId] ?: return@mapNotNull null
                        ShareWithParticipant(shareEntity.toDomain(), participant.toDomain())
                    }
                    LedgerResult.Success(SplitWithShares(splitEntity.toDomain(), shares)) as LedgerResult<SplitWithShares?>
                }
            }
        }.catch { e -> emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error"))) }

    override suspend fun getSplitForTransaction(transactionId: Long): LedgerResult<SplitWithShares?> = try {
        val splitEntity = splitDao.getByTransaction(transactionId)
        if (splitEntity == null) {
            LedgerResult.Success(null)
        } else {
            val shareEntities = splitDao.getShares(splitEntity.id)
            val shares = shareEntities.mapNotNull { shareEntity ->
                val participant = participantDao.getById(shareEntity.participantId) ?: return@mapNotNull null
                ShareWithParticipant(shareEntity.toDomain(), participant.toDomain())
            }
            LedgerResult.Success(SplitWithShares(splitEntity.toDomain(), shares))
        }
    } catch (e: Exception) {
        LedgerLogger.e("RoomSplitRepository: getSplitForTransaction failed", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun createSplit(
        transactionId: Long,
        splitType: SplitType,
        participantShares: Map<String, ShareInput>,
    ): LedgerResult<String> {
        if (splitDao.getByTransaction(transactionId) != null) {
            return LedgerResult.Failure(LedgerError.Unknown("A split already exists for this transaction"))
        }
        if (participantShares.isEmpty()) {
            return LedgerResult.Failure(LedgerError.Unknown("A split needs at least one participant"))
        }
        val totalMinor = totalMinorFor(transactionId) ?: return LedgerResult.Failure(LedgerError.Unknown("Transaction not found"))

        val shareEntities = try {
            buildShareEntities(splitId = "", participantShares, splitType, totalMinor)
        } catch (e: IllegalArgumentException) {
            return LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Invalid split shares"))
        }

        return try {
            val splitId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            splitDao.insertSplit(SplitEntity(splitId, transactionId, splitType, now, now))
            splitDao.insertShares(shareEntities.map { it.copy(id = UUID.randomUUID().toString(), splitId = splitId) })
            LedgerResult.Success(splitId)
        } catch (e: Exception) {
            LedgerLogger.e("RoomSplitRepository: createSplit failed", e)
            LedgerResult.Failure(LedgerError.DatabaseFailure)
        }
    }

    override suspend fun addParticipant(splitId: String, participantId: String, share: ShareInput): LedgerResult<Unit> {
        return try {
        val splitEntity = splitDao.getById(splitId) ?: return LedgerResult.Failure(LedgerError.Unknown("Split not found"))
        val totalMinor = totalMinorFor(splitEntity.transactionId) ?: return LedgerResult.Failure(LedgerError.Unknown("Transaction not found"))
        val existingShares = splitDao.getShares(splitId)

        if (existingShares.any { it.participantId == participantId }) {
            return LedgerResult.Failure(LedgerError.Unknown("Participant is already part of this split"))
        }

        when (splitEntity.splitType) {
            SplitType.EQUAL -> {
                val totalParticipantCount = existingShares.size + 2 // +1 new, +1 self
                val perPerson = splitCalculator.equalShare(totalMinor, totalParticipantCount)
                existingShares.forEach { splitDao.updateShareAmount(it.id, perPerson, null) }
                splitDao.insertShare(
                    SplitShareEntity(UUID.randomUUID().toString(), splitId, participantId, perPerson, null)
                )
            }
            SplitType.EXACT -> {
                val amount = (share as? ShareInput.Exact)?.amountMinor
                    ?: return LedgerResult.Failure(LedgerError.Unknown("EXACT split requires an explicit amount"))
                val proposed = existingShares.map { it.shareAmountMinor } + amount
                if (!splitCalculator.isWithinTotal(proposed, totalMinor)) {
                    return LedgerResult.Failure(LedgerError.Unknown("Shares would exceed the transaction total"))
                }
                splitDao.insertShare(SplitShareEntity(UUID.randomUUID().toString(), splitId, participantId, amount, null))
            }
            SplitType.PERCENTAGE -> {
                val pct = (share as? ShareInput.Percent)?.percentage
                    ?: return LedgerResult.Failure(LedgerError.Unknown("PERCENTAGE split requires an explicit percentage"))
                val proposed = existingShares.mapNotNull { it.percentage } + pct
                if (!splitCalculator.isWithinTotalPercentage(proposed)) {
                    return LedgerResult.Failure(LedgerError.Unknown("Percentages would exceed 100%"))
                }
                val amount = splitCalculator.percentageShare(totalMinor, pct)
                splitDao.insertShare(SplitShareEntity(UUID.randomUUID().toString(), splitId, participantId, amount, pct))
            }
        }
        splitDao.touch(splitId, System.currentTimeMillis())
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerLogger.e("RoomSplitRepository: addParticipant failed", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }
    }

    override suspend fun removeParticipant(splitId: String, participantId: String): LedgerResult<Unit> {
        return try {
        val splitEntity = splitDao.getById(splitId) ?: return LedgerResult.Failure(LedgerError.Unknown("Split not found"))
        splitDao.deleteShareForParticipant(splitId, participantId)

        if (splitEntity.splitType == SplitType.EQUAL) {
            val remaining = splitDao.getShares(splitId)
            if (remaining.isNotEmpty()) {
                val totalMinor = totalMinorFor(splitEntity.transactionId)
                if (totalMinor != null) {
                    val perPerson = splitCalculator.equalShare(totalMinor, remaining.size + 1) // +1 self
                    remaining.forEach { splitDao.updateShareAmount(it.id, perPerson, null) }
                }
            }
        }
        // EXACT/PERCENTAGE: remaining shares are left exactly as the user set
        // them — removing a participant never implicitly redistributes their
        // amount onto anyone else.
        splitDao.touch(splitId, System.currentTimeMillis())
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerLogger.e("RoomSplitRepository: removeParticipant failed", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }
    }

    override suspend fun updateShare(shareId: String, share: ShareInput): LedgerResult<Unit> {
        return try {
        val shareEntity = splitDao.getShareById(shareId) ?: return LedgerResult.Failure(LedgerError.Unknown("Share not found"))
        val splitEntity = splitDao.getById(shareEntity.splitId) ?: return LedgerResult.Failure(LedgerError.Unknown("Split not found"))
        if (splitEntity.splitType == SplitType.EQUAL) {
            return LedgerResult.Failure(LedgerError.Unknown("EQUAL shares only change via add/removeParticipant"))
        }
        val totalMinor = totalMinorFor(splitEntity.transactionId) ?: return LedgerResult.Failure(LedgerError.Unknown("Transaction not found"))
        val otherShares = splitDao.getShares(splitEntity.id).filter { it.id != shareId }

        when (share) {
            is ShareInput.Exact -> {
                val proposed = otherShares.map { it.shareAmountMinor } + share.amountMinor
                if (!splitCalculator.isWithinTotal(proposed, totalMinor)) {
                    return LedgerResult.Failure(LedgerError.Unknown("Shares would exceed the transaction total"))
                }
                splitDao.updateShareAmount(shareId, share.amountMinor, null)
            }
            is ShareInput.Percent -> {
                val proposed = otherShares.mapNotNull { it.percentage } + share.percentage
                if (!splitCalculator.isWithinTotalPercentage(proposed)) {
                    return LedgerResult.Failure(LedgerError.Unknown("Percentages would exceed 100%"))
                }
                splitDao.updateShareAmount(shareId, splitCalculator.percentageShare(totalMinor, share.percentage), share.percentage)
            }
            ShareInput.Auto -> return LedgerResult.Failure(LedgerError.Unknown("Auto is only valid for EQUAL splits"))
        }
        splitDao.touch(splitEntity.id, System.currentTimeMillis())
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerLogger.e("RoomSplitRepository: updateShare failed", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }
    }

    override suspend fun markSettled(shareId: String, settled: Boolean): LedgerResult<Unit> = try {
        splitDao.updateSettled(shareId, settled, if (settled) System.currentTimeMillis() else null)
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerLogger.e("RoomSplitRepository: markSettled failed", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override suspend fun deleteSplit(splitId: String): LedgerResult<Unit> = try {
        splitDao.deleteSplit(splitId) // cascades to split_shares via FK ON DELETE CASCADE
        LedgerResult.Success(Unit)
    } catch (e: Exception) {
        LedgerLogger.e("RoomSplitRepository: deleteSplit failed", e)
        LedgerResult.Failure(LedgerError.DatabaseFailure)
    }

    override fun observeOutstandingBalance(participantId: String): Flow<LedgerResult<Long>> =
        splitDao.observeOutstandingForParticipant(participantId)
            .map { shares -> LedgerResult.Success(shares.sumOf { it.shareAmountMinor }) as LedgerResult<Long> }
            .catch { e -> emit(LedgerResult.Failure(LedgerError.Unknown(e.message ?: "Database error"))) }

    private suspend fun totalMinorFor(transactionId: Long): Long? {
        val result = transactionRepository.getTransactionById(transactionId)
        return (result as? LedgerResult.Success)?.data?.amount?.minorUnits
    }

    /** Pure computation, split id/foreign key filled in by the caller — kept
     *  separate so createSplit's validation can run before anything is written. */
    private fun buildShareEntities(
        splitId: String,
        participantShares: Map<String, ShareInput>,
        splitType: SplitType,
        totalMinor: Long,
    ): List<SplitShareEntity> = when (splitType) {
        SplitType.EQUAL -> {
            val perPerson = splitCalculator.equalShare(totalMinor, participantShares.size + 1) // +1 self
            participantShares.keys.map { pid -> SplitShareEntity(id = "", splitId = splitId, participantId = pid, shareAmountMinor = perPerson, percentage = null) }
        }
        SplitType.EXACT -> {
            val amounts = participantShares.values.map {
                (it as? ShareInput.Exact)?.amountMinor ?: throw IllegalArgumentException("EXACT split requires an amount for every participant")
            }
            require(splitCalculator.isWithinTotal(amounts, totalMinor)) { "Shares would exceed the transaction total" }
            participantShares.entries.map { (pid, input) ->
                SplitShareEntity(id = "", splitId = splitId, participantId = pid, shareAmountMinor = (input as ShareInput.Exact).amountMinor, percentage = null)
            }
        }
        SplitType.PERCENTAGE -> {
            val percentages = participantShares.values.map {
                (it as? ShareInput.Percent)?.percentage ?: throw IllegalArgumentException("PERCENTAGE split requires a percentage for every participant")
            }
            require(splitCalculator.isWithinTotalPercentage(percentages)) { "Percentages would exceed 100%" }
            participantShares.entries.map { (pid, input) ->
                val pct = (input as ShareInput.Percent).percentage
                SplitShareEntity(id = "", splitId = splitId, participantId = pid, shareAmountMinor = splitCalculator.percentageShare(totalMinor, pct), percentage = pct)
            }
        }
    }
}

