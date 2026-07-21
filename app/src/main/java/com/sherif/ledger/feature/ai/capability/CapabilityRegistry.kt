package com.sherif.ledger.feature.ai.capability

import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.domain.LLMProvider
import com.sherif.ledger.feature.ai.settings.AiSettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RC5 Part 6 — routes a capability request to whichever provider the user
 * assigned it to (Settings can assign Merchant Classification -> Groq,
 * Insights -> Claude, Search -> Gemini independently; see
 * AiSettingsRepository.providerForCapability). This is the ONLY place that
 * resolves "which provider" — AIOrchestrator never branches on a provider
 * id itself.
 */
@Singleton
class CapabilityRegistry @Inject constructor(
    private val providers: Map<String, @JvmSuppressWildcards LLMProvider>,
    private val settingsRepository: AiSettingsRepository,
) {
    val availableProviders: List<LLMProvider> get() = providers.values.sortedBy { it.displayName }

    fun provider(id: String): LLMProvider? = providers[id]

    suspend fun providerFor(capability: AICapability): LLMProvider? {
        val id = settingsRepository.providerForCapability(capability).first() ?: return null
        return providers[id]
    }
}
