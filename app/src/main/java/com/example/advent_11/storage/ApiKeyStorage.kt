package com.example.advent_11.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Хранилище API ключа в зашифрованном SharedPreferences (Android Keystore).
 */
class ApiKeyStorage(context: Context) {

    private val sharedPrefs = EncryptedSharedPreferences.create(
        PREFS_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(apiKey: String) {
        sharedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String? = sharedPrefs.getString(KEY_API_KEY, null)

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    companion object {
        private const val PREFS_NAME = "secure_api_prefs"
        private const val KEY_API_KEY = "deepseek_api_key"
    }
}
