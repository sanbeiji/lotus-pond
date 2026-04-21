package com.example.lotuspondreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lotuspondreader.models.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val PRONUNCIATION = stringPreferencesKey("pronunciation")
        val STUDY_MODE = booleanPreferencesKey("study_mode")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val SHOW_PRONUNCIATION = booleanPreferencesKey("show_pronunciation")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data
        .map { preferences ->
            UserSettings(
                apiKey = preferences[API_KEY] ?: "",
                selectedModel = preferences[SELECTED_MODEL] ?: "gemini-2.5-flash-lite",
                pronunciation = preferences[PRONUNCIATION] ?: "pinyin",
                studyMode = preferences[STUDY_MODE] ?: false,
                showTranslation = preferences[SHOW_TRANSLATION] ?: false,
                showPronunciation = preferences[SHOW_PRONUNCIATION] ?: true,
                themePreference = preferences[THEME_PREFERENCE] ?: "system"
            )
        }

    suspend fun saveSettings(settings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[API_KEY] = settings.apiKey
            preferences[SELECTED_MODEL] = settings.selectedModel
            preferences[PRONUNCIATION] = settings.pronunciation
            preferences[STUDY_MODE] = settings.studyMode
            preferences[SHOW_TRANSLATION] = settings.showTranslation
            preferences[SHOW_PRONUNCIATION] = settings.showPronunciation
            preferences[THEME_PREFERENCE] = settings.themePreference
        }
    }
}
