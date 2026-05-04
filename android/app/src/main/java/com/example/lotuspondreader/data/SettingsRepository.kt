package com.example.lotuspondreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        val PRONUNCIATION = stringPreferencesKey("pronunciation")
        val STUDY_MODE = booleanPreferencesKey("study_mode")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val SHOW_PRONUNCIATION = booleanPreferencesKey("show_pronunciation")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FONT_SIZE_PREFERENCE = stringPreferencesKey("font_size_preference")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data
        .map { preferences ->
            UserSettings(
                apiKey = encryptedPrefs.getString(SECURE_API_KEY, "") ?: "",
                selectedModel = preferences[SELECTED_MODEL] ?: "gemini-2.5-flash-lite",
                pronunciation = preferences[PRONUNCIATION] ?: "pinyin",
                studyMode = preferences[STUDY_MODE] ?: true,
                showTranslation = preferences[SHOW_TRANSLATION] ?: true,
                showPronunciation = preferences[SHOW_PRONUNCIATION] ?: true,
                themePreference = preferences[THEME_PREFERENCE] ?: "system",
                useDynamicColor = preferences[DYNAMIC_COLOR] ?: false,
                fontSizePreference = preferences[FONT_SIZE_PREFERENCE] ?: "small"
            )
        }

    suspend fun saveSettings(settings: UserSettings) {
        // Save sensitive info to EncryptedSharedPreferences
        encryptedPrefs.edit().putString(SECURE_API_KEY, settings.apiKey).apply()

        // Save non-sensitive info to DataStore
        dataStore.edit { preferences ->
            preferences[LAST_UPDATED] = System.currentTimeMillis()
            preferences[SELECTED_MODEL] = settings.selectedModel
            preferences[PRONUNCIATION] = settings.pronunciation
            preferences[STUDY_MODE] = settings.studyMode
            preferences[SHOW_TRANSLATION] = settings.showTranslation
            preferences[SHOW_PRONUNCIATION] = settings.showPronunciation
            preferences[THEME_PREFERENCE] = settings.themePreference
            preferences[DYNAMIC_COLOR] = settings.useDynamicColor
            preferences[FONT_SIZE_PREFERENCE] = settings.fontSizePreference
        }
    }
}
