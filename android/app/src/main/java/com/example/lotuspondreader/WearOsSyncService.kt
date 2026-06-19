package com.example.lotuspondreader

import android.util.Log
import com.example.lotuspondreader.api.StoryRepository
import com.example.lotuspondreader.data.SettingsRepository
import com.example.lotuspondreader.data.StoryDatabase
import com.example.lotuspondreader.data.StoryEntity
import com.example.lotuspondreader.models.HistoryItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WearOsSyncService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storyRepository = StoryRepository()

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("WearOsSyncService", "Received message: ${messageEvent.path}")

        when (messageEvent.path) {
            "/fetch_history" -> {
                serviceScope.launch {
                    sendHistoryToWatch(messageEvent.sourceNodeId)
                }
            }
            "/generate_story" -> {
                serviceScope.launch {
                    try {
                        generateNewStoryAndSync(messageEvent.sourceNodeId)
                    } catch (e: Exception) {
                        Log.e("WearOsSyncService", "Error generating story", e)
                        sendErrorToWatch(messageEvent.sourceNodeId, e.message ?: "Unknown error occurred")
                    }
                }
            }
        }
    }

    private suspend fun sendHistoryToWatch(nodeId: String) {
        try {
            val database = StoryDatabase.getDatabase(applicationContext)
            // Fetch top 20 items ordered by ID desc
            val entities = database.storyDao().getAllHistory().first()
            val historyItems = entities.take(20).map { entity ->
                HistoryItem(
                    id = entity.id,
                    title = entity.title,
                    data = entity.storyData,
                    date = entity.date
                )
            }
            val json = Json.encodeToString(historyItems)
            Wearable.getMessageClient(applicationContext)
                .sendMessage(nodeId, "/history_response", json.toByteArray())
                .addOnSuccessListener {
                    Log.d("WearOsSyncService", "History synced successfully to watch.")
                }
                .addOnFailureListener { e ->
                    Log.e("WearOsSyncService", "Failed to sync history to watch", e)
                }
        } catch (e: Exception) {
            Log.e("WearOsSyncService", "Error in sendHistoryToWatch", e)
            sendErrorToWatch(nodeId, e.message ?: "Failed to read history database")
        }
    }

    private suspend fun generateNewStoryAndSync(nodeId: String) {
        val settingsRepository = SettingsRepository(applicationContext)
        val settings = settingsRepository.userSettingsFlow.first()

        if (settings.apiKey.isBlank()) {
            sendErrorToWatch(nodeId, "Gemini API key is not configured on the phone.")
            return
        }

        // Send a temporary "loading" status if desired, or simply generate
        Log.d("WearOsSyncService", "Generating story for Wear OS... Level: ${settings.wearOsStoryLevel}")

        val genres = listOf("Adventure", "Daily Life", "Fantasy", "Mystery", "Sci-Fi", "Historical", "Romance", "Pirates", "Music")
        val randomGenre = genres.random()

        val plot = storyRepository.generateGenrePrompt(
            apiKey = settings.apiKey,
            genre = randomGenre
        )

        val response = storyRepository.generateStory(
            apiKey = settings.apiKey,
            model = settings.selectedModel,
            plot = plot,
            skillLevel = settings.wearOsStoryLevel,
            length = 200,
            requiredTerms = ""
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val newEntity = StoryEntity(
            title = response.title,
            storyData = response,
            date = currentDate,
            level = settings.wearOsStoryLevel
        )

        val database = StoryDatabase.getDatabase(applicationContext)
        database.storyDao().insertStory(newEntity)

        Log.d("WearOsSyncService", "New Wear OS story saved. Syncing history...")
        sendHistoryToWatch(nodeId)
    }

    private fun sendErrorToWatch(nodeId: String, errorMsg: String) {
        Wearable.getMessageClient(applicationContext)
            .sendMessage(nodeId, "/error_response", errorMsg.toByteArray())
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
