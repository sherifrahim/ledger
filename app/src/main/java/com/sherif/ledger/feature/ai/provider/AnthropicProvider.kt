package com.sherif.ledger.feature.ai.provider

import com.sherif.ledger.feature.ai.domain.AICompletionResult
import com.sherif.ledger.feature.ai.domain.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class AnthropicMessage(val role: String, val content: String)

@Serializable
private data class AnthropicRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<AnthropicMessage>,
    val temperature: Double,
)

@Serializable
private data class AnthropicContentBlock(val type: String, val text: String? = null)

@Serializable
private data class AnthropicUsage(val input_tokens: Int? = null, val output_tokens: Int? = null)

@Serializable
private data class AnthropicResponse(
    val content: List<AnthropicContentBlock> = emptyList(),
    val usage: AnthropicUsage? = null,
)

/** Anthropic's Messages API — a distinct wire format from the OpenAI-compatible one (x-api-key header, versioned, content-block array). */
@Singleton
class AnthropicProvider @Inject constructor() : LLMProvider {

    override val id: String = "anthropic"
    override val displayName: String = "Anthropic"
    override val requiresApiKey: Boolean = true
    override val baseUrlConfigurable: Boolean = false
    override val defaultBaseUrl: String = "https://api.anthropic.com/v1"
    override val knownModels: List<String> = listOf(
        "claude-sonnet-4-5", "claude-opus-4-1", "claude-3-5-haiku-latest",
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun complete(
        apiKey: String?,
        baseUrl: String,
        model: String,
        prompt: String,
        maxTokens: Int,
        temperature: Double,
    ): AICompletionResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (apiKey.isNullOrBlank()) return@withContext AICompletionResult.Failure("No API key configured", 0)
        try {
            val body = json.encodeToString(
                AnthropicRequest.serializer(),
                AnthropicRequest(model = model, max_tokens = maxTokens, messages = listOf(AnthropicMessage("user", prompt)), temperature = temperature),
            )
            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                if (!response.isSuccessful) {
                    return@withContext AICompletionResult.Failure("HTTP ${response.code}", latency)
                }
                val text = response.body?.string().orEmpty()
                val parsed = json.decodeFromString(AnthropicResponse.serializer(), text)
                val content = parsed.content.firstOrNull { it.type == "text" }?.text
                    ?: return@withContext AICompletionResult.Failure("Empty response", latency)
                val tokens = (parsed.usage?.input_tokens ?: 0) + (parsed.usage?.output_tokens ?: 0)
                AICompletionResult.Success(content, tokens.takeIf { it > 0 }, latency)
            }
        } catch (e: IOException) {
            AICompletionResult.Failure("Network error: ${e.javaClass.simpleName}", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            AICompletionResult.Failure("Parse error: ${e.javaClass.simpleName}", System.currentTimeMillis() - start)
        }
    }
}
