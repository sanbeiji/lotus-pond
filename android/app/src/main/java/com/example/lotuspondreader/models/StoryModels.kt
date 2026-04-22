package com.example.lotuspondreader.models

import kotlinx.serialization.Serializable

@Serializable
data class StoryResponse(
    val title: String,
    val sentences: List<Sentence>
)

@Serializable
data class Sentence(
    val mandarin: String,
    val pinyin: String? = null,
    val zhuyin: String? = null,
    val english: String
)

@Serializable
data class HistoryItem(
    val id: Long,
    val title: String,
    val data: StoryResponse,
    val date: String
)

@Serializable
data class UserSettings(
    val apiKey: String = "",
    val selectedModel: String = "gemini-2.5-flash-lite",
    val pronunciation: String = "pinyin", // "pinyin" or "zhuyin"
    val studyMode: Boolean = false,
    val showTranslation: Boolean = false,
    val showPronunciation: Boolean = true,
    val themePreference: String = "system", // "system", "light", "dark"
    val useDynamicColor: Boolean = true
)
