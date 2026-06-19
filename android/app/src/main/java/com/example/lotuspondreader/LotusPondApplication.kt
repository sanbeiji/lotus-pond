package com.example.lotuspondreader

import android.app.Application
import android.util.Log
import com.example.lotuspondreader.data.StoryDatabase
import com.example.lotuspondreader.models.HistoryItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LotusPondApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("LotusPondApplication", "Application onCreate - setting up Room observer for Wear OS DataClient")

        applicationScope.launch {
            try {
                val database = StoryDatabase.getDatabase(applicationContext)
                database.storyDao().getAllHistory().collect { entities ->
                    Log.d("LotusPondApplication", "Database changed, syncing ${entities.size} items to Wear OS DataClient")
                    val historyItems = entities.take(20).map { entity ->
                        HistoryItem(
                            id = entity.id,
                            title = entity.title,
                            data = entity.storyData,
                            date = entity.date
                        )
                    }
                    val json = Json.encodeToString(historyItems)
                    
                    val putDataReq = PutDataMapRequest.create("/history").apply {
                        dataMap.putString("history_json", json)
                        dataMap.putLong("timestamp", System.currentTimeMillis())
                    }.asPutDataRequest().setUrgent()

                    Wearable.getDataClient(applicationContext)
                        .putDataItem(putDataReq)
                        .addOnSuccessListener {
                            Log.d("LotusPondApplication", "Successfully synced history DataItem to Wearable network.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("LotusPondApplication", "Failed to sync history DataItem", e)
                        }
                }
            } catch (e: Exception) {
                Log.e("LotusPondApplication", "Error in Wear OS DataClient sync observer", e)
            }
        }
    }
}
