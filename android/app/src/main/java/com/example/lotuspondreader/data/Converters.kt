package com.example.lotuspondreader.data

import androidx.room.TypeConverter
import com.example.lotuspondreader.models.StoryResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStoryResponse(value: StoryResponse): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStoryResponse(value: String): StoryResponse {
        return Json.decodeFromString(value)
    }
}
