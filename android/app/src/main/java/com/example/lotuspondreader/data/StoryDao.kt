package com.example.lotuspondreader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM history_items ORDER BY id DESC")
    fun getAllHistory(): Flow<List<StoryEntity>>

    @Insert
    suspend fun insertStory(story: StoryEntity): Long

    @Delete
    suspend fun deleteStory(story: StoryEntity): Int

    @Query("DELETE FROM history_items")
    suspend fun clearHistory(): Int
}
