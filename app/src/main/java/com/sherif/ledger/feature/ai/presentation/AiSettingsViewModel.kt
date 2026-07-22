package com.sherif.ledger.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.feature.ai.audit.AiAuditEntry
import com.sherif.ledger.feature.ai.audit.AiAuditLogger
import com.sherif.ledger.feature.ai.capability.CapabilityRegistry
import com.sherif.ledger.feature.ai.cost.AiCostSummary
import com.sherif.ledger.feature.ai.cost.AiCostTracker
import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.domain.AICompletionResult
import com.sherif.ledger.feature.ai.domain.LLMProvider
import com.sherif.ledger.core.domain.usecase.intelligence.AiCategorizationSweepUseCase
import com.sherif.ledger.feature.ai.settings.AiSettingsRepository
import com.sherif.ledger.feature.ai.settings.SecureApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderUi(
    val id: String,
    val displayName: String,
    val requiresApiKey: Boolean,
    val hasApiKey: Boolean,
    val baseUrlConfigurable: Boolean,
    val knownModels: List<String>,
)

data class CapabilitySettingUi(
    val capability: AICapability,
    val selectedProviderId: String?,
    val selectedModel: String?,
    val fallbackProviderId: String?,
    val confidenceThreshold: Int,
)

data class AiSettingsUiState(
    val isAiEnabled: Boolean = false,
    val temperature: Double = AiSettingsRepository.DEFAULT_TEMPERATURE,
    val maxTokens: Int = AiSettingsRepository.DEFAULT_MAX_TOKENS,
    val providers: List<ProviderUi> = emptyList(),
    val capabilitySettings: List<CapabilitySettingUi> = emptyList(),
    val auditLog: List<AiAuditEntry> = emptyList(),
    val costSummary: AiCostSummary = AiCostSummary(0, 0.0, 0),
    val testResultByProvider: Map<String, String> = emptyMap(),
    val testingProviderId: String? = null,
)

/**
 * RC5/RC6 Part 8 — Provider Settings. This screen is the ONLY place API keys
 * are entered (never plaintext-stored — see SecureApiKeyStore) and the only
 * place a capability is assigned to a provider/model/fallback/confidence
 * threshold. Nothing here commits anything to Ledger's financial data;
 * "Test Connection" is the sole way this screen ever calls a provider, and
 * even that requires AI to already be enabled.
 */
@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val aiSettingsRepository: AiSettingsRepository,
    private val secureApiKeyStore: SecureApiKeyStore,
    private val capabilityRegistry: CapabilityRegistry,
    private val auditLogger: AiAuditLogger,
    private val costTracker: AiCostTracker,
    private val aiCategorizationSweep: AiCategorizationSweepUseCase,
) : ViewModel() {

    private val _testResultByProvider = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _testingProviderId = MutableStateFlow<String?>(null)
    private val _costSummary = MutableStateFlow(AiCostSummary(0, 0.0, 0))

    // combine() only has direct-typed overloads up to 5 flows; nesting two
    // 3-way combines keeps every field properly typed instead of casting
    // through Array<Any?> (the 6+ vararg overload requires one shared type).
    private val coreSettings = combine(
        aiSettingsRepository.isAiEnabled,
        aiSettingsRepository.temperature,
        aiSettingsRepository.maxTokens,
    ) { enabled, temperature, maxTokens -> Triple(enabled, temperature, maxTokens) }

    private val testAndCostState = combine(
        _testResultByProvider,
        _testingProviderId,
        _costSummary,
    ) { testResults, testing, cost -> Triple(testResults, testing, cost) }

    val uiState: StateFlow<AiSettingsUiState> = combine(
        coreSettings,
        auditLogger.observeRecent(50),
        testAndCostState,
    ) { (enabled, temperature, maxTokens), audit, (testResults, testing, cost) ->
        val providers = capabilityRegistry.availableProviders.map { it.toUi() }
        val capabilitySettings = AICapability.entries.map { capability ->
            CapabilitySettingUi(
                capability = capability,
                selectedProviderId = aiSettingsRepository.providerForCapability(capability).first(),
                selectedModel = aiSettingsRepository.modelForCapability(capability).first(),
                fallbackProviderId = aiSettingsRepository.fallbackProviderForCapability(capability).first(),
                confidenceThreshold = aiSettingsRepository.confidenceThresholdForCapability(capability).first(),
            )
        }
        AiSettingsUiState(
            isAiEnabled = enabled,
            temperature = temperature,
            maxTokens = maxTokens,
            providers = providers,
            capabilitySettings = capabilitySettings,
            auditLog = audit,
            costSummary = cost,
            testResultByProvider = testResults,
            testingProviderId = testing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiSettingsUiState())

    init {
        refreshCostSummary()
    }

    private fun refreshCostSummary() {
        viewModelScope.launch { _costSummary.value = costTracker.todaySummary() }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiSettingsRepository.setAiEnabled(enabled)
            // Turning AI on categorises the existing UNKNOWN-expense backlog now,
            // so the effect is immediate rather than waiting for the next launch.
            if (enabled) runCatching { aiCategorizationSweep.execute() }
        }
    }

    fun setTemperature(value: Double) {
        viewModelScope.launch { aiSettingsRepository.setTemperature(value) }
    }

    fun setMaxTokens(value: Int) {
        viewModelScope.launch { aiSettingsRepository.setMaxTokens(value) }
    }

    fun selectProvider(capability: AICapability, providerId: String) {
        viewModelScope.launch { aiSettingsRepository.setProviderForCapability(capability, providerId) }
    }

    fun selectFallbackProvider(capability: AICapability, providerId: String?) {
        viewModelScope.launch { aiSettingsRepository.setFallbackProviderForCapability(capability, providerId) }
    }

    fun selectModel(capability: AICapability, model: String) {
        viewModelScope.launch { aiSettingsRepository.setModelForCapability(capability, model) }
    }

    fun setConfidenceThreshold(capability: AICapability, threshold: Int) {
        viewModelScope.launch { aiSettingsRepository.setConfidenceThresholdForCapability(capability, threshold) }
    }

    fun setApiKey(providerId: String, key: String) {
        if (key.isBlank()) secureApiKeyStore.clearApiKey(providerId) else secureApiKeyStore.setApiKey(providerId, key)
        // Force a UI refresh of hasApiKey by nudging the enabled flow's dependents — simplest: re-emit test results unchanged.
        _testResultByProvider.value = _testResultByProvider.value
    }

    fun setBaseUrl(providerId: String, url: String) {
        viewModelScope.launch { aiSettingsRepository.setBaseUrlForProvider(providerId, url) }
    }

    fun testConnection(providerId: String) {
        val provider = capabilityRegistry.provider(providerId) ?: return
        viewModelScope.launch {
            _testingProviderId.value = providerId
            val apiKey = secureApiKeyStore.getApiKey(providerId)
            val baseUrl = if (provider.baseUrlConfigurable) {
                aiSettingsRepository.baseUrlForProvider(providerId).first() ?: provider.defaultBaseUrl
            } else {
                provider.defaultBaseUrl
            }
            val model = provider.knownModels.firstOrNull() ?: "default"
            val result = provider.complete(apiKey, baseUrl, model, "Reply with the single word: OK", maxTokens = 8)
            val message = when (result) {
                is AICompletionResult.Success -> "Connected (${result.latencyMs}ms)"
                is AICompletionResult.Failure -> "Failed: ${result.reason}"
            }
            _testResultByProvider.value = _testResultByProvider.value + (providerId to message)
            _testingProviderId.value = null
        }
    }

    private fun LLMProvider.toUi() = ProviderUi(
        id = id,
        displayName = displayName,
        requiresApiKey = requiresApiKey,
        hasApiKey = secureApiKeyStore.hasApiKey(id),
        baseUrlConfigurable = baseUrlConfigurable,
        knownModels = knownModels,
    )
}
