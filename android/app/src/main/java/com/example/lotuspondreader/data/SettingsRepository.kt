package com.example.lotuspondreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.lotuspondreader.models.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val SECURE_API_KEY = "secure_api_key"
        val LAST_UPDATED = longPreferencesKey("last_updated")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val GENERATE_PINYIN = booleanPreferencesKey("generate_pinyin")
        val GENERATE_ZHUYIN = booleanPreferencesKey("generate_zhuyin")
        val GENERATE_TRANSLATION = booleanPreferencesKey("generate_translation")
        val SHOW_PINYIN = booleanPreferencesKey("show_pinyin")
        val SHOW_ZHUYIN = booleanPreferencesKey("show_zhuyin")
        val STUDY_MODE = booleanPreferencesKey("study_mode")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FONT_SIZE_PREFERENCE = stringPreferencesKey("font_size_preference")
        val SPEECH_RATE_PREFERENCE = floatPreferencesKey("speech_rate_preference")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data
        .map { preferences ->
            val savedModel = preferences[SELECTED_MODEL] ?: "gemini-flash-lite-latest"
            val normalizedModel = if (savedModel != "gemini-flash-latest" && savedModel != "gemini-flash-lite-latest") {
                "gemini-flash-lite-latest"
            } else {
                savedModel
            }
            UserSettings(
                apiKey = encryptedPrefs.getString(SECURE_API_KEY, "") ?: "",
                selectedModel = normalizedModel,
                generatePinyin = preferences[GENERATE_PINYIN] ?: false,
                generateZhuyin = preferences[GENERATE_ZHUYIN] ?: false,
                generateTranslation = preferences[GENERATE_TRANSLATION] ?: false,
                showPinyin = preferences[SHOW_PINYIN] ?: true,
                showZhuyin = preferences[SHOW_ZHUYIN] ?: false,
                studyMode = preferences[STUDY_MODE] ?: true,
                showTranslation = preferences[SHOW_TRANSLATION] ?: true,
                themePreference = preferences[THEME_PREFERENCE] ?: "system",
                useDynamicColor = preferences[DYNAMIC_COLOR] ?: false,
                fontSizePreference = preferences[FONT_SIZE_PREFERENCE] ?: "small",
                speechRatePreference = preferences[SPEECH_RATE_PREFERENCE] ?: 0.9f
            )
        }

    suspend fun saveSettings(settings: UserSettings) {
        // Save sensitive info to EncryptedSharedPreferences
        encryptedPrefs.edit().putString(SECURE_API_KEY, settings.apiKey).apply()

        // Save non-sensitive info to DataStore
        dataStore.edit { preferences ->
            preferences[LAST_UPDATED] = System.currentTimeMillis()
            preferences[SELECTED_MODEL] = settings.selectedModel
            preferences[GENERATE_PINYIN] = settings.generatePinyin
            preferences[GENERATE_ZHUYIN] = settings.generateZhuyin
            preferences[GENERATE_TRANSLATION] = settings.generateTranslation
            preferences[SHOW_PINYIN] = settings.showPinyin
            preferences[SHOW_ZHUYIN] = settings.showZhuyin
            preferences[STUDY_MODE] = settings.studyMode
            preferences[SHOW_TRANSLATION] = settings.showTranslation
            preferences[THEME_PREFERENCE] = settings.themePreference
            preferences[DYNAMIC_COLOR] = settings.useDynamicColor
            preferences[FONT_SIZE_PREFERENCE] = settings.fontSizePreference
            preferences[SPEECH_RATE_PREFERENCE] = settings.speechRatePreference
        }
    }
}
