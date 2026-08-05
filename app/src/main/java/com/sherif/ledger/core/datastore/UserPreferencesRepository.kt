package com.sherif.ledger.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.sherif.ledger.core.designsystem.theme.LedgerThemeType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val smsImportedKey = booleanPreferencesKey("sms_imported")
    private val lastSmsImportDateKey = longPreferencesKey("last_sms_import_date")
    private val themeTypeKey = stringPreferencesKey("theme_type")
    // Liquid Glass — an optional surface style layered on top of the base
    // light/dark theme. Off by default; the solid surfaces are the default look.
    private val liquidGlassKey = booleanPreferencesKey("liquid_glass_enabled")

    // Local-only profile (name/email) shown in Profile and the Dashboard
    // avatar — no auth, no server; collected once via ProfileSetupScreen on
    // very first launch, before anything else. Real sign-in/cloud sync would
    // replace this, not extend it — deliberately not built until that's decided.
    private val userNameKey = stringPreferencesKey("user_name")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val hasCompletedProfileSetupKey = booleanPreferencesKey("has_completed_profile_setup")

    // Notification Listener access (Settings > Notification access) is a
    // heavyweight, unfamiliar system flow — not a normal runtime permission
    // dialog — and SMS-only capture (SmsImporter/SmsReceiver) works standalone
    // without it. Real user testing confirmed this gate, mandatory and
    // un-skippable, was the single biggest onboarding blocker. This flag lets
    // the user explicitly defer it and proceed with SMS-only capture; they can
    // still grant it later from Profile for app-notification captures (Careem,
    // wallet apps) that never arrive as SMS.
    private val notificationAccessSkippedKey = booleanPreferencesKey("notification_access_skipped")

    // The user's chosen historical-import window (Part 2/3: onboarding range
    // selection) and a summary of what the last import run actually did with
    // it — persisted so the Developer Console can explain the import long
    // after it ran, not only in the moment.
    private val importWindowLabelKey = stringPreferencesKey("import_window_label")
    private val importWindowStartMillisKey = longPreferencesKey("import_window_start_millis")
    private val importWindowEndMillisKey = longPreferencesKey("import_window_end_millis")
    private val importSmsScannedKey = longPreferencesKey("import_sms_scanned")
    private val importSmsWithinWindowKey = longPreferencesKey("import_sms_within_window")
    private val importSmsIgnoredOutsideWindowKey = longPreferencesKey("import_sms_ignored_outside_window")
    private val importSmsMatchedKey = longPreferencesKey("import_sms_matched")
    private val importTransactionsCreatedKey = longPreferencesKey("import_transactions_created")
    private val importTransactionsMergedKey = longPreferencesKey("import_transactions_merged")
    private val importTransactionsDiscardedKey = longPreferencesKey("import_transactions_discarded")

    val isSmsImported: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[smsImportedKey] ?: false
        }

    val lastSmsImportDate: Flow<Long> = context.dataStore.data
        .map { it[lastSmsImportDateKey] ?: 0L }

    val themeType: Flow<LedgerThemeType> = context.dataStore.data
        .map { preferences ->
            val name = preferences[themeTypeKey] ?: LedgerThemeType.Dark.name
            try {
                LedgerThemeType.valueOf(name)
            } catch (e: Exception) {
                LedgerThemeType.Dark
            }
        }

    suspend fun setThemeType(themeType: LedgerThemeType) {
        context.dataStore.edit { preferences ->
            preferences[themeTypeKey] = themeType.name
        }
    }

    // Real backdrop-blur glass (see LedgerGlass.kt) — back to OFF by default.
    // Briefly defaulted on for the visual depth it gives (Apple Wallet-style
    // frosted material), but it's a continuous RenderEffect blur recomposited
    // behind the nav island on every scroll frame — genuinely GPU-costly, and
    // real user testing reported the app feeling "sluggish" / "not smooth"
    // immediately after this went live. Correctness > polish: available as an
    // opt-in in Settings for whoever wants it and has the hardware for it.
    val isLiquidGlassEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[liquidGlassKey] ?: false }

    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[liquidGlassKey] = enabled
        }
    }

    suspend fun setSmsImported(imported: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[smsImportedKey] = imported
        }
    }

    suspend fun setLastSmsImportDate(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[lastSmsImportDateKey] = timestamp
        }
    }

    val isProfileSetup: Flow<Boolean> = context.dataStore.data
        .map { it[hasCompletedProfileSetupKey] ?: false }

    val userName: Flow<String> = context.dataStore.data
        .map { it[userNameKey] ?: "" }

    val userEmail: Flow<String> = context.dataStore.data
        .map { it[userEmailKey] ?: "" }

    suspend fun setUserProfile(name: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[userNameKey] = name
            preferences[userEmailKey] = email
            preferences[hasCompletedProfileSetupKey] = true
        }
    }

    val isNotificationAccessSkipped: Flow<Boolean> = context.dataStore.data
        .map { it[notificationAccessSkippedKey] ?: false }

    suspend fun setNotificationAccessSkipped(skipped: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[notificationAccessSkippedKey] = skipped
        }
    }

    val importSummary: Flow<ImportSummary> = context.dataStore.data
        .map { preferences ->
            ImportSummary(
                windowLabel = preferences[importWindowLabelKey] ?: "",
                windowStartMillis = preferences[importWindowStartMillisKey] ?: 0L,
                windowEndMillis = preferences[importWindowEndMillisKey] ?: 0L,
                smsScanned = preferences[importSmsScannedKey] ?: 0L,
                smsWithinWindow = preferences[importSmsWithinWindowKey] ?: 0L,
                smsIgnoredOutsideWindow = preferences[importSmsIgnoredOutsideWindowKey] ?: 0L,
                smsMatched = preferences[importSmsMatchedKey] ?: 0L,
                transactionsCreated = preferences[importTransactionsCreatedKey] ?: 0L,
                transactionsMerged = preferences[importTransactionsMergedKey] ?: 0L,
                transactionsDiscarded = preferences[importTransactionsDiscardedKey] ?: 0L,
            )
        }

    suspend fun setImportSummary(summary: ImportSummary) {
        context.dataStore.edit { preferences ->
            preferences[importWindowLabelKey] = summary.windowLabel
            preferences[importWindowStartMillisKey] = summary.windowStartMillis
            preferences[importWindowEndMillisKey] = summary.windowEndMillis
            preferences[importSmsScannedKey] = summary.smsScanned
            preferences[importSmsWithinWindowKey] = summary.smsWithinWindow
            preferences[importSmsIgnoredOutsideWindowKey] = summary.smsIgnoredOutsideWindow
            preferences[importSmsMatchedKey] = summary.smsMatched
            preferences[importTransactionsCreatedKey] = summary.transactionsCreated
            preferences[importTransactionsMergedKey] = summary.transactionsMerged
            preferences[importTransactionsDiscardedKey] = summary.transactionsDiscarded
        }
    }
}

/** What the most recent historical SMS import actually did — see Developer Console diagnostics (Part 4). */
data class ImportSummary(
    val windowLabel: String,
    val windowStartMillis: Long,
    val windowEndMillis: Long,
    val smsScanned: Long,
    val smsWithinWindow: Long,
    val smsIgnoredOutsideWindow: Long,
    val smsMatched: Long,
    val transactionsCreated: Long,
    val transactionsMerged: Long,
    val transactionsDiscarded: Long,
)
