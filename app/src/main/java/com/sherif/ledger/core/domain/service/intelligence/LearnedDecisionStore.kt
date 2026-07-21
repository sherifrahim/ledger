package com.sherif.ledger.core.domain.service.intelligence

import com.sherif.ledger.core.database.dao.LearnedDecisionDao
import com.sherif.ledger.core.database.entity.LearnedDecisionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Stable decision-type keys — new decision types are a new constant here, never a new table. */
object DecisionType {
    /** subjectKey = the raw institution identifier (NotificationEnvelope.packageName/SMS sender); learnedValue = the confirmed institution name a user promoted a Candidate Account to. */
    const val INSTITUTION = "institution"
}

/**
 * RC8 Phase B — "Ledger must learn": generic deterministic memory for any
 * user-confirmed decision. When a user confirms something (today: promoting
 * a Candidate Account to a real institution — see
 * [com.sherif.ledger.core.domain.usecase.account.PromoteCandidateAccountUseCase]),
 * that decision is stored here so a FUTURE occurrence of the same evidence
 * prefers this learned answer before ever falling back to a fresh guess or
 * consulting AI. AI is never consulted for something already in this store.
 *
 * Same in-memory-cache-over-Room pattern as the existing, working
 * [com.sherif.ledger.feature.merchant.LearnedMerchantCategoryStore] — kept
 * deliberately separate rather than merged into it, since that store is
 * specifically merchant-category-shaped and already wired into a real UI
 * flow (Review Inbox); this one is intentionally decision-type-agnostic.
 */
@Singleton
class LearnedDecisionStore @Inject constructor(
    private val dao: LearnedDecisionDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cache: Map<Pair<String, String>, LearnedDecisionEntity> = emptyMap()

    init {
        scope.launch { reload() }
    }

    private suspend fun reload() {
        cache = dao.getAll().associateBy { it.decisionType to it.subjectKey }
    }

    /** Null when nothing has been learned yet for this (decisionType, subjectKey) — a real gap, never guessed around. */
    fun valueFor(decisionType: String, subjectKey: String): String? =
        cache[decisionType to subjectKey]?.learnedValue

    suspend fun learn(decisionType: String, subjectKey: String, learnedValue: String, confidence: Int = 100) {
        val entity = LearnedDecisionEntity(decisionType, subjectKey, learnedValue, confidence, System.currentTimeMillis())
        dao.upsert(entity)
        cache = cache + ((decisionType to subjectKey) to entity)
    }

    /** For the Intelligence Inspector — every learned decision, most recent first. */
    fun all(): List<LearnedDecisionEntity> = cache.values.sortedByDescending { it.updatedAt }
}
