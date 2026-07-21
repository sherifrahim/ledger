package com.sherif.ledger.feature.ai.confidence

import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.settings.AiSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RC6's "Confidence System": the deterministic engine always computes a
 * confidence score for its own decision first (this class doesn't compute
 * that score — merchant/category/duplicate/relationship engines already do,
 * e.g. `HeuristicExtractor`'s intent score, `MerchantResolver`'s match
 * confidence). This class is the ONE place that decides whether a score is
 * low enough to justify spending an AI call on it, per capability, per the
 * user's own configured threshold (`AiSettingsRepository`, default 70).
 *
 * Not wired into the live capture pipeline yet — Phase C. Exists now so
 * that wiring, when it happens, is "call `shouldConsultAi`, then call
 * `AIOrchestrator` if true" rather than a new ad-hoc threshold check
 * invented at the call site.
 */
@Singleton
class ConfidenceGate @Inject constructor(
    private val aiSettingsRepository: AiSettingsRepository,
) {
    /** True when the deterministic engine's own [confidencePercent] (0-100) falls below the user's configured threshold for this capability. */
    suspend fun shouldConsultAi(capability: AICapability, confidencePercent: Int): Boolean {
        val threshold = aiSettingsRepository.confidenceThresholdForCapability(capability).first()
        return confidencePercent < threshold
    }
}
