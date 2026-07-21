package com.sherif.ledger.feature.ai.domain

/** One completed call to a provider — the raw text response plus what the audit log/cost tracker need, before any suggestion parsing. */
sealed interface AICompletionResult {
    data class Success(
        val rawContent: String,
        val tokensUsed: Int?,
        val latencyMs: Long,
    ) : AICompletionResult

    data class Failure(
        val reason: String,
        val latencyMs: Long,
    ) : AICompletionResult
}

/**
 * RC5 Part 5 — every AI vendor implements this SAME interface; nothing
 * outside `feature/ai/provider` ever references a provider SDK or a
 * vendor-specific request/response shape directly. Adding a new provider is
 * one new class implementing this interface plus one new
 * `@Provides @IntoMap` binding (see AiProviderModule) — no other file
 * changes.
 */
interface LLMProvider {
    /** Stable identifier, used as the DI multibinding key and persisted in settings/audit log — never displayed raw to the user. */
    val id: String
    val displayName: String

    /** False only for local providers (Ollama, LM Studio) — nothing else in the app treats "no key" as a special case. */
    val requiresApiKey: Boolean

    /** True for providers whose endpoint is a user-supplied address (local network), not a fixed hosted URL. */
    val baseUrlConfigurable: Boolean
    val defaultBaseUrl: String

    /** Known models as of this file's authorship — not fetched live from any provider; the AI Settings screen lets a user type a different one for local providers. */
    val knownModels: List<String>

    suspend fun complete(
        apiKey: String?,
        baseUrl: String,
        model: String,
        prompt: String,
        maxTokens: Int,
        temperature: Double = 0.2,
    ): AICompletionResult
}
