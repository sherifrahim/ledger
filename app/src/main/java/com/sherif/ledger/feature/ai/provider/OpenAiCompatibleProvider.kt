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

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int,
    val temperature: Double,
)

@Serializable
private data class ChatCompletionChoice(val message: ChatMessage)

@Serializable
private data class ChatCompletionUsage(val total_tokens: Int? = null)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<ChatCompletionChoice> = emptyList(),
    val usage: ChatCompletionUsage? = null,
)

/**
 * One real implementation of the "OpenAI-compatible chat completions" wire
 * format — used by OpenAI itself, Groq, OpenRouter, Ollama (its `/v1`
 * compatibility endpoint), and LM Studio's local server. These five
 * genuinely share one request/response shape; this class is the single
 * implementation, parameterized per provider (see AiProviderModule) rather
 * than five near-duplicate classes.
 *
 * NEVER logs [apiKey] or includes it in any exception message — failures
 * are reported by HTTP status/short reason only (see [complete]'s catch
 * blocks).
 */
class OpenAiCompatibleProvider(
    override val id: String,
    override val displayName: String,
    override val requiresApiKey: Boolean,
    override val baseUrlConfigurable: Boolean,
    override val defaultBaseUrl: String,
    override val knownModels: List<String>,
) : LLMProvider {

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
        try {
            val body = json.encodeToString(
                ChatCompletionRequest.serializer(),
                ChatCompletionRequest(model = model, messages = listOf(ChatMessage("user", prompt)), max_tokens = maxTokens, temperature = temperature),
            )
            val requestBuilder = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/chat/completions")
                .post(body.toRequestBody("application/json".toMediaType()))
            if (requiresApiKey && !apiKey.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            client.newCall(requestBuilder.build()).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                if (!response.isSuccessful) {
                    return@withContext AICompletionResult.Failure("HTTP ${response.code}", latency)
                }
                val text = response.body?.string().orEmpty()
                val parsed = json.decodeFromString(ChatCompletionResponse.serializer(), text)
                val content = parsed.choices.firstOrNull()?.message?.content
                    ?: return@withContext AICompletionResult.Failure("Empty response", latency)
                AICompletionResult.Success(content, parsed.usage?.total_tokens, latency)
            }
        } catch (e: IOException) {
            AICompletionResult.Failure("Network error: ${e.javaClass.simpleName}", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            AICompletionResult.Failure("Parse error: ${e.javaClass.simpleName}", System.currentTimeMillis() - start)
        }
    }
}
