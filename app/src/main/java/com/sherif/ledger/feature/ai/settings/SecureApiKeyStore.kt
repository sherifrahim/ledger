package com.sherif.ledger.feature.ai.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RC5 Part 9 — Secure API Storage. Uses `androidx.security.crypto`'s
 * `EncryptedSharedPreferences`, backed by a Keystore-generated
 * `MasterKey` (AES256-GCM) — the Google-recommended way to meet "Android
 * Keystore with encrypted local storage" without hand-rolling Keystore
 * `Cipher` calls, which are easy to get subtly wrong.
 *
 * Hard rules, enforced by never doing the alternative anywhere in this
 * class or its callers:
 * - Never logged (no `LedgerLogger` call anywhere in this file touches a key value).
 * - Never included in `AiAuditLogEntity` (that table only ever receives
 *   provider id / model / latency / token count / confidence — see
 *   AiAuditLogger, which never even has a reference to this class).
 * - Never included in any DiagnosticCollector/exported bundle — nothing in
 *   `core/domain/service/diagnostic` references this class.
 */
@Singleton
class SecureApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "ai_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getApiKey(providerId: String): String? = prefs.getString(keyFor(providerId), null)

    fun setApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString(keyFor(providerId), apiKey).apply()
    }

    fun clearApiKey(providerId: String) {
        prefs.edit().remove(keyFor(providerId)).apply()
    }

    fun hasApiKey(providerId: String): Boolean = !getApiKey(providerId).isNullOrBlank()

    private fun keyFor(providerId: String) = "api_key_$providerId"
}
