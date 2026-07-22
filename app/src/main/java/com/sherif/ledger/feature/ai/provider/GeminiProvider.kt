package com.sherif.ledger.feature.ai.provider

import com.sherif.ledger.feature.ai.domain.AICompletionResult
import com.sherif.ledger.feature.ai.domain.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class GeminiPart(val text: String)

@Serializable
private data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
private data class GeminiGenerationConfig(val maxOutputTokens: Int, val temperature: Double)

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig,
)

@Serializable
private data class GeminiCandidate(val content: GeminiContent? = null)

@Serializable
private data class GeminiUsageMetadata(val totalTokenCount: Int? = null)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val usageMetadata: GeminiUsageMetadata? = null,
)

@Serializable
private data class GeminiError(val error: GeminiErrorBody? = null)

@Serializable
private data class GeminiErrorBody(val code: Int? = null, val message: String? = null, val status: String? = null)

/** Google's Generative Language API — API key travels as a query parameter, not a header; response is a candidates/content/parts tree. */
@Singleton
class GeminiProvider @Inject constructor() : LLMProvider {

    override val id: String = "gemini"
    override val displayName: String = "Google Gemini"
    override val requiresApiKey: Boolean = true
    override val baseUrlConfigurable: Boolean = false
    override val defaultBaseUrl: String = "https://generativelanguage.googleapis.com/v1beta"
    override val knownModels: List<String> = listOf("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash")

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
                GeminiRequest.serializer(),
                GeminiRequest(
                    contents = listOf(GeminiContent(listOf(GeminiPart(prompt)))),
                    generationConfig = GeminiGenerationConfig(maxTokens, temperature),
                ),
            )
            val url = (baseUrl.trimEnd('/') + "/models/$model:generateContent").toHttpUrl()
                .newBuilder()
                .addQueryParameter("key", apiKey)
                .build()
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                if (!response.isSuccessful) {
                    // Surface Google's own error message (quota/billing/rate-limit
                    // reason) rather than a bare HTTP code — the body is where the
                    // actual "why" lives (e.g. RESOURCE_EXHAUSTED, retry-after).
                    val detail = runCatching {
                        val err = json.decodeFromString(GeminiError.serializer(), response.body?.string().orEmpty())
                        err.error?.message ?: err.error?.status
                    }.getOrNull()
                    val reason = if (detail.isNullOrBlank()) "HTTP ${response.code}" else "HTTP ${response.code}: ${detail.take(180)}"
                    return@withContext AICompletionResult.Failure(reason, latency)
                }
                val text = response.body?.string().orEmpty()
                val parsed = json.decodeFromString(GeminiResponse.serializer(), text)
                val content = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: return@withContext AICompletionResult.Failure("Empty response", latency)
                AICompletionResult.Success(content, parsed.usageMetadata?.totalTokenCount, latency)
            }
        } catch (e: IOException) {
            AICompletionResult.Failure("Network error: ${e.javaClass.simpleName}", System.currentTimeMillis() - start)
        } catch (e: Exception) {
            AICompletionResult.Failure("Parse error: ${e.javaClass.simpleName}", System.currentTimeMillis() - start)
        }
    }
}
