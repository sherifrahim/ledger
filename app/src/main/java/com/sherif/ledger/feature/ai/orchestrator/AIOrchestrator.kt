package com.sherif.ledger.feature.ai.orchestrator

import com.sherif.ledger.feature.ai.audit.AiAuditLogger
import com.sherif.ledger.feature.ai.audit.AiDebugTrace
import com.sherif.ledger.feature.ai.audit.AiDebugTraceStore
import com.sherif.ledger.feature.ai.audit.CacheStatus
import com.sherif.ledger.feature.ai.cache.AiSuggestionCache
import com.sherif.ledger.feature.ai.capability.CapabilityRegistry
import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.domain.AICompletionResult
import com.sherif.ledger.feature.ai.domain.AIContext
import com.sherif.ledger.feature.ai.domain.AISuggestion
import com.sherif.ledger.feature.ai.domain.LLMProvider
import com.sherif.ledger.feature.ai.prompt.PromptLibrary
import com.sherif.ledger.feature.ai.settings.AiSettingsRepository
import com.sherif.ledger.feature.ai.settings.SecureApiKeyStore
import com.sherif.ledger.feature.ai.validation.AISuggestionValidator
import com.sherif.ledger.feature.ai.validation.AIValidationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Every outcome AIOrchestrator can produce — the deterministic engine (or a settings screen) branches on this, never on a raw exception. */
sealed interface AIOrchestratorResult {
    /** The master "Enable AI" switch is off — see AiSettingsRepository.isAiEnabled. */
    data object AiDisabled : AIOrchestratorResult

    /** AI is on, but no provider is assigned to this capability yet (Settings hasn't been configured for it). */
    data object NoProviderConfigured : AIOrchestratorResult

    /** A provider is configured but has no API key stored (irrelevant for Ollama/LM Studio). */
    data object MissingApiKey : AIOrchestratorResult

    data class Suggested(val suggestion: AISuggestion, val providerId: String, val model: String) : AIOrchestratorResult

    data class Failed(val reason: String) : AIOrchestratorResult
}

private data class AttemptOutcome(val providerId: String, val model: String, val result: AICompletionResult)

/**
 * RC5/RC6 — the ONE place an AI request flows through, per the spec's own
 * pipeline: Notification → Parser → Merchant Engine → Relationship Engine →
 * AI Orchestrator → Database. Deliberately NOT wired into that pipeline yet
 * (`ProcessNotificationUseCase` is untouched) — this builds the
 * infrastructure only; Phase C (a later, separate task) is what would call
 * this from the live capture path, and only after a commit step accepts a
 * validated suggestion, never this class directly.
 *
 * RC6 pipeline, in order, every request: cache check → primary provider
 * (with retry) → fallback provider (with retry) → parse → validate → cache
 * the validated result → audit log → debug trace. [requestSuggestion] never
 * throws to its caller and never touches TransactionRepository/
 * AccountRepository — it returns an opinion or a reason it couldn't produce
 * one. The deterministic engine always decides what happens next.
 */
@Singleton
class AIOrchestrator @Inject constructor(
    private val capabilityRegistry: CapabilityRegistry,
    private val aiSettingsRepository: AiSettingsRepository,
    private val secureApiKeyStore: SecureApiKeyStore,
    private val auditLogger: AiAuditLogger,
    private val validator: AISuggestionValidator,
    private val cache: AiSuggestionCache,
    private val debugTraceStore: AiDebugTraceStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun requestSuggestion(capability: AICapability, context: AIContext): AIOrchestratorResult {
        if (!aiSettingsRepository.isAiEnabled.first()) return AIOrchestratorResult.AiDisabled

        val prompt = PromptLibrary.promptFor(capability, context)

        cache.get(capability, context, CACHE_TTL_MILLIS)?.let { cached ->
            recordDebugTrace(capability, "cache", "cached", context, prompt, null, "Valid (cached)", 0, CacheStatus.HIT, null)
            return AIOrchestratorResult.Suggested(cached, providerId = "cache", model = "cached")
        }

        val provider = capabilityRegistry.providerFor(capability) ?: return AIOrchestratorResult.NoProviderConfigured

        var outcome = attemptWithRetry(provider, capability, prompt)
        if (outcome.result is AICompletionResult.Failure) {
            val fallbackId = aiSettingsRepository.fallbackProviderForCapability(capability).first()
            val fallbackProvider = fallbackId?.let { capabilityRegistry.provider(it) }
            if (fallbackProvider != null) {
                outcome = attemptWithRetry(fallbackProvider, capability, prompt)
            }
        }

        return finalize(capability, context, prompt, outcome)
    }

    private suspend fun attemptWithRetry(provider: LLMProvider, capability: AICapability, prompt: String): AttemptOutcome {
        val model = aiSettingsRepository.modelForCapability(capability).first() ?: provider.knownModels.firstOrNull() ?: "default"
        val apiKey = secureApiKeyStore.getApiKey(provider.id)
        if (provider.requiresApiKey && apiKey.isNullOrBlank()) {
            return AttemptOutcome(provider.id, model, AICompletionResult.Failure("No API key configured", 0))
        }
        val baseUrl = if (provider.baseUrlConfigurable) {
            aiSettingsRepository.baseUrlForProvider(provider.id).first() ?: provider.defaultBaseUrl
        } else {
            provider.defaultBaseUrl
        }
        val temperature = aiSettingsRepository.temperature.first()
        val maxTokens = aiSettingsRepository.maxTokens.first()

        var last: AICompletionResult = AICompletionResult.Failure("Not attempted", 0)
        for (attempt in 1..MAX_ATTEMPTS) {
            val result = provider.complete(apiKey, baseUrl, model, prompt, maxTokens, temperature)
            if (result is AICompletionResult.Success) return AttemptOutcome(provider.id, model, result)
            last = result
            if (attempt < MAX_ATTEMPTS) delay(RETRY_DELAY_MS)
        }
        return AttemptOutcome(provider.id, model, last)
    }

    private suspend fun finalize(capability: AICapability, context: AIContext, prompt: String, outcome: AttemptOutcome): AIOrchestratorResult {
        return when (val result = outcome.result) {
            is AICompletionResult.Success -> {
                val suggestion = parseSuggestion(result.rawContent)
                if (suggestion == null) {
                    recordOutcome(capability, outcome, success = false, confidence = null, error = "Response did not match the expected JSON shape")
                    recordDebugTrace(capability, outcome.providerId, outcome.model, context, prompt, result.rawContent, "N/A — unparseable", result.latencyMs, CacheStatus.MISS, "Unparseable response")
                    return AIOrchestratorResult.Failed("Response did not match the expected JSON shape")
                }
                when (val validation = validator.validate(capability, suggestion)) {
                    is AIValidationResult.Invalid -> {
                        recordOutcome(capability, outcome, success = false, confidence = suggestion.confidencePercent, error = "Validation failed: ${validation.reason}")
                        recordDebugTrace(capability, outcome.providerId, outcome.model, context, prompt, result.rawContent, "Invalid: ${validation.reason}", result.latencyMs, CacheStatus.MISS, "Validation failed")
                        AIOrchestratorResult.Failed("Validation failed: ${validation.reason}")
                    }
                    AIValidationResult.Valid -> {
                        cache.put(capability, context, suggestion)
                        recordOutcome(capability, outcome, success = true, confidence = suggestion.confidencePercent, error = null)
                        recordDebugTrace(capability, outcome.providerId, outcome.model, context, prompt, result.rawContent, "Valid", result.latencyMs, CacheStatus.MISS, null)
                        AIOrchestratorResult.Suggested(suggestion, outcome.providerId, outcome.model)
                    }
                }
            }
            is AICompletionResult.Failure -> {
                recordOutcome(capability, outcome, success = false, confidence = null, error = result.reason)
                recordDebugTrace(capability, outcome.providerId, outcome.model, context, prompt, null, "N/A — request failed", result.latencyMs, CacheStatus.MISS, result.reason)
                AIOrchestratorResult.Failed(result.reason)
            }
        }
    }

    private suspend fun recordOutcome(capability: AICapability, outcome: AttemptOutcome, success: Boolean, confidence: Int?, error: String?) {
        auditLogger.record(
            capability = capability,
            providerId = outcome.providerId,
            model = outcome.model,
            latencyMs = (outcome.result as? AICompletionResult.Success)?.latencyMs ?: (outcome.result as? AICompletionResult.Failure)?.latencyMs ?: 0,
            tokensUsed = (outcome.result as? AICompletionResult.Success)?.tokensUsed,
            success = success,
            confidencePercent = confidence,
            errorSummary = error,
        )
    }

    private fun recordDebugTrace(
        capability: AICapability,
        providerId: String,
        model: String,
        context: AIContext,
        prompt: String,
        rawResponse: String?,
        validationResult: String,
        latencyMs: Long,
        cacheStatus: CacheStatus,
        error: String?,
    ) {
        debugTraceStore.record(
            AiDebugTrace(
                timestampMillis = System.currentTimeMillis(),
                capability = capability,
                providerId = providerId,
                model = model,
                renderedContext = context.toString(),
                prompt = prompt,
                rawResponse = rawResponse,
                validationResult = validationResult,
                executionTimeMs = latencyMs,
                cacheStatus = cacheStatus,
                error = error,
            ),
        )
    }

    /** Null on ANY parse failure — a malformed response is a failure, never a partially-trusted guess. */
    private fun parseSuggestion(rawContent: String): AISuggestion? = runCatching {
        // Some providers wrap JSON in a markdown code fence despite instructions; strip it defensively.
        val cleaned = rawContent.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        json.decodeFromString(AISuggestion.serializer(), cleaned)
    }.getOrNull()

    companion object {
        private const val MAX_ATTEMPTS = 2
        private val RETRY_DELAY_MS = TimeUnit.SECONDS.toMillis(1)
        private val CACHE_TTL_MILLIS = TimeUnit.HOURS.toMillis(24)
    }
}
