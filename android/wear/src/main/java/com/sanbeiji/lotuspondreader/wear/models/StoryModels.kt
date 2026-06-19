package com.sanbeiji.lotuspondreader.wear.models

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
    val words: List<String> = emptyList(),
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
