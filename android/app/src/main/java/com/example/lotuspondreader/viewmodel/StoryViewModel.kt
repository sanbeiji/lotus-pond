package com.example.lotuspondreader.viewmodel

import android.content.Context
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
    private val storyDao: StoryDao,
    private val context: Context
) : ViewModel() {

    init {
        viewModelScope.launch {
            com.example.lotuspondreader.data.DictionaryImporter.importIfNeeded(
                context,
                com.example.lotuspondreader.data.StoryDatabase.getDatabase(context)
            )
        }
    }

    private val _uiState = MutableStateFlow<StoryUiState>(StoryUiState.Idle)
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private val _activeFetches = MutableStateFlow<Set<String>>(emptySet())
    val activeFetches: StateFlow<Set<String>> = _activeFetches.asStateFlow()

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    val userSettings = settingsRepository.userSettingsFlow
    
    private val _deletedStoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val deletionJobs = java.util.concurrent.ConcurrentHashMap<Long, kotlinx.coroutines.Job>()

    val history = kotlinx.coroutines.flow.combine(
        storyDao.getAllHistory(),
        _deletedStoryIds
    ) { dbList, deletedIds ->
        dbList.filter { it.id !in deletedIds }
    }

    val plot = MutableStateFlow("")
    val skillLevel = MutableStateFlow("A1 (Entry)")
    val length = MutableStateFlow("400")
    val requiredTerms = MutableStateFlow("")

    private val _isGeneratingPrompt = MutableStateFlow(false)
    val isGeneratingPrompt: StateFlow<Boolean> = _isGeneratingPrompt.asStateFlow()

    private var currentEntity: StoryEntity? = null

    fun resetUiState() {
        _uiState.value = StoryUiState.Idle
    }

    fun clearErrorEvent() {
        _errorEvent.value = null
    }

    suspend fun lookupWord(word: String): List<com.example.lotuspondreader.data.DictEntry> {
        val database = com.example.lotuspondreader.data.StoryDatabase.getDatabase(context)
        return com.example.lotuspondreader.data.WordLookupService.lookupWord(word, database.dictDao())
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            settingsRepository.saveSettings(settings)
        }
    }

    fun fetchGenrePrompt(genre: String) {
        viewModelScope.launch {
            val currentSettings = userSettings.first()
            if (currentSettings.apiKey.isBlank()) {
                _errorEvent.value = "Please configure your Gemini API key in settings."
                return@launch
            }

            _isGeneratingPrompt.value = true
            try {
                val promptResult = storyRepository.generateGenrePrompt(
                    apiKey = currentSettings.apiKey,
                    genre = genre
                )
                plot.value = promptResult
            } catch (e: Exception) {
                _errorEvent.value = e.message ?: "Failed to generate genre prompt."
            } finally {
                _isGeneratingPrompt.value = false
            }
        }
    }

    private fun getAudioCacheFile(text: String, voiceStyle: String): java.io.File {
        val hashInput = "${text}_${voiceStyle}"
        val digest = java.security.MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(hashInput.toByteArray(Charsets.UTF_8))
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }
        
        val ttsDir = java.io.File(context.cacheDir, "tts_cache")
        if (!ttsDir.exists()) {
            ttsDir.mkdirs()
        }
        return java.io.File(ttsDir, "$hashString.pcm")
    }

    suspend fun generateSpeech(text: String, voiceStyle: String): ByteArray? {
        return try {
            val cacheFile = getAudioCacheFile(text, voiceStyle)
            if (cacheFile.exists()) {
                return cacheFile.readBytes()
            }

            val currentSettings = userSettings.first()
            if (currentSettings.apiKey.isBlank()) {
                _errorEvent.value = "Please configure your Gemini API key in settings."
                return null
            }
            val base64Data = storyRepository.generateSpeech(currentSettings.apiKey, text, voiceStyle)
            val audioBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            cacheFile.writeBytes(audioBytes)
            audioBytes
        } catch (e: Exception) {
            _errorEvent.value = e.message ?: "Failed to generate speech."
            null
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

    fun deleteStory(story: StoryEntity) {
        // Cancel any previous pending delete job for this specific item if somehow invoked again
        deletionJobs[story.id]?.cancel()

        // 1. Add to optimistic delete tracking flow
        _deletedStoryIds.value = _deletedStoryIds.value + story.id

        // 2. Launch deferred background deletion job
        val job = viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(4000) // 4 seconds cancellation window
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    storyDao.deleteStory(story)
                    
                    // Also delete cached audio files for this story's sentences
                    val voiceStyles = listOf("standard", "southern", "heavy_southern")
                    story.storyData.sentences.forEach { sentence ->
                        voiceStyles.forEach { style ->
                            val cacheFile = getAudioCacheFile(sentence.mandarin, style)
                            if (cacheFile.exists()) {
                                cacheFile.delete()
                            }
                        }
                    }
                }
            } finally {
                _deletedStoryIds.value = _deletedStoryIds.value - story.id
                deletionJobs.remove(story.id)
            }
        }
        deletionJobs[story.id] = job
    }

    fun undoDeleteStory(storyId: Long) {
        deletionJobs[storyId]?.cancel()
        deletionJobs.remove(storyId)
        _deletedStoryIds.value = _deletedStoryIds.value - storyId
    }

    fun clearHistory() {
        viewModelScope.launch {
            // Commit any pending deletions immediately before clearing all
            deletionJobs.values.forEach { it.cancel() }
            deletionJobs.clear()
            _deletedStoryIds.value = emptySet()
            
            storyDao.clearHistory()
            
            // Clear entire cache directory
            val ttsDir = java.io.File(context.cacheDir, "tts_cache")
            if (ttsDir.exists()) {
                ttsDir.deleteRecursively()
            }
        }
    }
}
