package com.sherif.ledger.core.domain.usecase.intelligence

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.intelligence.CategoryIntelligenceEngine
import com.sherif.ledger.core.domain.service.intelligence.CategorySource
import com.sherif.ledger.feature.ai.settings.AiSettingsRepository
import com.sherif.ledger.feature.merchant.LearnedMerchantCategoryStore
import com.sherif.ledger.feature.merchant.MerchantCategory
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional AI augmentation of categorisation — the "smart handling" that only
 * happens when the user has turned AI on. Offline-first is preserved: this is a
 * NO-OP unless [AiSettingsRepository.isAiEnabled] is true (which it is not by
 * default), and even then it only touches expenses the deterministic tiers left
 * UNKNOWN — the ones that would otherwise sit in the Review Queue.
 *
 * For each such transaction it asks [CategoryIntelligenceEngine.resolveWithAiFallback]
 * (which is itself confidence-gated, and routes through AIOrchestrator's
 * enable/provider/key/validation/audit pipeline). A confident, validated AI
 * category is persisted as a [LearnedMerchantCategoryStore] override, so it then
 * flows through the SAME deterministic read path as everything else — no LLM call
 * ever happens on a hot read/balance path, and the frozen analytics read chain is
 * untouched. The user can always re-categorise from the Review Queue.
 */
@Singleton
class AiCategorizationSweepUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryIntelligenceEngine: CategoryIntelligenceEngine,
    private val learnedMerchantCategoryStore: LearnedMerchantCategoryStore,
    private val aiSettingsRepository: AiSettingsRepository,
) {
    data class Report(val aiEnabled: Boolean, val considered: Int, val categorized: Int) {
        fun summary() = "AI sweep: enabled=$aiEnabled considered=$considered categorized=$categorized"
    }

    suspend fun execute(): Report {
        if (!aiSettingsRepository.isAiEnabled.first()) return Report(aiEnabled = false, considered = 0, categorized = 0)

        val txns = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)?.data
            ?: return Report(aiEnabled = true, considered = 0, categorized = 0)

        var considered = 0
        var categorized = 0
        for (t in txns) {
            if (t.type != TransactionType.EXPENSE) continue
            val raw = t.rawText?.takeIf { it.isNotBlank() } ?: continue

            // Skip anything the deterministic tiers can already resolve — including
            // things already AI-categorised on a prior sweep (now LEARNED_MEMORY).
            val deterministic = categoryIntelligenceEngine.resolveDeterministic(raw)
            if (deterministic.source != CategorySource.UNKNOWN) continue

            considered++
            val resolved = categoryIntelligenceEngine.resolveWithAiFallback(
                rawMerchantText = raw,
                amountMinor = t.amount.minorUnits,
                currencyCode = t.amount.currencyCode,
                deterministic = deterministic,
            )
            if (resolved.source == CategorySource.AI_SUGGESTION) {
                val cat = runCatching { MerchantCategory.valueOf(resolved.category) }.getOrNull()
                if (cat != null && cat != MerchantCategory.UNKNOWN) {
                    learnedMerchantCategoryStore.learn(raw, cat)
                    categorized++
                }
            }
        }
        return Report(aiEnabled = true, considered = considered, categorized = categorized).also {
            if (it.considered > 0) LedgerLogger.d(it.summary())
        }
    }
}
