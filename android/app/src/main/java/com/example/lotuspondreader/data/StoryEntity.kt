package com.example.lotuspondreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lotuspondreader.models.StoryResponse

@Entity(tableName = "history_items")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val storyData: StoryResponse,
    val date: String,
    val level: String? = null
)
