package com.example.lotuspondreader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lotuspondreader.api.StoryRepository
import com.example.lotuspondreader.data.SettingsRepository
import com.example.lotuspondreader.data.StoryDatabase

class StoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoryViewModel(
                settingsRepository = SettingsRepository(context),
                storyRepository = StoryRepository(),
                storyDao = StoryDatabase.getDatabase(context).storyDao()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
