package com.sherif.ledger.core.di

import com.sherif.ledger.feature.ai.domain.LLMProvider
import com.sherif.ledger.feature.ai.provider.AnthropicProvider
import com.sherif.ledger.feature.ai.provider.GeminiProvider
import com.sherif.ledger.feature.ai.provider.OpenAiCompatibleProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/**
 * RC5 Part 5 — every provider Ledger knows about, bound into one
 * `Map<String, LLMProvider>` (Hilt multibinding, same pattern as
 * DiagnosticCollectorModule's `@IntoSet`). CapabilityRegistry looks providers
 * up by this map; nothing else in the app is aware providers exist as a
 * list. Adding an 8th provider is one more `@Provides @IntoMap` function
 * here — no other file changes.
 *
 * Model lists and base URLs reflect each vendor's public API as of this
 * file's authorship — verify/update before relying on them; none of this
 * has been exercised against a live endpoint in this environment (no network
 * testing capability, no API keys available while building it).
 */
@Module
@InstallIn(SingletonComponent::class)
object AiProviderModule {

    @Provides
    @IntoMap
    @StringKey("openai")
    fun provideOpenAi(): LLMProvider = OpenAiCompatibleProvider(
        id = "openai",
        displayName = "OpenAI",
        requiresApiKey = true,
        baseUrlConfigurable = false,
        defaultBaseUrl = "https://api.openai.com/v1",
        knownModels = listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1"),
    )

    @Provides
    @IntoMap
    @StringKey("groq")
    fun provideGroq(): LLMProvider = OpenAiCompatibleProvider(
        id = "groq",
        displayName = "Groq",
        requiresApiKey = true,
        baseUrlConfigurable = false,
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        knownModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
    )

    @Provides
    @IntoMap
    @StringKey("openrouter")
    fun provideOpenRouter(): LLMProvider = OpenAiCompatibleProvider(
        id = "openrouter",
        displayName = "OpenRouter",
        requiresApiKey = true,
        baseUrlConfigurable = false,
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        knownModels = listOf("openai/gpt-4o", "anthropic/claude-3.5-sonnet", "meta-llama/llama-3.3-70b-instruct"),
    )

    @Provides
    @IntoMap
    @StringKey("ollama")
    fun provideOllama(): LLMProvider = OpenAiCompatibleProvider(
        id = "ollama",
        displayName = "Ollama (Local)",
        requiresApiKey = false,
        baseUrlConfigurable = true,
        defaultBaseUrl = "http://localhost:11434/v1",
        knownModels = listOf("llama3.3", "mistral", "phi3"),
    )

    @Provides
    @IntoMap
    @StringKey("lmstudio")
    fun provideLmStudio(): LLMProvider = OpenAiCompatibleProvider(
        id = "lmstudio",
        displayName = "LM Studio (Local)",
        requiresApiKey = false,
        baseUrlConfigurable = true,
        defaultBaseUrl = "http://localhost:1234/v1",
        knownModels = emptyList(), // whatever model the user has loaded locally
    )

    @Provides
    @IntoMap
    @StringKey("anthropic")
    fun provideAnthropic(provider: AnthropicProvider): LLMProvider = provider

    @Provides
    @IntoMap
    @StringKey("gemini")
    fun provideGemini(provider: GeminiProvider): LLMProvider = provider
}
