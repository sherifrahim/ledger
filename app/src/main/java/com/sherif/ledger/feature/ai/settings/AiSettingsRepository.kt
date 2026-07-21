package com.sherif.ledger.feature.ai.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sherif.ledger.feature.ai.domain.AICapability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_preferences")

/**
 * Non-secret AI settings — enable toggle, per-capability provider/model/
 * confidence-threshold/fallback-provider selection, per-provider base URL
 * overrides (for local providers), and global temperature/max-tokens.
 * Deliberately a SEPARATE DataStore file from UserPreferencesRepository's
 * `user_preferences` — the AI layer stays its own module, not entangled
 * with unrelated app preferences. API keys are NEVER stored here — see
 * SecureApiKeyStore.
 */
@Singleton
class AiSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val aiEnabledKey = booleanPreferencesKey("ai_enabled")
    private val temperatureKey = doublePreferencesKey("temperature")
    private val maxTokensKey = intPreferencesKey("max_tokens")

    /** Master switch — defaults OFF. No capability ever runs while this is false; see AIOrchestrator. */
    val isAiEnabled: Flow<Boolean> = context.aiDataStore.data.map { it[aiEnabledKey] ?: false }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.aiDataStore.edit { it[aiEnabledKey] = enabled }
    }

    fun providerForCapability(capability: AICapability): Flow<String?> =
        context.aiDataStore.data.map { it[providerKey(capability)] }

    suspend fun setProviderForCapability(capability: AICapability, providerId: String) {
        context.aiDataStore.edit { it[providerKey(capability)] = providerId }
    }

    /** RC6's "fallback strategy" — tried only after the primary provider exhausts its retries. Absent means no fallback configured. */
    fun fallbackProviderForCapability(capability: AICapability): Flow<String?> =
        context.aiDataStore.data.map { it[fallbackProviderKey(capability)] }

    suspend fun setFallbackProviderForCapability(capability: AICapability, providerId: String?) {
        context.aiDataStore.edit {
            if (providerId == null) it.remove(fallbackProviderKey(capability)) else it[fallbackProviderKey(capability)] = providerId
        }
    }

    fun modelForCapability(capability: AICapability): Flow<String?> =
        context.aiDataStore.data.map { it[modelKey(capability)] }

    suspend fun setModelForCapability(capability: AICapability, model: String) {
        context.aiDataStore.edit { it[modelKey(capability)] = model }
    }

    /** RC6's "Confidence System" — the deterministic engine's own confidence score for a decision must fall BELOW this before AI is even considered. 0-100; default 70. */
    fun confidenceThresholdForCapability(capability: AICapability): Flow<Int> =
        context.aiDataStore.data.map { it[confidenceThresholdKey(capability)] ?: DEFAULT_CONFIDENCE_THRESHOLD }

    suspend fun setConfidenceThresholdForCapability(capability: AICapability, threshold: Int) {
        context.aiDataStore.edit { it[confidenceThresholdKey(capability)] = threshold.coerceIn(0, 100) }
    }

    /** Only meaningful for Ollama/LM Studio, whose endpoint is a local address the user supplies — see LLMProvider.baseUrlConfigurable. */
    fun baseUrlForProvider(providerId: String): Flow<String?> =
        context.aiDataStore.data.map { it[baseUrlKey(providerId)] }

    suspend fun setBaseUrlForProvider(providerId: String, url: String) {
        context.aiDataStore.edit { it[baseUrlKey(providerId)] = url }
    }

    /** Global for now (not per-capability/provider) — deliberately simple until there's a real need for finer control. */
    val temperature: Flow<Double> = context.aiDataStore.data.map { it[temperatureKey] ?: DEFAULT_TEMPERATURE }

    suspend fun setTemperature(value: Double) {
        context.aiDataStore.edit { it[temperatureKey] = value.coerceIn(0.0, 2.0) }
    }

    val maxTokens: Flow<Int> = context.aiDataStore.data.map { it[maxTokensKey] ?: DEFAULT_MAX_TOKENS }

    suspend fun setMaxTokens(value: Int) {
        context.aiDataStore.edit { it[maxTokensKey] = value.coerceIn(16, 4096) }
    }

    private fun providerKey(capability: AICapability) = stringPreferencesKey("provider_${capability.name}")
    private fun fallbackProviderKey(capability: AICapability) = stringPreferencesKey("fallback_provider_${capability.name}")
    private fun modelKey(capability: AICapability) = stringPreferencesKey("model_${capability.name}")
    private fun confidenceThresholdKey(capability: AICapability) = intPreferencesKey("confidence_threshold_${capability.name}")
    private fun baseUrlKey(providerId: String) = stringPreferencesKey("base_url_$providerId")

    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 70
        const val DEFAULT_TEMPERATURE = 0.2
        const val DEFAULT_MAX_TOKENS = 512
    }
}
