package com.example.lotuspondreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lotuspondreader.api.StoryRepository
import com.example.lotuspondreader.data.SettingsRepository
import com.example.lotuspondreader.data.StoryDao
import com.example.lotuspondreader.data.StoryEntity
import com.example.lotuspondreader.models.StoryResponse
import com.example.lotuspondreader.models.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class StoryUiState {
    object Idle : StoryUiState()
    object Loading : StoryUiState()
    data class Success(val story: StoryResponse) : StoryUiState()
    data class Error(val message: String) : StoryUiState()
}

class StoryViewModel(
    private val settingsRepository: SettingsRepository,
    private val storyRepository: StoryRepository,
    private val storyDao: StoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoryUiState>(StoryUiState.Idle)
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    val userSettings = settingsRepository.userSettingsFlow
    val history = storyDao.getAllHistory()

    val plot = MutableStateFlow("")
    val skillLevel = MutableStateFlow("A1 (Entry)")
    val length = MutableStateFlow("300")
    val requiredTerms = MutableStateFlow("")

    fun resetUiState() {
        _uiState.value = StoryUiState.Idle
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            settingsRepository.saveSettings(settings)
        }
    }

    fun loadStoryFromHistory(story: StoryResponse) {
        _uiState.value = StoryUiState.Success(story)
    }

    fun generateStory(
        plot: String,
        skillLevel: String,
        length: Int,
        requiredTerms: String
    ) {
        viewModelScope.launch {
            val currentSettings = userSettings.first()
            if (currentSettings.apiKey.isBlank()) {
                _uiState.value = StoryUiState.Error("Please configure your Gemini API key in settings.")
                return@launch
            }

            _uiState.value = StoryUiState.Loading
            try {
                val response = storyRepository.generateStory(
                    apiKey = currentSettings.apiKey,
                    model = currentSettings.selectedModel,
                    plot = plot,
                    skillLevel = skillLevel,
                    length = length,
                    requiredTerms = requiredTerms,
                    pronunciation = currentSettings.pronunciation
                )

                _uiState.value = StoryUiState.Success(response)

                // Save to history
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                storyDao.insertStory(
                    StoryEntity(
                        title = response.title,
                        storyData = response,
                        date = currentDate,
                        level = skillLevel
                    )
                )
            } catch (e: Exception) {
                _uiState.value = StoryUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            storyDao.clearHistory()
        }
    }
}
