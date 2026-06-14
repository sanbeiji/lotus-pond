package com.example.lotuspondreader.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary_entries",
    indices = [Index(value = ["traditional"])]
)
data class DictEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val traditional: String,
    val simplified: String,
    val pinyin: String,
    val english: String
)
