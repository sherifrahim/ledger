package com.sherif.ledger.core.domain.service.diagnostic

import android.content.Context
import android.os.Build
import androidx.room.Room
import com.sherif.ledger.BuildConfig
import com.sherif.ledger.core.database.LedgerDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject

@Serializable
data class AppInfoDto(
    val versionName: String,
    val versionCode: Int,
    val applicationId: String,
    val buildType: String,
    val isDebugBuild: Boolean,
    val androidVersion: String,
    val androidSdkInt: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
    val databaseVersion: Int,
    val gitHash: String,
    val generatedAtEpochMillis: Long,
)

/**
 * Snapshot of build/device facts for the diagnostic bundle. Includes the short
 * git commit (BuildConfig.GIT_HASH, injected by a buildConfigField in
 * build.gradle.kts) so a user's report can be tied to an exact build.
 */
class AppInfoCollector @Inject constructor(
    @ApplicationContext private val context: Context,
) : DiagnosticCollector {

    override val id: String = "app_info"

    override suspend fun collect(): DiagnosticSection {
        val dto = AppInfoDto(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            applicationId = BuildConfig.APPLICATION_ID,
            buildType = BuildConfig.BUILD_TYPE,
            isDebugBuild = BuildConfig.DEBUG,
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            androidSdkInt = Build.VERSION.SDK_INT,
            deviceManufacturer = Build.MANUFACTURER ?: "unknown",
            deviceModel = Build.MODEL ?: "unknown",
            databaseVersion = LedgerDatabase.DATABASE_VERSION,
            gitHash = BuildConfig.GIT_HASH,
            generatedAtEpochMillis = Instant.now().toEpochMilli(),
        )
        val json = Json { prettyPrint = true }
        return DiagnosticSection.Json(id, json.encodeToString(dto))
    }
}



