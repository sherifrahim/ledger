package com.sherif.ledger.feature.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads an update APK to cacheDir/updates and launches the system package
 * installer on it. Reuses the FileProvider authority
 * [com.sherif.ledger.core.domain.service.diagnostic.DiagnosticBundleGenerator]
 * already declares for sharing the diagnostic zip — one provider, two cache
 * subfolders (see res/xml/file_paths.xml) — rather than a second provider
 * declaration for the same purpose.
 */
@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Returns true once the system installer has been launched; false on any download failure. */
    suspend fun downloadAndInstall(update: UpdateInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(dir, "ledger-${update.versionCode}.apk")

            val request = Request.Builder().url(update.downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                apkFile.outputStream().use { out -> body.byteStream().copyTo(out) }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context, "${context.packageName}.diagnosticfileprovider", apkFile,
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) { context.startActivity(installIntent) }
            true
        } catch (e: Exception) {
            false
        }
    }
}
