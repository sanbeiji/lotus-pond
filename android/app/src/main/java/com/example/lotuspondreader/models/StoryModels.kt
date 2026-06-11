package com.example.lotuspondreader.models

import kotlinx.serialization.Serializable

@Serializable
data class StoryResponse(
    val title: String,
    val sentences: List<Sentence>,
    val requiredTerms: String = ""
)

@Serializable
data class Sentence(
    val mandarin: String,
    val pinyin: String? = null,
    val zhuyin: String? = null,
    val english: String? = null
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
    val selectedModel: String = "gemini-flash-lite-latest",
    val generatePinyin: Boolean = false,
    val generateZhuyin: Boolean = false,
    val generateTranslation: Boolean = false,
    val showPinyin: Boolean = true,
    val showZhuyin: Boolean = false,
    val studyMode: Boolean = true,
    val showTranslation: Boolean = true,
    val themePreference: String = "system", // "system", "light", "dark"
    val useDynamicColor: Boolean = false,
    val fontSizePreference: String = "small", // "small", "medium", "large"
    val speechRatePreference: Float = 0.9f,
    val useGeminiTts: Boolean = false,
    val geminiTtsVoiceStyle: String = "standard"
)
