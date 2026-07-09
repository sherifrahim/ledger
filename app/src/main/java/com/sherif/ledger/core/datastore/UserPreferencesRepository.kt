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
            val name = preferences[themeTypeKey] ?: LedgerThemeType.Classic.name
            try {
                LedgerThemeType.valueOf(name)
            } catch (e: Exception) {
                LedgerThemeType.Classic
            }
        }

    suspend fun setThemeType(themeType: LedgerThemeType) {
        context.dataStore.edit { preferences ->
            preferences[themeTypeKey] = themeType.name
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
}
