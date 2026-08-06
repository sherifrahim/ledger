package com.sherif.ledger.feature.update

import com.sherif.ledger.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class GithubAsset(val name: String, val browser_download_url: String)

@Serializable
private data class GithubRelease(val tag_name: String, val assets: List<GithubAsset> = emptyList())

/**
 * Checks GitHub Releases (published by .github/workflows/release.yml on every
 * push to main) for a build newer than the one running. The repo is public,
 * so this is an unauthenticated GET — no token, no meaningful rate-limit risk
 * at this app's scale.
 *
 * Silent by design: any failure (offline, GitHub unreachable, malformed
 * release) just resolves to "no update found". This also runs automatically
 * on every launch, so it must never surface as an error to a user who didn't
 * ask for one — only an explicit "Check for Updates" tap gets a shown result.
 */
@Singleton
class CheckForUpdateUseCase @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // release.yml uploads app-debug.apk / app-release.apk by name; match the
    // asset for the build type actually running so a debug install only ever
    // offers a debug update, same for release.
    private val assetName = if (BuildConfig.DEBUG) "app-debug.apk" else "app-release.apk"

    suspend fun execute(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/sherifrahim/ledger/releases/latest")
                .addHeader("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val release = json.decodeFromString(GithubRelease.serializer(), body)
                val remoteVersionCode = Regex("""build-(\d+)""").find(release.tag_name)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: return@withContext null
                if (remoteVersionCode <= BuildConfig.VERSION_CODE) return@withContext null
                val asset = release.assets.firstOrNull { it.name == assetName } ?: return@withContext null
                UpdateInfo(
                    versionCode = remoteVersionCode,
                    versionName = "1.0.$remoteVersionCode",
                    downloadUrl = asset.browser_download_url,
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
