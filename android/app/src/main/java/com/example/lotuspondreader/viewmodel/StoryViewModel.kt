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

    private val _activeFetches = MutableStateFlow<Set<String>>(emptySet())
    val activeFetches: StateFlow<Set<String>> = _activeFetches.asStateFlow()

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    val userSettings = settingsRepository.userSettingsFlow
    val history = storyDao.getAllHistory()

    val plot = MutableStateFlow("")
    val skillLevel = MutableStateFlow("A1 (Entry)")
    val length = MutableStateFlow("400")
    val requiredTerms = MutableStateFlow("")

    private var currentEntity: StoryEntity? = null

    fun resetUiState() {
        _uiState.value = StoryUiState.Idle
    }

    fun clearErrorEvent() {
        _errorEvent.value = null
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            settingsRepository.saveSettings(settings)
        }
    }

    fun loadStoryFromHistory(storyEntity: StoryEntity) {
        currentEntity = storyEntity
        _uiState.value = StoryUiState.Success(storyEntity.storyData)
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
                    requiredTerms = requiredTerms
                )

                _uiState.value = StoryUiState.Success(response)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                val newEntity = StoryEntity(
                    title = response.title,
                    storyData = response,
                    date = currentDate,
                    level = skillLevel
                )
                val insertedId = storyDao.insertStory(newEntity)
                currentEntity = newEntity.copy(id = insertedId)

                val syncedSettings = currentSettings.copy(
                    showPinyin = currentSettings.generatePinyin,
                    showZhuyin = currentSettings.generateZhuyin,
                    showTranslation = currentSettings.generateTranslation
                )
                settingsRepository.saveSettings(syncedSettings)

                if (syncedSettings.generatePinyin) checkAndFetchMissing("pinyin")
                if (syncedSettings.generateZhuyin) checkAndFetchMissing("zhuyin")
                if (syncedSettings.generateTranslation) checkAndFetchMissing("english")
            } catch (e: Exception) {
                _uiState.value = StoryUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun checkAndFetchMissing(type: String) {
        val currentState = _uiState.value
        if (currentState !is StoryUiState.Success) return
        val story = currentState.story

        val isMissing = story.sentences.any {
            when (type) {
                "pinyin" -> it.pinyin.isNullOrEmpty()
                "zhuyin" -> it.zhuyin.isNullOrEmpty()
                else -> it.english.isNullOrEmpty()
            }
        }
        if (!isMissing) return

        viewModelScope.launch {
            val currentSettings = userSettings.first()
            if (currentSettings.apiKey.isBlank()) return@launch

            _activeFetches.value = _activeFetches.value + type
            try {
                val sentencesList = story.sentences.map { it.mandarin }
                val fetched = storyRepository.fetchDynamicContent(
                    apiKey = currentSettings.apiKey,
                    model = currentSettings.selectedModel,
                    type = type,
                    sentences = sentencesList
                )

                val latestState = _uiState.value
                if (latestState is StoryUiState.Success) {
                    val currentStory = latestState.story
                    val updatedSentences = currentStory.sentences.mapIndexed { index, sentence ->
                        when (type) {
                            "pinyin" -> sentence.copy(pinyin = fetched[index])
                            "zhuyin" -> sentence.copy(zhuyin = fetched[index])
                            else -> sentence.copy(english = fetched[index])
                        }
                    }
                    val updatedStory = currentStory.copy(sentences = updatedSentences)
                    _uiState.value = StoryUiState.Success(updatedStory)

                    currentEntity?.let { entity ->
                        val updatedEntity = entity.copy(storyData = updatedStory)
                        storyDao.updateStory(updatedEntity)
                        currentEntity = updatedEntity
                    }
                }
            } catch (e: Exception) {
                _errorEvent.value = "Failed to load $type: ${e.message}"
                val revertedSettings = when (type) {
                    "pinyin" -> currentSettings.copy(showPinyin = false)
                    "zhuyin" -> currentSettings.copy(showZhuyin = false)
                    else -> currentSettings.copy(showTranslation = false)
                }
                settingsRepository.saveSettings(revertedSettings)
            } finally {
                _activeFetches.value = _activeFetches.value - type
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            storyDao.clearHistory()
        }
    }
}
